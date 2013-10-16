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

package osgi.jpa.managed.jdbc.test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import javax.sql.XADataSource;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;
import aQute.bnd.annotation.component.Reference;
import aQute.test.dummy.ds.DummyDS;
import aQute.test.dummy.log.DummyLog;

public class JDBCManagedTest extends TestCase {

	public static final String								MYSQL_URL				= "jdbc:mysql:///test_jdbc?user=root&password=&createDatabaseIfNotExist=true";
	public static final String								XA_DATA_SOURCE_FACTORY	= "osgi.jdbc.managed.aux.XADataSourceFactory";
	public static final String								H2_URL					= "jdbc:h2:mem:test";

	@Rule
	TemporaryFolder											folder					= new TemporaryFolder();
	private ConfigurationAdmin								cm;
	private BundleContext									context					= FrameworkUtil.getBundle(getClass())
																							.getBundleContext();
	ServiceTracker<XADataSource, XADataSource>				xtds					= new ServiceTracker<XADataSource, XADataSource>(
																							context, XADataSource.class,
																							null);
	ServiceTracker<DataSourceFactory, DataSourceFactory>	dsf						= new ServiceTracker<DataSourceFactory, DataSourceFactory>(
																							context,
																							DataSourceFactory.class, null);

	@Override
	public void setUp() throws Exception {
		try {
			DummyDS ds = new DummyDS();
			ds.setContext(context);
			ds.add(this);
			ds.add(new DummyLog());
			ds.wire();
		} catch (Exception e) {
			e.printStackTrace();
			Thread.sleep(1000000);
		}
		xtds.open();
		dsf.open();
	}

	@Reference()
	void setCM(ConfigurationAdmin cm) {
		assertNotNull(cm);
		this.cm = cm;
	}

	/**
	 * Check if we can create a H2 DataSource through the JDBC bundle.
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void testH2() throws InterruptedException, IOException, SQLException {
		// Make sure no DataSource service yet exists
		assertNull(xtds.getService());

		//
		// Create a configuration that will create a H2 DataSource
		//
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("url", H2_URL);
		props.put("dataSourceFactory.target", "(" + DataSourceFactory.OSGI_JDBC_DRIVER_CLASS + "=org.h2.Driver)");

		Configuration c = cm.createFactoryConfiguration(XA_DATA_SOURCE_FACTORY, null);
		c.update(props);
		try {

			XADataSource service = xtds.waitForService(20000);
			assertEquals(1, xtds.size());
			assertNotNull(service);

			Connection connection = service.getXAConnection().getConnection();
			assertNotNull(connection);
			DatabaseMetaData metaData = connection.getMetaData();
			assertNotNull(metaData);
			assertEquals(metaData.getURL(), H2_URL);
			assertEquals(metaData.getDatabaseProductName(), "H2");
			Statement statement = connection.createStatement();
			statement.execute("create table blub;");
			connection.close();
		} finally {
			c.delete();
		}
		checkGone();
	}

	/**
	 * Test MySQL
	 * 
	 * @throws Exception
	 */
	public void testMySQL() throws Exception {
		// Make sure no DataSource service yet exists
		assertNull(xtds.getService());

		//
		// Create a configuration that will create a H2 DataSource
		//
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("url", MYSQL_URL);
		props.put("dataSourceFactory.target", "(" + DataSourceFactory.OSGI_JDBC_DRIVER_CLASS
				+ "=com.mysql.jdbc.Driver)");

		Configuration c = cm.createFactoryConfiguration(XA_DATA_SOURCE_FACTORY, null);
		c.update(props);
		try {

			XADataSource service = xtds.waitForService(20000);
			assertEquals(1, xtds.size());
			assertNotNull(service);

			Connection connection = service.getXAConnection().getConnection();
			assertNotNull(connection);
			DatabaseMetaData metaData = connection.getMetaData();
			assertNotNull(metaData);
			assertEquals(metaData.getURL(), MYSQL_URL);
			assertEquals(metaData.getDatabaseProductName(), "MySQL");
			Statement statement = connection.createStatement();
			statement.execute("drop table blub;");
			statement.execute("create table blub ( first int);");
			connection.close();
		} catch (Exception e) {
			// if no MySQL is installed we ignore this test
			if (e.getClass().getName().equals("com.mysql.jdbc.exceptions.jdbc4.CommunicationsException"))
				return;

			throw e;
		} finally {
			c.delete();
		}
		checkGone();

	}

	private void checkGone() throws InterruptedException {
		for (int i = 0; i < 500; i++) {
			if (xtds.getService() == null) {
				System.out.println("done");
				return;
			}
			Thread.sleep(100);
		}
		fail("service still there");
	}
}
