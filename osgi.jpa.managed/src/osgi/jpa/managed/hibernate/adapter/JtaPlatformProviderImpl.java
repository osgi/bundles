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
