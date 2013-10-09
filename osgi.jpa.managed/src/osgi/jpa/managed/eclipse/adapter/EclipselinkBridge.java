
package osgi.jpa.managed.eclipse.adapter;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import javax.transaction.TransactionManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import osgi.jpa.managed.api.JPABridgePersistenceProvider;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

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
			protected Class<?> findClass(String className) throws ClassNotFoundException {

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
