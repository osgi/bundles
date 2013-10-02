package osgi.jpa.managed.aux;

import java.io.*;
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
class DataSourceWrapper implements DataSource {

	private final XADataSource							xaDataSource;
	private final TransactionManager					transactionManager;
	private final boolean								transactionMode;
	private final Map<Transaction, TransactionSession>	xaConnections	= new ConcurrentHashMap<Transaction, TransactionSession>();
	private final JPABridgeLogMessages					msgs;
	private final Set<Connection>						connections		= Collections.synchronizedSet( new HashSet<Connection>());

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

		TransactionSession(Transaction transaction, XAConnection xaConnection)
				throws Exception {
			this.transaction = transaction;
			this.xaConnection = xaConnection;
			transaction.enlistResource(xaConnection.getXAResource());
		}

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
				public void setAutoCommit(boolean autoCommit)
						throws SQLException {
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
					msgs.step("close on connection due to jta ignored "
							+ delegate);
					// Do not close the connection until the end of
					// of the commit.
				}
			};
		}

		public void close(int status) throws Exception {
			for (Connection c : connections) {
				c.close();
			}
		}
	}

	public DataSourceWrapper(TransactionManager tm, XADataSource ds,
			boolean transactionMode, JPABridgeLogMessages msgs) {
		this.transactionManager = tm;
		this.xaDataSource = ds;
		this.transactionMode = transactionMode;
		this.msgs = msgs;
	}

	//
	// Close any connections outside a transaction
	public void close() {
		System.out.println("Close all connections " + connections);
		for ( Connection c : connections ) {
			try {
				c.close();
			} catch (SQLException e) {
				// ignore
			}
		}
	}
	@Override
	public Connection getConnection() throws SQLException {

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
			session = new TransactionSession(transaction,
					xaDataSource.getXAConnection());

			xaConnections.put(transaction, session);

			//
			// Just to make it visible to the inner class
			//
			final TransactionSession session0 = session;

			//
			// Ensure we close the connection at the end of the transaction
			// by registering a callback with the transaction.
			//

			transactionManager.getTransaction().registerSynchronization(
					new Synchronization() {

						@Override
						public void beforeCompletion() {
						}

						@Override
						public void afterCompletion(int status) {
							try {
								msgs.step("close transaction " + transaction
										+ " " + status + " "
										+ Thread.currentThread());
								session0.close(status);
							} catch (Exception e) {
								// Ignore since these errors
								// should have been handled during
								// the commit phase of the transaction
								msgs.failed(
										"closing connection after transaction close",
										e);
							}
						}
					});

			return session0.getConnection();
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw new SQLException("DataSourceWrapper:getConnection", e);
		}
	}

	private Connection add(final Connection connection) {
		System.out.println("Add " + connection);
		connections.add(connection);
		return new ConnectionWrapper(connection) {
			public void close() {
				System.out.println("close " + connection);
				connections.remove(connection);
			}
		};
	}

	/**
	 * Just show that we're a proxy on another XA Data Source
	 */
	public String toString() {
		return xaDataSource + "'";
	}

	//
	// Delegate to our XA Data Source
	//

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return xaDataSource.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		xaDataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		xaDataSource.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return xaDataSource.getLoginTimeout();
	}

	//
	// Have no idea what those wrap methods do, sounds
	// very fishy
	//

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(XADataSource.class))
			iface.cast(xaDataSource);
		throw new SQLException("Cannot unwrap to " + iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(XADataSource.class);
	}

	@Override
	public Connection getConnection(String username, String password)
			throws SQLException {
		return getConnection();
	}

	//
	// Full Connection delegator kept separate to increase readability
	// of main code.
	//

	static class ConnectionWrapper implements Connection {
		Connection	delegate;

		ConnectionWrapper(Connection delegate) {
			this.delegate = delegate;
		}

		public <T> T unwrap(Class<T> iface) throws SQLException {
			return delegate.unwrap(iface);
		}

		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return delegate.isWrapperFor(iface);
		}

		public Statement createStatement() throws SQLException {
			return delegate.createStatement();
		}

		public PreparedStatement prepareStatement(String sql)
				throws SQLException {
			return delegate.prepareStatement(sql);
		}

		public CallableStatement prepareCall(String sql) throws SQLException {
			return delegate.prepareCall(sql);
		}

		public String nativeSQL(String sql) throws SQLException {
			return delegate.nativeSQL(sql);
		}

		public void setAutoCommit(boolean autoCommit) throws SQLException {
			delegate.setAutoCommit(autoCommit);
		}

		public boolean getAutoCommit() throws SQLException {
			return delegate.getAutoCommit();
		}

		public void commit() throws SQLException {
			delegate.commit();
		}

		public void rollback() throws SQLException {
			delegate.rollback();
		}

		public void close() throws SQLException {
				delegate.close();
		}

		public boolean isClosed() throws SQLException {
			return delegate.isClosed();
		}

		public DatabaseMetaData getMetaData() throws SQLException {
			return delegate.getMetaData();
		}

		public void setReadOnly(boolean readOnly) throws SQLException {
			delegate.setReadOnly(readOnly);
		}

		public boolean isReadOnly() throws SQLException {
			return delegate.isReadOnly();
		}

		public void setCatalog(String catalog) throws SQLException {
			delegate.setCatalog(catalog);
		}

		public String getCatalog() throws SQLException {
			return delegate.getCatalog();
		}

		public void setTransactionIsolation(int level) throws SQLException {
			delegate.setTransactionIsolation(level);
		}

		public int getTransactionIsolation() throws SQLException {
			return delegate.getTransactionIsolation();
		}

		public SQLWarning getWarnings() throws SQLException {
			return delegate.getWarnings();
		}

		public void clearWarnings() throws SQLException {
			delegate.clearWarnings();
		}

		public Statement createStatement(int resultSetType,
				int resultSetConcurrency) throws SQLException {
			return delegate
					.createStatement(resultSetType, resultSetConcurrency);
		}

		public PreparedStatement prepareStatement(String sql,
				int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return delegate.prepareStatement(sql, resultSetType,
					resultSetConcurrency);
		}

		public CallableStatement prepareCall(String sql, int resultSetType,
				int resultSetConcurrency) throws SQLException {
			return delegate.prepareCall(sql, resultSetType,
					resultSetConcurrency);
		}

		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return delegate.getTypeMap();
		}

		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
			delegate.setTypeMap(map);
		}

		public void setHoldability(int holdability) throws SQLException {
			delegate.setHoldability(holdability);
		}

		public int getHoldability() throws SQLException {
			return delegate.getHoldability();
		}

		public Savepoint setSavepoint() throws SQLException {
			return delegate.setSavepoint();
		}

		public Savepoint setSavepoint(String name) throws SQLException {
			return delegate.setSavepoint(name);
		}

		public void rollback(Savepoint savepoint) throws SQLException {
			delegate.rollback(savepoint);
		}

		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
			delegate.releaseSavepoint(savepoint);
		}

		public Statement createStatement(int resultSetType,
				int resultSetConcurrency, int resultSetHoldability)
				throws SQLException {
			return delegate.createStatement(resultSetType,
					resultSetConcurrency, resultSetHoldability);
		}

		public PreparedStatement prepareStatement(String sql,
				int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return delegate.prepareStatement(sql, resultSetType,
					resultSetConcurrency, resultSetHoldability);
		}

		public CallableStatement prepareCall(String sql, int resultSetType,
				int resultSetConcurrency, int resultSetHoldability)
				throws SQLException {
			return delegate.prepareCall(sql, resultSetType,
					resultSetConcurrency, resultSetHoldability);
		}

		public PreparedStatement prepareStatement(String sql,
				int autoGeneratedKeys) throws SQLException {
			return delegate.prepareStatement(sql, autoGeneratedKeys);
		}

		public PreparedStatement prepareStatement(String sql,
				int[] columnIndexes) throws SQLException {
			return delegate.prepareStatement(sql, columnIndexes);
		}

		public PreparedStatement prepareStatement(String sql,
				String[] columnNames) throws SQLException {
			return delegate.prepareStatement(sql, columnNames);
		}

		public Clob createClob() throws SQLException {
			return delegate.createClob();
		}

		public Blob createBlob() throws SQLException {
			return delegate.createBlob();
		}

		public NClob createNClob() throws SQLException {
			return delegate.createNClob();
		}

		public SQLXML createSQLXML() throws SQLException {
			return delegate.createSQLXML();
		}

		public boolean isValid(int timeout) throws SQLException {
			return delegate.isValid(timeout);
		}

		public void setClientInfo(String name, String value)
				throws SQLClientInfoException {
			delegate.setClientInfo(name, value);
		}

		public void setClientInfo(Properties properties)
				throws SQLClientInfoException {
			delegate.setClientInfo(properties);
		}

		public String getClientInfo(String name) throws SQLException {
			return delegate.getClientInfo(name);
		}

		public Properties getClientInfo() throws SQLException {
			return delegate.getClientInfo();
		}

		public Array createArrayOf(String typeName, Object[] elements)
				throws SQLException {
			return delegate.createArrayOf(typeName, elements);
		}

		public Struct createStruct(String typeName, Object[] attributes)
				throws SQLException {
			return delegate.createStruct(typeName, attributes);
		}

	}

}
