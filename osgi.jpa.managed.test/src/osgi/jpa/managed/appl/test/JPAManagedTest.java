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

package osgi.jpa.managed.appl.test;

import java.io.IOException;
import java.util.Hashtable;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import junit.framework.TestCase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;
import osgi.jpa.managed.jdbc.test.JDBCManagedTest;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.test.dummy.ds.DummyDS;
import aQute.test.dummy.log.DummyLog;

@Component
public class JPAManagedTest extends TestCase {
	private BundleContext		context	= FrameworkUtil.getBundle(getClass()).getBundleContext();
	private TransactionManager	tm;
	private ConfigurationAdmin	cm;

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
	}

	public void testH2AndHibernate() throws Throwable {
		Configuration db = startH2();
		Configuration jpa = startHibernate();
		try {
			assertDb();
		} finally {
			db.delete();
			jpa.delete();
		}
	}

	public void testH2AndEclipseLink() throws Throwable {
		Configuration db = startH2();
		Configuration jpa = startEclipseLink();
		try {
			assertDb();
		} finally {
			db.delete();
			jpa.delete();
		}
	}

	private void assertDb() throws InterruptedException, Throwable {
		ServiceTracker<EntityManager, EntityManager> ems = new ServiceTracker<EntityManager, EntityManager>(context,
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
			} catch (Exception e) {
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
			} catch (Exception e) {
				e.printStackTrace();
				tm.rollback();
				throw e;
			}
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	private Configuration startHibernate() throws IOException {
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("name", "*");
		props.put("persistenceProvider.target", "(service.vendor=Hibernate)");
		Configuration c = cm.createFactoryConfiguration("osgi.jpa.managed.aux.JPAManager", null);
		c.update(props);
		return c;
	}

	private Configuration startEclipseLink() throws IOException {
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("name", "*");
		props.put("persistenceProvider.target", "(service.vendor=EclipseLink)");
		Configuration c = cm.createFactoryConfiguration("osgi.jpa.managed.aux.JPAManager", null);
		c.update(props);
		return c;
	}

	private Configuration startH2() throws IOException {
		Hashtable<String, String> props = new Hashtable<String, String>();
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
