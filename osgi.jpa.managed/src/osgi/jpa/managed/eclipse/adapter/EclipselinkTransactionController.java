package osgi.jpa.managed.eclipse.adapter;

import javax.transaction.*;

import org.eclipse.persistence.transaction.*;

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
	protected TransactionManager acquireTransactionManager() throws Exception {
		return tm;
	}

}
