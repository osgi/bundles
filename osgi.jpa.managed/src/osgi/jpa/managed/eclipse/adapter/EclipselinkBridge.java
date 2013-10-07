package osgi.jpa.managed.eclipse.adapter;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.persistence.*;
import javax.persistence.spi.*;
import javax.transaction.*;

import org.osgi.framework.*;
import org.osgi.framework.wiring.*;

import osgi.jpa.managed.api.*;
import aQute.bnd.annotation.component.*;

/**
 * Create the persistence provider for Eclipselink and delegate all the messages
 * to it. SPEC:
 */
@Component(provide = PersistenceProvider.class, properties = "service.vendor=EclipseLink")
@SuppressWarnings("rawtypes")
public class EclipselinkBridge implements PersistenceProvider, JPABridgePersistenceProvider {
	private org.eclipse.persistence.jpa.PersistenceProvider	pp;

	@Activate
	public void activate(BundleContext context) {
		pp = new org.eclipse.persistence.jpa.PersistenceProvider();
	}

	public int hashCode() {
		return pp.hashCode();
	}

	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		return pp.createEntityManagerFactory(persistenceUnitName, properties);
	}

	public boolean equals(Object obj) {
		return pp.equals(obj);
	}

	@SuppressWarnings("unchecked")
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		properties.put("eclipselink.logging.level", "FINEST");
		properties.put("eclipselink.ddl-generation", "create-or-extend-tables");
		properties.put("eclipselink.target-server", EclipselinkTransactionController.class.getName());
		return pp.createContainerEntityManagerFactory(info, properties);
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

	@Override
	public List<String> getWovenImports() {
		return Arrays.asList();
	}

	@Override
	public URL getRootUrl(Bundle b, String location) {
		if (location.lastIndexOf('/') > 0) {
			location = location.substring(0, location.lastIndexOf('/'));
		}
		return b.getResource(location);
	}

	@Reference
	void setTm(TransactionManager tm) {
		EclipselinkTransactionController.tm = tm;
	}

	@Override
	public ClassLoader getClassLoader(Bundle persistentUnitBundle) {
		ClassLoader cla = persistentUnitBundle.adapt(BundleWiring.class).getClassLoader();
		final ClassLoader clb = getClass().getClassLoader();

		return new ClassLoader(cla) {

			@Override
			protected Class< ? > findClass(String className) throws ClassNotFoundException {

				return clb.loadClass(className);
			}

			@Override
			protected URL findResource(String resource) {
				return clb.getResource(resource);
			}

			@Override
			protected Enumeration<URL> findResources(String resource) throws IOException {
				return clb.getResources(resource);
			}
		};
	}

	@Override
	public String getPersistentProviderClassName() {
		return null;
	}
}
