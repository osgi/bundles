/*
 * Copyright (c) OSGi Alliance (2013). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package osgi.jta.bitronix.adapter;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;

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

	public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException,
			RollbackException, SecurityException, SystemException {
		delegate.commit();
	}

	public int getStatus() throws SystemException {
		return delegate.getStatus();
	}

	public Transaction getTransaction() throws SystemException {
		return delegate.getTransaction();
	}

	public void resume(Transaction arg0) throws IllegalStateException, InvalidTransactionException, SystemException {
		delegate.resume(arg0);
	}

	public void rollback() throws IllegalStateException, SecurityException, SystemException {
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
