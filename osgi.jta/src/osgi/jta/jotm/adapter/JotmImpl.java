package osgi.jta.jotm.adapter;

import javax.transaction.*;

import org.objectweb.jotm.*;

import aQute.bnd.annotation.component.*;

@Component
public class JotmImpl implements javax.transaction.TransactionManager {
	TransactionManager						delegate;
	Jotm									jotm;

	@Activate
	void activate() throws Throwable {
		try {
			RmiConfiguration rmc = new RmiConfiguration() {
				
				@Override
				public boolean isCorbaCompliant() {
					return false;
				}
				
				@Override
				public void init() throws RmiConfigurationException {
				}
			};
			jotm = new org.objectweb.jotm.Jotm(true, false, rmc);
			delegate = jotm.getTransactionManager();
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	public void begin() throws NotSupportedException, SystemException {
		delegate.begin();
	}

	public void commit() throws HeuristicMixedException,
			HeuristicRollbackException, IllegalStateException,
			RollbackException, SecurityException, SystemException {
		delegate.commit();
	}

	public int getStatus() throws SystemException {
		return delegate.getStatus();
	}

	public Transaction getTransaction() throws SystemException {
		return delegate.getTransaction();
	}

	public void resume(Transaction arg0) throws IllegalStateException,
			InvalidTransactionException, SystemException {
		delegate.resume(arg0);
	}

	public void rollback() throws IllegalStateException, SecurityException,
			SystemException {
		delegate.rollback();
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		delegate.setRollbackOnly();
	}

	public void setTransactionTimeout(int arg0) throws SystemException {
		delegate.setTransactionTimeout(arg0);
	}

	public Transaction suspend() throws SystemException {
		return delegate.suspend();
	}
}