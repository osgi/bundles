package osgi.jpa.managed.datanucleus.adapter;

import java.io.*;
import java.lang.instrument.*;
import java.net.*;
import java.security.*;
import java.util.*;

import javax.persistence.*;
import javax.persistence.spi.*;

import org.osgi.framework.*;
import org.osgi.framework.wiring.*;

import osgi.jpa.managed.api.*;

@SuppressWarnings("rawtypes")
public class DatanucleusWrapper implements PersistenceProvider,JPABridgePersistenceProvider {
	private PersistenceProvider	pp;

	public DatanucleusWrapper(
			PersistenceProvider pp) {
		this.pp = pp;
	}


	@Override
	public EntityManagerFactory createEntityManagerFactory(String emName,
			Map map) {
		// TODO Auto-generated method stub
		return null;
	}


	public int hashCode() {
		return pp.hashCode();
	}

	public boolean equals(Object obj) {
		return pp.equals(obj);
	}

	@SuppressWarnings("unchecked")
	public EntityManagerFactory createContainerEntityManagerFactory(
			PersistenceUnitInfo info, Map properties) {
		properties.put("datanucleus.jpa.addClassTransformer", "false");
		properties.put("datanucleus.plugin.pluginRegistryClassName",
				"org.datanucleus.plugin.OSGiPluginRegistry");
		
		info.addTransformer(new ClassTransformer() {
			
			@Override
			public byte[] transform(ClassLoader loader, String className,
					Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
					byte[] classfileBuffer) throws IllegalClassFormatException {

				
				// TODO Auto-generated method stub
				return null;
			}
		});
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

	@Override
	public ClassLoader getClassLoader(Bundle persistentUnitBundle) {
		ClassLoader cla = persistentUnitBundle.adapt(BundleWiring.class)
				.getClassLoader();
		final ClassLoader clb = getClass().getClassLoader();

		return new ClassLoader(cla) {

			@Override
			protected Class<?> findClass(String className)
					throws ClassNotFoundException {

				return clb.loadClass(className);
			}

			@Override
			protected URL findResource(String resource) {
				return clb.getResource(resource);
			}

			@Override
			protected Enumeration<URL> findResources(String resource)
					throws IOException {
				return clb.getResources(resource);
			}
		};
	}

	@Override
	public String getPersistentProviderClassName() {
		return pp.getClass().getName();
	}

}