package osgi.jpa.managed.api;

import java.net.*;
import java.util.*;

import org.osgi.framework.*;

/**
 * Can be implemented by bundles that bridge a specific Perisistence Provider.
 */
public interface JPABridgePersistenceProvider {
	/**
	 * Provide the extra imports.
	 * 
	 * @return
	 */
	List<String> getWovenImports();

	/**
	 * Not clear what the spec is for the root. Hibernate could live with null
	 * but EclipseLink wants a value
	 */

	URL getRootUrl(Bundle b, String location);

	/**
	 * The bundle for the persistent unit bundle
	 * 
	 * @param persistentUnitBundle
	 * @return
	 */
	ClassLoader getClassLoader(Bundle persistentUnitBundle);

	String getPersistentProviderClassName();
}
