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

package osgi.jpa.managed.eclipse.adapter;

import javax.transaction.TransactionManager;
import org.eclipse.persistence.transaction.JTATransactionController;

/**
 * This sucks SOOOOOOOOOOOOOO majorly, having to use statics to make EclipseLink
 * understand our transaction manager.
 */
public class EclipselinkTransactionController extends JTATransactionController {
	static TransactionManager	tm;

	/**
	 * This method can be can be overridden by subclasses to obtain the
	 * transaction manager by whatever means is appropriate to the server. This
	 * method is invoked by the constructor to initialize the transaction
	 * manager at instance-creation time. Alternatively the transaction manager
	 * can be set directly on the controller instance using the
	 * setTransactionManager() method after the instance has been created.
	 * 
	 * @return The TransactionManager for the transaction system
	 */
	@Override
	protected TransactionManager acquireTransactionManager() throws Exception {
		return tm;
	}

}
