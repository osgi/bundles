package osgi.jpa.managed.jdbc.test;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.sql.*;

import junit.framework.*;

import org.junit.*;
import org.junit.rules.*;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.jdbc.*;
import org.osgi.util.tracker.*;

import aQute.bnd.annotation.component.*;
import aQute.test.dummy.ds.*;
import aQute.test.dummy.log.*;

public class JDBCManagedTest extends TestCase {

	public static final String							MYSQL_URL				= "jdbc:mysql:///test_jdbc?user=root&password=&createDatabaseIfNotExist=true";
	public static final String							XA_DATA_SOURCE_FACTORY	= "osgi.jdbc.managed.aux.XADataSourceFactory";
	public static final String							H2_URL					= "jdbc:h2:mem:test";

	@Rule
	TemporaryFolder										folder					= new TemporaryFolder();
	private ConfigurationAdmin							cm;
	private BundleContext								context					= FrameworkUtil.getBundle(getClass())
																						.getBundleContext();
	ServiceTracker<XADataSource,XADataSource>			xtds					= new ServiceTracker<XADataSource,XADataSource>(
																						context, XADataSource.class,
																						null);
	ServiceTracker<DataSourceFactory,DataSourceFactory>	dsf						= new ServiceTracker<DataSourceFactory,DataSourceFactory>(
																						context,
																						DataSourceFactory.class, null);

	@Before
	public void setUp() throws Exception {
		try {
			DummyDS ds = new DummyDS();
			ds.setContext(context);
			ds.add(this);
			ds.add(new DummyLog());
			ds.wire();
		}
		catch (Exception e) {
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
	 */
	public void testH2() throws InterruptedException, IOException, SQLException {
		// Make sure no DataSource service yet exists
		assertNull(xtds.getService());

		//
		// Create a configuration that will create a H2 DataSource
		//
		Hashtable<String,String> props = new Hashtable<String,String>();
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
		}
		finally {
			c.delete();
		}
		checkGone();
	}

	/**
	 * Test MySQL
	 */
	public void testMySQL() throws Exception {
		// Make sure no DataSource service yet exists
		assertNull(xtds.getService());

		//
		// Create a configuration that will create a H2 DataSource
		//
		Hashtable<String,String> props = new Hashtable<String,String>();
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
		}
		catch (Exception e) {
			// if no MySQL is installed we ignore this test
			if (e.getClass().getName().equals("com.mysql.jdbc.exceptions.jdbc4.CommunicationsException"))
				return;

			throw e;
		}
		finally {
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