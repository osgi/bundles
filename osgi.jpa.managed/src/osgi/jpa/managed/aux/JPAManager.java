package osgi.jpa.managed.aux;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.spi.PersistenceProvider;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.xml.bind.JAXB;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.util.tracker.BundleTracker;

import osgi.jpa.managed.api.JPABridgePersistenceProvider;
import v2_0.Persistence;
import v2_0.Persistence.PersistenceUnit;
import v2_0.Persistence.PersistenceUnit.Properties.Property;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.lib.converter.Converter;
import aQute.libg.clauses.Clauses;
import aQute.service.logger.Log;

/**
 * This component bridges JPA, TransactionManager, and DataSourceFactory
 * services into an Entity Manager for each matching Persistence Unit. An Entity
 * Manager is registered that will automatically create and enlist an Entity
 * Manager per transaction. Connections for the JTA Data Source in JPA are
 * automatically enlisted in the current transaction.
 */
@Component(designateFactory = JPAManager.Config.class, immediate = true)
public class JPAManager {

	static final String					META_PERSISTENCE	= "Meta-Persistence";

	BundleContext						context;
	TransactionManager					transactionManager;
	XADataSource						xaDataSource;
	PersistenceProvider					persistenceProvider;
	BundleTracker<PersistentBundle>		bundles;
	JPABridgeLogMessages				log;
	Map<String, Object>					bridgeProperties	= new HashMap<String, Object>();
	TransformersHook					transformersHook;
	ServiceRegistration<WeavingHook>	transformersHookRegistration;

	interface Config {

		String name();
	}

	Config	config;

	@Activate
	void activate(BundleContext context, Map<String, Object> props)
			throws Exception {
		this.context = context;
		config = Converter.cnv(Config.class, props);

		//
		// The Transformers hook enables weaving in OSGi
		// Every persistence unit will register itself with
		// the transformers hook. Order is important, once
		// the bundle tracker is called, the transformers
		// will be registered so do not move this method lower.
		//

		transformersHook = new TransformersHook(getImports(persistenceProvider));
		context.registerService(WeavingHook.class, transformersHook, null);

		//
		// Track bundles with persistence units.
		//

		bundles = new BundleTracker<PersistentBundle>(context, Bundle.ACTIVE
				+ Bundle.STARTING, null) {

			public PersistentBundle addingBundle(Bundle bundle,
					BundleEvent event) {
				try {
					//
					// Parse any persistence units, returns null (not tracked)
					// when there is no PU
					//
					return parse(bundle);
				} catch (Exception e) {
					e.printStackTrace();
					log.bundleParseException(bundle, e);
					return null;
				}
			}

			public void removedBundle(Bundle bundle, BundleEvent event,
					PersistentBundle put) {
				put.close();
			}
		};
		
		bundles.open();
	}

	@Deactivate
	void deactivate() {
		bundles.close();
	}

	/**
	 * Calculate the imports of the persistence provider. Since this guy is
	 * running, it must have satisfied all its imports. So we use those exact
	 * imports and add them to our transformed classes, this will ensure that
	 * any classes that the Persistence Provider needs from that class will be
	 * satisfied.
	 * 
	 * @param pp
	 *            Persistence Provider for this unit
	 * @return A list of import clauses from the provider
	 */
	static Pattern	WORD	= Pattern.compile("[a-zA-Z0-9]+");

	private List<String> getImports(PersistenceProvider pp) throws IOException {
		//
		// Check if this pp is a bridge that is aware of 
		// what we're doing
		//
		if ( pp instanceof JPABridgePersistenceProvider) {
			 List<String> wovenImports = ((JPABridgePersistenceProvider) pp).getWovenImports();
			 if ( wovenImports != null)
				 return wovenImports;
		}
		
		//
		// Get the pp's class's bundle's context
		//
		Bundle b;
		if ( pp instanceof BundleReference) 
			b = ((BundleReference) pp).getBundle();
		else
			b = FrameworkUtil.getBundle(pp.getClass());
		
		if (b != null) {
			//
			// Get the import clauses
			//
			Clauses clauses = Clauses.parse((String)b.getHeaders().get("Export-Package"), null);
			if (!clauses.isEmpty()) {
				List<String> list = new ArrayList<String>();
				for (Entry<String, Map<String, String>> e : clauses.entrySet()) {

					//
					// Create a new clause
					//
					StringBuilder sb = new StringBuilder();
					sb.append(e.getKey());
					for (Entry<String, String> ee : e.getValue().entrySet()) {
						if ( ee.getKey().endsWith(":"))
							continue;
						
						sb.append(";").append(ee.getKey()).append("=");
						String v = ee.getValue();
						if (WORD.matcher(v).matches())
							sb.append(ee.getValue());
						else
							sb.append("\"").append(ee.getValue()).append("\"");
					}
					list.add(sb.toString());
				}
				return list;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Check a bundle for persistence units following the rules in the OSGi
	 * spec.
	 * <p>
	 * A Persistence Bundle is a bundle that specifies the Meta-Persistence
	 * header, see Meta Persistence Header on page 439. This header refers to
	 * one or more Persistence Descriptors in the Persistence Bundle. Commonly,
	 * this is the META-INF/persistence.xml resource. This location is the
	 * standard for non- OSGi environments, however an OSGi bundle can also use
	 * other locations as well as multiple resources. Any entity classes must
	 * originate in the bundle’s JAR, it cannot come from a fragment. This
	 * requirement is necessary to simplify enhancing entity classes.
	 * 
	 * @param bundle
	 *            the bundle to be searched
	 * @return a Persistent Bundle or null if it has no matching persistence
	 *         units
	 */
	PersistentBundle parse(Bundle bundle) throws Exception {
		String metapersistence = (String) bundle.getHeaders().get(
				META_PERSISTENCE);

		if (metapersistence == null || metapersistence.trim().isEmpty())
			return null;

		//
		// We can have multiple persistence units.
		//

		Set<Persistence.PersistenceUnit> set = new HashSet<Persistence.PersistenceUnit>();
		for (String location : metapersistence.split("\\s*,\\s*")) {
			Property p = new Property();

			//
			// Lets remember where we came from
			//
			p.setName("location");
			p.setValue(location);

			//
			// Try to find the resource for the persistence unit
			// on the classpath: getResource
			//

			URL url = bundle.getResource(location);
			if (url == null) {
				log.locationWithoutPersistenceUnit(bundle, location);
			} else {
				Persistence persistence = JAXB
						.unmarshal(url, Persistence.class);
				for (Persistence.PersistenceUnit pu : persistence
						.getPersistenceUnit()) {

					if (config.name() == null || config.name().trim().isEmpty()
							|| config.name().equals("*")
							|| config.name().equals(pu.getName())) {
						if ( pu.getProperties() == null)
							pu.setProperties(new Persistence.PersistenceUnit.Properties());
						pu.getProperties().getProperty().add(p);
						String reason = isValid(pu);
						if (reason == null) {
							set.add(pu);
						} else {
							StringWriter sb = new StringWriter();
							JAXB.marshal(pu, sb);
							log.invalidPersistenceUnit(bundle, location,
									reason, sb.toString());
						}
					}
				}
			}
		}

		// Ignore this bundle if no valid PUs
		if (set.isEmpty())
			return null;

		return new PersistentBundle(this, bundle, set);
	}

	/**
	 * Validate against the name, provider, etc. TODO validate the PU
	 * 
	 * @param pu
	 *            The persistence unit to check
	 * @return A failure reason when it is not good or null if it is ok
	 */
	private String isValid(PersistenceUnit pu) {
		return null;
	}

	@Reference
	void setDataSourceFactory(XADataSource dsf) throws SQLException {
		this.xaDataSource = dsf;
	}

	@Reference
	void setLog(Log log) {
		this.log = log.logger(JPABridgeLogMessages.class);
	}

	@Reference
	void setPersistenceProvider(PersistenceProvider pp) {
		this.persistenceProvider = pp;
	}

	@Reference
	void setTransactionManager(TransactionManager tm) {
		this.transactionManager = tm;
	}

}
