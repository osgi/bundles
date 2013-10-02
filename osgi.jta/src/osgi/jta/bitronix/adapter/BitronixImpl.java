package osgi.jta.bitronix.adapter;

import javax.transaction.*;

import aQute.bnd.annotation.component.*;
import bitronix.tm.*;

@Component
public class BitronixImpl implements javax.transaction.TransactionManager {
	BitronixTransactionManager	delegate;

	@Activate
	void activate() throws Throwable {
		delegate = TransactionManagerServices.getTransactionManager();
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