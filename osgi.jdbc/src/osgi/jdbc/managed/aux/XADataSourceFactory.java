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

package osgi.jdbc.managed.aux;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.sql.XADataSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jdbc.DataSourceFactory;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Meta;
import aQute.lib.converter.Converter;

/**
 * This component configures an XA Data Source from a Data Source Factory. It
 * uses DS's integration with Config Admin to get all the properties.
 * <p>
 * We create an XA DataSource since this can be used to create non-transactional
 * connections.
 */

@Component(designateFactory = XADataSourceFactory.Config.class)
public class XADataSourceFactory {
	private DataSourceFactory	dsf;
	private XADataSource		ds;

	/**
	 * The configuration interface. Specifies the
	 */
	@Meta.OCD(description = "Creates an XA Data Source")
	interface Config {
		@Meta.AD(description = "The jdbc url, e.g. `hdbc:h2:mem:`", required = true)
		String url();

		@Meta.AD(description = "The user id (can often also encoded in the URL)", required = false)
		String user();

		@Meta.AD(description = "The password (can often also encoded in the URL)", required = false)
		String _password();

		@Meta.AD(description = "An optional name for this datasource", required = false)
		String name();

		@Meta.AD(description = "Optional filter for selecting a target Data Source Factory", required = false)
		String dataSourceFactory_target();
	}

	private Config					config;
	private ServiceRegistration<?>	registration;

	/**
	 * Awfully simple. Just use the Data Source Factory to create a Data Source.
	 * We delegate all the XA Data Source calls t this Data Source.
	 * 
	 * @param properties the DS properties (also from Config admin)
	 * @throws Exception
	 */
	@Activate
	void activate(BundleContext context, Map<String, Object> properties) throws Exception {
		config = Converter.cnv(Config.class, properties);
		assert config.url() != null;

		Properties props = new Properties();

		props.put(DataSourceFactory.JDBC_URL, config.url());

		if (config.user() != null && config._password() != null) {
			props.setProperty(DataSourceFactory.JDBC_USER, config.user());
			props.setProperty(DataSourceFactory.JDBC_PASSWORD, config._password());
		}

		ds = dsf.createXADataSource(props);
		registration = context.registerService(XADataSource.class.getName(), ds, new Hashtable<String, Object>(
				properties));
	}

	@Deactivate
	void deactivate() {
		registration.unregister();
	}

	/**
	 * Reference to the Data Source Factory created by the Database driver. The
	 * method is public because the name is used as a configuration parameter to
	 * select specific Data Source Factory services.
	 * 
	 * @param dsf the Data Source factory from the db provider.
	 */
	@Reference
	public void setDataSourceFactory(DataSourceFactory dsf) {
		this.dsf = dsf;
	}
}
