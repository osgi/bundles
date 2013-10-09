
package osgi.jpa.managed.hibernate.adapter;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformProvider;

public class JtaPlatformProviderImpl implements JtaPlatformProvider {

	public static TransactionManager	transactionManager;

	@Override
	public JtaPlatform getProvidedJtaPlatform() {
		return new AbstractJtaPlatform() {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected TransactionManager locateTransactionManager() {
				return transactionManager;
			}

			@Override
			protected UserTransaction locateUserTransaction() {
				return new UserTransaction() {

					@Override
					public void begin() throws NotSupportedException, SystemException {
						transactionManager.begin();
					}

					@Override
					public void commit() throws HeuristicMixedException, HeuristicRollbackException,
							IllegalStateException, RollbackException, SecurityException, SystemException {
						transactionManager.commit();
					}

					@Override
					public int getStatus() throws SystemException {
						return transactionManager.getStatus();
					}

					@Override
					public void rollback() throws IllegalStateException, SecurityException, SystemException {
						transactionManager.rollback();
					}

					@Override
					public void setRollbackOnly() throws IllegalStateException, SystemException {
						transactionManager.setRollbackOnly();
					}

					@Override
					public void setTransactionTimeout(int seconds) throws SystemException {
						transactionManager.setTransactionTimeout(seconds);
					}

				};
			}

		};
	}

}
