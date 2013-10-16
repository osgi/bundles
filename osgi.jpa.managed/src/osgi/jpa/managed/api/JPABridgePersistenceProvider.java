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

package osgi.jpa.managed.api;

import java.net.URL;
import java.util.List;
import org.osgi.framework.Bundle;

/**
 * Can be implemented by bundles that bridge a specific Perisistence Provider.
 */
public interface JPABridgePersistenceProvider {
	/**
	 * Provide the extra imports.
	 * 
	 * @return a list of imports to add to the woven class.
	 */
	List<String> getWovenImports();

	/**
	 * Not clear what the spec is for the root. Hibernate could live with null
	 * but EclipseLink wants a value
	 * 
	 * @param b The persistence unit bundle.
	 * @param location The location of the persistence unit in the bundle.
	 * @return the URL (or null) for this root url
	 */

	URL getRootUrl(Bundle b, String location);

	/**
	 * The class loader for the persistent unit bundle. Some providers need a
	 * special class loader setup to add additional resources.
	 * 
	 * @param b the Persistent Unit Bundle
	 * @return the class loader for this persistence unit.
	 */
	ClassLoader getClassLoader(Bundle b);

	String getPersistentProviderClassName();
}
