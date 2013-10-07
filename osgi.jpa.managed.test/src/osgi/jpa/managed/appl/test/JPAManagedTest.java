package osgi.jpa.managed.appl.test;

import java.io.*;
import java.util.*;

import javax.persistence.*;
import javax.transaction.*;

import junit.framework.*;

import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.jdbc.*;
import org.osgi.util.tracker.*;

import osgi.jpa.managed.jdbc.test.*;
import aQute.bnd.annotation.component.*;
import aQute.test.dummy.ds.*;
import aQute.test.dummy.log.*;

@Component
public class JPAManagedTest extends TestCase {
	private BundleContext		context	= FrameworkUtil.getBundle(getClass()).getBundleContext();
	private TransactionManager	tm;
	private ConfigurationAdmin	cm;

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
	}

	public void testH2AndHibernate() throws Throwable {
		Configuration db = startH2();
		Configuration jpa = startHibernate();
		try {
			assertDb();
		}
		finally {
			db.delete();
			jpa.delete();
		}
	}

	public void testH2AndEclipseLink() throws Throwable {
		Configuration db = startH2();
		Configuration jpa = startEclipseLink();
		try {
			assertDb();
		}
		finally {
			db.delete();
			jpa.delete();
		}
	}

	private void assertDb() throws InterruptedException, Throwable {
		ServiceTracker<EntityManager,EntityManager> ems = new ServiceTracker<EntityManager,EntityManager>(context,
				EntityManager.class, null);
		ems.open();
		EntityManager em = ems.waitForService(10000);
		assertNotNull(em);

		try {
			long key = 0;
			tm.setTransactionTimeout(100000);
			tm.begin();
			try {
				Domain d = new Domain();

				d.setName("blabla");

				// SPEC: generated values are not shown with
				// mysql until you do flush

				em.persist(d);
				em.flush();
				System.out.println(d);
				key = d.getId();
				assertNotNull(key);
				tm.commit();
			}
			catch (Exception e) {
				e.printStackTrace();
				tm.rollback();
				throw e;
			}

			tm.begin();
			try {
				Domain find = em.find(Domain.class, key);
				assertNotNull(find);
				assertEquals(key, find.getId());
				assertEquals("blabla", find.getName());
				tm.commit();
			}
			catch (Exception e) {
				e.printStackTrace();
				tm.rollback();
				throw e;
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	private Configuration startHibernate() throws IOException {
		Hashtable<String,String> props = new Hashtable<String,String>();
		props.put("name", "*");
		props.put("persistenceProvider.target", "(service.vendor=Hibernate)");
		Configuration c = cm.createFactoryConfiguration("osgi.jpa.managed.aux.JPAManager", null);
		c.update(props);
		return c;
	}

	private Configuration startEclipseLink() throws IOException {
		Hashtable<String,String> props = new Hashtable<String,String>();
		props.put("name", "*");
		props.put("persistenceProvider.target", "(service.vendor=EclipseLink)");
		Configuration c = cm.createFactoryConfiguration("osgi.jpa.managed.aux.JPAManager", null);
		c.update(props);
		return c;
	}

	private Configuration startH2() throws IOException {
		Hashtable<String,String> props = new Hashtable<String,String>();
		props.put("url", JDBCManagedTest.H2_URL);
		props.put("dataSourceFactory.target", "(" + DataSourceFactory.OSGI_JDBC_DRIVER_CLASS + "=org.h2.Driver)");

		Configuration c = cm.createFactoryConfiguration(JDBCManagedTest.XA_DATA_SOURCE_FACTORY, null);
		c.update(props);

		return c;
	}

	// void activate() throws NotSupportedException, SystemException,
	// IllegalStateException, SecurityException, HeuristicMixedException,
	// HeuristicRollbackException, RollbackException {
	// System.out.println("JPAManagedTest:" + em);
	// try {
	// long key = 0;
	// tm.setTransactionTimeout(100000);
	// tm.begin();
	// try {
	// Domain d = new Domain();
	//
	// d.setName("blabla");
	//
	// // SPEC: generated values are not shown with
	// // mysql until you do flush
	//
	// em.persist(d);
	// em.flush();
	// System.out.println(d);
	// key = d.getId();
	// tm.commit();
	// } catch (Exception e) {
	// e.printStackTrace();
	// tm.rollback();
	// }
	//
	// tm.begin();
	// try {
	// Domain find = em.find(Domain.class, key);
	// System.out.println(find);
	// tm.commit();
	// } catch (Exception e) {
	// e.printStackTrace();
	// tm.rollback();
	// }
	// } catch (Throwable t) {
	// t.printStackTrace();
	// }
	// }

	@Reference
	void setTM(TransactionManager tm) {
		this.tm = tm;
	}

	@Reference
	void setConfigurationAdmin(ConfigurationAdmin cm) {
		this.cm = cm;
	}
}
