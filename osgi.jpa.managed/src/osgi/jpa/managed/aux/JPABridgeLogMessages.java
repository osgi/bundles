package osgi.jpa.managed.aux;

import org.osgi.framework.*;

import aQute.service.logger.*;

/**
 * The log messages for the JPA Bridge application.
 */
interface JPABridgeLogMessages extends LogMessages {

	@Format("While parsing bundle %s for a persistence unit we encountered an unexpected exception %s. This bundle (also the other persistence units in this bundle) will be ignored.")
	ERROR bundleParseException(Bundle bundle, Exception e);

	@Format("Bundle %s specifies location %s in the Meta-Persistence header but no such resource is found in the bundle at that location. "
			+ "It might be possible that this is optional and supplied by a fragment. You might also want to check the Bundle-Classpath, "
			+ "the resource is searched on the classpath, not as a bundle entry.")
	WARNING locationWithoutPersistenceUnit(Bundle bundle, String location);

	@Format("Bundle %s specifies a persistence unit %s but this persistence unit is invalid because of %s. The content is %s")
	ERROR invalidPersistenceUnit(Bundle bundle, String location, String reason, String xml);

}