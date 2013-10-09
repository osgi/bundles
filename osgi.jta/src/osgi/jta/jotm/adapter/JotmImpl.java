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

package osgi.jta.jotm.adapter;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.objectweb.jotm.Jotm;
import org.objectweb.jotm.RmiConfiguration;
import org.objectweb.jotm.RmiConfigurationException;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;

@Component
public class JotmImpl implements javax.transaction.TransactionManager {
	TransactionManager	delegate;
	Jotm				jotm;

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
