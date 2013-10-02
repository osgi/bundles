package osgi.jpa.managed.hibernate.adapter;

import javax.transaction.*;

import org.hibernate.engine.transaction.jta.platform.internal.*;
import org.hibernate.engine.transaction.jta.platform.spi.*;

public class JtaPlatformProviderImpl implements JtaPlatformProvider {

	public static TransactionManager	transactionManager;

	@Override
	public JtaPlatform getProvidedJtaPlatform() {
		return new AbstractJtaPlatform() {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected TransactionManager locateTransactionManager() {
				System.out.println("get transaction for hibernate");
				return transactionManager;
			}

			@Override
			protected UserTransaction locateUserTransaction() {
				System.out.println("get user transaction for hibernate");
				return new UserTransaction() {

					@Override
					public void begin() throws NotSupportedException,
							SystemException {
						transactionManager.begin();
					}

					@Override
					public void commit() throws HeuristicMixedException,
							HeuristicRollbackException, IllegalStateException,
							RollbackException, SecurityException,
							SystemException {
						transactionManager.commit();
					}

					@Override
					public int getStatus() throws SystemException {
						return transactionManager.getStatus();
					}

					@Override
					public void rollback() throws IllegalStateException,
							SecurityException, SystemException {
						transactionManager.rollback();
					}

					@Override
					public void setRollbackOnly() throws IllegalStateException,
							SystemException {
						transactionManager.setRollbackOnly();
					}

					@Override
					public void setTransactionTimeout(int seconds)
							throws SystemException {
						transactionManager.setTransactionTimeout(seconds);
					}
					
				};
			}
			
		};
	}


}
