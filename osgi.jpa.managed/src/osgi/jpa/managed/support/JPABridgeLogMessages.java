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

package osgi.jpa.managed.support;

import org.osgi.framework.Bundle;
import aQute.service.logger.Format;
import aQute.service.logger.LogMessages;

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
