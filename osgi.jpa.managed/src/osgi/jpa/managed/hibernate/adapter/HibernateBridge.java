package osgi.jpa.managed.hibernate.adapter;

import java.net.*;
import java.util.*;

import javax.persistence.*;
import javax.persistence.spi.*;
import javax.transaction.*;

import org.hibernate.jpa.*;
import org.osgi.framework.*;

import osgi.jpa.managed.api.*;
import aQute.bnd.annotation.component.*;

/**
 * Create the persistence provider for hibernate and delegate all the messages
 * to it. SPEC:
 */
@Component(provide = PersistenceProvider.class, properties = "service.vendor=Hibernate")
@SuppressWarnings("rawtypes")
public class HibernateBridge implements PersistenceProvider, JPABridgePersistenceProvider {
	private HibernatePersistenceProvider	pp;
	private TransactionManager				tm;

	@Activate
	public void activate(BundleContext context) {
		pp = new org.hibernate.jpa.HibernatePersistenceProvider();
		JtaPlatformProviderImpl.transactionManager = tm;
	}

	public int hashCode() {
		return pp.hashCode();
	}

	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			return pp.createEntityManagerFactory(persistenceUnitName, properties);
		}
		finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	public boolean equals(Object obj) {
		return pp.equals(obj);
	}

	@SuppressWarnings("unchecked")
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			info.addTransformer(null);

			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			properties.put("hibernate.jdbc.use_get_generated_keys", "true");
			properties.put("hibernate.hbm2ddl.auto", "create");
			final EntityManagerFactory emf = pp.createContainerEntityManagerFactory(info, properties);
			return new DelegatedEntityManagerFactory(emf) {

			};
		}
		finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	public void generateSchema(PersistenceUnitInfo info, Map map) {
		pp.generateSchema(info, map);
	}

	public boolean generateSchema(String persistenceUnitName, Map map) {
		return pp.generateSchema(persistenceUnitName, map);
	}

	public ProviderUtil getProviderUtil() {
		return pp.getProviderUtil();
	}

	public String toString() {
		return pp.toString();
	}

	/**
	 * Return the imports for this provider needed on a proxy
	 */
	@Override
	public List<String> getWovenImports() {
		return Arrays.asList("*");
	}

	/**
	 * Hibernate could seemingly live with a null URL ... and it crashed on an
	 * OSGi URL ...
	 */
	@Override
	public URL getRootUrl(Bundle b, String location) {
		return null;
	}

	@Reference
	void setTM(TransactionManager tm) {
		this.tm = tm;
	}

	@Override
	public ClassLoader getClassLoader(Bundle b) {
		return null;
	}

	@Override
	public String getPersistentProviderClassName() {
		return null;
	}

}
