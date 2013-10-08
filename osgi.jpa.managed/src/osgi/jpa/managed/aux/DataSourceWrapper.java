package osgi.jpa.managed.aux;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import javax.sql.*;
import javax.transaction.*;

/**
 * The intent of this class is wrap an XA Data Source in transactional or
 * non-transactional mode. If used in transactional mode it enlists any
 * connections that are obtained through this object and ensure that non of the
 * transaction methods are called on the connection in that case.
 */
class DataSourceWrapper implements InvocationHandler {

	private final XADataSource							xaDataSource;
	private final TransactionManager					transactionManager;
	private final boolean								transactionMode;
	private final Map<Transaction,TransactionSession>	xaConnections	= new ConcurrentHashMap<Transaction,TransactionSession>();
	private final JPABridgeLogMessages					msgs;
	private final Set<Connection>						connections		= Collections
																				.synchronizedSet(new HashSet<Connection>());
	private DataSource									datasource;

	/**
	 * A TransactionSession is created when a XAConnection is used in a
	 * Transaction, it then lives until the transactions is ended. This class
	 * ensures enlisting/delisting and closes all used connections in this
	 * transaction.
	 */
	class TransactionSession {
		final XAConnection		xaConnection;
		final Set<Connection>	connections	= new HashSet<Connection>();
		final Transaction		transaction;

		TransactionSession(Transaction transaction, XAConnection xaConnection) throws Exception {
			this.transaction = transaction;
			this.xaConnection = xaConnection;
			transaction.enlistResource(xaConnection.getXAResource());
		}

		@SuppressWarnings("unused")
		public Connection getConnection() throws SQLException {
			Connection connection = xaConnection.getConnection();
			connections.add(connection);

			//
			// The connection is under control of a transaction
			// so make sure it is not used wrongly. We wrap
			// it and throw exceptions when then try to call
			// transaction control methods. Also ignore
			// close since we will close the connection at the end
			// of the commit
			//

			return new ConnectionWrapper(connection) {
				public void setAutoCommit(boolean autoCommit) throws SQLException {
					throw new UnsupportedOperationException("jta transaction");
				}

				public boolean getAutoCommit() throws SQLException {
					throw new UnsupportedOperationException("jta transaction");
				}

				public void commit() throws SQLException {
					throw new UnsupportedOperationException("jta transaction");
				}

				public void rollback() throws SQLException {
					throw new UnsupportedOperationException("jta transaction");
				}

				public void close() throws SQLException {
					msgs.step("close on connection due to jta ignored " + delegate);
					// Do not close the connection until the end of
					// of the commit.
				}
			}.getConnection();
		}

		public void close(int status) throws Exception {
			for (Connection c : connections) {
				c.close();
			}
		}
	}

	public DataSourceWrapper(TransactionManager tm, XADataSource ds, boolean transactionMode, JPABridgeLogMessages msgs) {
		this.transactionManager = tm;
		this.xaDataSource = ds;
		this.transactionMode = transactionMode;
		this.msgs = msgs;
		this.datasource = (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {
			DataSource.class
		}, this);
	}

	@Override
	public Object invoke(Object target, Method method, Object[] args) throws Throwable {
		try {
			if (method.getName().equals("getConnection")) // getConnection()/getConnection(user,password)
				return getLocalConnection();

			if (method.getName().equals("close") && args.length == 0)
				close();

			return method.invoke(target, args);
		}
		catch (InvocationTargetException ite) {
			throw ite.getTargetException();
		}
	}

	public DataSource getDataSource() {
		return datasource;
	}

	//
	// Close any connections outside a transaction
	//

	public void close() {
		System.out.println("Close all connections " + connections);
		for (Connection c : connections) {
			try {
				c.close();
			}
			catch (SQLException e) {
				// ignore
			}
		}
	}

	public Connection getLocalConnection() throws SQLException {

		//
		// If we are not transaction, just return a connection
		// and leave it up to the caller to close it.
		//

		if (!transactionMode) {
			return add(xaDataSource.getXAConnection().getConnection());
		}

		try {

			//
			// We're now in transaction mode. So we need to enlist
			// ourselves with the current transaction. So we
			// require a real transaction
			//

			final Transaction transaction = transactionManager.getTransaction();
			if (transaction == null) {
				//
				// SPEC: Hibernate was calling a Jta Data Source without a
				// transaction so we return a non-transaction data source.
				// not sure what the spec says about this. The right
				// thing to do is throw an exception I would say.
				// throw new
				// SQLException("A JTA Data source requires a transaction and there is none");

				return xaDataSource.getXAConnection().getConnection();
			}

			//
			// SPEC: Initially got this wrong, it seems you can enlist a
			// XAConnection only once per transaction. So we now use a
			// session to control the life cycle: Transaction -> XAConnection
			// -> * Connection
			//

			msgs.step("get jta Connection " + transaction);
			TransactionSession session = xaConnections.get(transaction);
			if (session != null)
				//
				// The session already exists, so just another
				// connection is required. Enlisting is already done
				//
				return add(session.getConnection());

			//
			// First time a connections is required for this
			// transaction. Create a session to track the
			// connections created in this transaction
			//
			session = new TransactionSession(transaction, xaDataSource.getXAConnection());

			xaConnections.put(transaction, session);

			//
			// Just to make it visible to the inner class
			//
			final TransactionSession session0 = session;

			//
			// Ensure we close the connection at the end of the transaction
			// by registering a callback with the transaction.
			//

			transactionManager.getTransaction().registerSynchronization(new Synchronization() {

				@Override
				public void beforeCompletion() {}

				@Override
				public void afterCompletion(int status) {
					try {
						msgs.step("close transaction " + transaction + " " + status + " " + Thread.currentThread());
						session0.close(status);
					}
					catch (Exception e) {
						// Ignore since these errors
						// should have been handled during
						// the commit phase of the transaction
						msgs.failed("closing connection after transaction close", e);
					}
				}
			});

			return session0.getConnection();
		}
		catch (SQLException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SQLException("DataSourceWrapper:getConnection", e);
		}
	}

	@SuppressWarnings("unused")
	private Connection add(final Connection connection) {
		connections.add(connection);
		return new ConnectionWrapper(connection) {
			public void close() {
				System.out.println("close " + connection);
				connections.remove(connection);
			}
		}.getConnection();
	}

	/**
	 * Just show that we're a proxy on another XA Data Source
	 */
	public String toString() {
		return xaDataSource + "'";
	}

	//
	// Full Connection delegator kept separate to increase readability
	// of main code.
	//

	static class ConnectionWrapper implements InvocationHandler {
		protected Connection	delegate;
		private Connection		proxy;

		ConnectionWrapper(Connection delegate) {
			this.delegate = delegate;
			this.proxy = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {
				Connection.class
			}, this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				// TODO Bit inefficient, should work for now
				Method m = getClass().getMethod(method.getName(), method.getParameterTypes());
				return m.invoke(this, args);
			}
			catch (NoSuchMethodException e) {
				try {
					return method.invoke(delegate, args);
				}
				catch (InvocationTargetException t) {
					throw t.getTargetException();
				}
			}
			catch (InvocationTargetException ite) {
				throw ite.getTargetException();
			}
		}

		public Connection getConnection() {
			return proxy;
		}
	}

}
