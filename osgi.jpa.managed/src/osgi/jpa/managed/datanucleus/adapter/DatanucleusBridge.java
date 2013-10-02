package osgi.jpa.managed.datanucleus.adapter;

import java.net.*;
import java.util.*;

import javax.persistence.spi.*;
import javax.transaction.*;

import org.osgi.framework.*;
import org.osgi.framework.wiring.*;
import org.osgi.util.tracker.*;

import aQute.bnd.annotation.component.*;

/**
 * Create the persistence provider for Eclipselink and delegate all the messages
 * to it.
 * 
 * SPEC:
 */
@Component(provide = {})
@SuppressWarnings("rawtypes")
public class DatanucleusBridge{
	static Version											baseVersion	= new Version(
																				"3.2");
	private BundleTracker									bt;

	@Activate
	public void activate(final BundleContext context) {
		bt = new BundleTracker<ServiceRegistration<PersistenceProvider>>(
				context,
				Bundle.ACTIVE + Bundle.STARTING,
				new BundleTrackerCustomizer<ServiceRegistration<PersistenceProvider>>() {

					@Override
					public ServiceRegistration<PersistenceProvider> addingBundle(
							Bundle bundle, BundleEvent event) {
						String name = bundle.getSymbolicName();
						if (name == null
								|| !name.equals("org.datanucleus.api.jpa"))
							return null;
						
						Version version = bundle.getVersion();
						if ( version == null || baseVersion.compareTo( version)> 0)
							return null;

						URL url = bundle.getResource("META-INF/services/" + PersistenceProvider.class.getName());
						if ( url == null)
							return null;
						
						ServiceLoader<PersistenceProvider> sl = ServiceLoader.load(PersistenceProvider.class, bundle.adapt(BundleWiring.class).getClassLoader());
						for ( PersistenceProvider pp : sl) {
							return bundle.getBundleContext().registerService(PersistenceProvider.class, new DatanucleusWrapper(pp), null);
						}
						return null;
					}

					@Override
					public void modifiedBundle(Bundle bundle,
							BundleEvent event,
							ServiceRegistration<PersistenceProvider> object) {
						// TODO Auto-generated method stub

					}

					@Override
					public void removedBundle(Bundle bundle, BundleEvent event,
							ServiceRegistration<PersistenceProvider> reg) {
						reg.unregister();
					}
				});
		bt.open();
	}
	
	@Deactivate
	void deactivate() {
		this.bt.close();
	}

	@Reference
	void setTm(TransactionManager tm) {
		// EclipselinkTransactionController.tm = tm;
	}

}
