package osgi.jpa.managed.aux;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.persistence.*;
import javax.persistence.spi.*;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.*;

import org.osgi.framework.wiring.*;

import osgi.jpa.managed.api.*;
import v2_0.Persistence.PersistenceUnit;
import v2_0.Persistence.PersistenceUnit.Properties.Property;
import v2_0.*;
import aQute.lib.io.*;

/**
 * This class is the interface between the bridge (the manager) and the
 * Persistence Provider. It is used by the Persistence Provider to get all
 * context information. We create one of these for each persistence unit found
 * in a bundle.
 */
class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
	private final PersistenceUnit			persistenceUnitXml;
	private final PersistentBundle			sourceBundle;
	private final String					location;
	// TODO can we have more than one transformer? Likely not
	private final List<ClassTransformer>	transformers	= new ArrayList<ClassTransformer>();
	private DataSourceWrapper				jtadatasource;
	private DataSourceWrapper				nonjtadatasource;

	/**
	 * Create a new Persistence Unit Info
	 * 
	 * @param bundle
	 *            the source bundle
	 * @param xml
	 *            The xml of the persistence unit
	 */
	PersistenceUnitInfoImpl(PersistentBundle bundle, PersistenceUnit xml)
			throws Exception {
		this.sourceBundle = bundle;
		this.persistenceUnitXml = xml;
		this.location = (String) getProperties().get("location");
	}

	/**
	 * Shutdown this persistence unit
	 */
	void shutdown() {
		for (ClassTransformer classTransformer : transformers) {
			sourceBundle.bridge.transformersHook.unregister(
					sourceBundle.bundle, classTransformer);
		}
	}

	/**
	 * Add a new transformer. SPEC: can this be called multiple times?
	 * 
	 * @see javax.persistence.spi.PersistenceUnitInfo#addTransformer(javax.persistence.spi.ClassTransformer)
	 */
	@Override
	public void addTransformer(ClassTransformer transformer) {
		try {
			sourceBundle.bridge.transformersHook.register(sourceBundle.bundle,
					transformer);
			if (transformer != null)
				transformers.add(transformer);
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#excludeUnlistedClasses()
	 */
	@Override
	public boolean excludeUnlistedClasses() {
		Boolean b = persistenceUnitXml.isExcludeUnlistedClasses();
		return b == null || b.booleanValue();
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#getClassLoader()
	 */
	@Override
	public synchronized ClassLoader getClassLoader() {
		sourceBundle.bridge.log.step("classloader " + getPersistenceUnitName());
		if ( sourceBundle.bridge.persistenceProvider instanceof JPABridgePersistenceProvider) {
			ClassLoader cl = ((JPABridgePersistenceProvider)sourceBundle.bridge.persistenceProvider).getClassLoader(sourceBundle.bundle);
			if (cl != null)
				return cl;
		}
		return sourceBundle.bundle.adapt(BundleWiring.class).getClassLoader();
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#getJarFileUrls()
	 */
	@Override
	public List<URL> getJarFileUrls() {
		try {
			List<URL> urls = new ArrayList<URL>();
			for (String url : persistenceUnitXml.getJarFile()) {
				urls.add(new URL(url));
			}
			return urls;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * We hand out a proxy that automatically enlists any connections on the
	 * current transaction.
	 * 
	 * @see javax.persistence.spi.PersistenceUnitInfo#getJtaDataSource()
	 */
	@Override
	public synchronized DataSourceWrapper getJtaDataSource() {
		System.out.println("get jta data source " + getPersistenceUnitName());

		try {
			if (jtadatasource == null) {
				jtadatasource = new DataSourceWrapper(
						sourceBundle.bridge.transactionManager,
						sourceBundle.bridge.xaDataSource, true,
						sourceBundle.bridge.log);
			}
			return jtadatasource;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#getManagedClassNames()
	 */
	@Override
	public List<String> getManagedClassNames() {
		return persistenceUnitXml.getClazz();
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#getMappingFileNames()
	 */
	@Override
	public List<String> getMappingFileNames() {
		return persistenceUnitXml.getMappingFile();
	}

	/**
	 * In this method we just create a simple temporary class loader. This class
	 * loader uses the bundle's class loader as parent but defines the classes
	 * in this class loader. This has the implicit assumption that the temp
	 * class loader is used BEFORE any bundle's classes are loaded since a class
	 * loader does parent delegation first. Sigh, guess it works most of the
	 * time. There is however, no good alternative though in OSGi we could
	 * actually refresh the bundle. TODO solve this ordering problem
	 * 
	 * @see javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()
	 */
	@Override
	public ClassLoader getNewTempClassLoader() {

		return new ClassLoader(getClassLoader()) {

			//
			// Use the bunde's resource interface to get the
			// bytes of the classes and define them in this
			// loader. Yuck.
			//
			@Override
			protected Class<?> findClass(String className)
					throws ClassNotFoundException {

				//
				// Use path of class, then get the resource
				//

				String path = className.replace('.', '/').concat(".class");
				URL resource = getParent().getResource(path);
				if (resource == null)
					throw new ClassNotFoundException(className
							+ " as resource " + path + " in " + getParent());

				try {

					//
					// Collect the resource's data and define
					// the class
					// TODO wonder how this works with security since
					// we have no protection domain?
					//

					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					IO.copy(resource.openStream(), bout);
					byte[] buffer = bout.toByteArray();
					return defineClass(className, buffer, 0, buffer.length);
				} catch (Exception e) {
					throw new ClassNotFoundException(className + " as resource"
							+ path + " in " + getParent(), e);
				}
			}

			//
			// Look for resources in our bundle
			//

			@Override
			protected URL findResource(String resource) {
				return getParent().getResource(resource);
			}

			@Override
			protected Enumeration<URL> findResources(String resource)
					throws IOException {
				return getParent().getResources(resource);
			}

		};
	}

	/**
	 * This Data Source is based on a XA Data Source but will not be enlisted in
	 * a transaction.
	 * 
	 * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
	 */
	@Override
	public DataSource getNonJtaDataSource() {
		try {
			if (nonjtadatasource == null) {
				nonjtadatasource = new DataSourceWrapper(
						sourceBundle.bridge.transactionManager,
						sourceBundle.bridge.xaDataSource, false,
						sourceBundle.bridge.log);
			}
			return nonjtadatasource;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * @see
	 * javax.persistence.spi.PersistenceUnitInfo#getPersistenceProviderClassName
	 * ()
	 */
	@Override
	public String getPersistenceProviderClassName() {
		PersistenceProvider pp = sourceBundle.bridge.persistenceProvider;
		if (pp instanceof JPABridgePersistenceProvider) {
			String className = ((JPABridgePersistenceProvider) pp).getPersistentProviderClassName();
			if ( className != null)
				return className;
		}
		return sourceBundle.bridge.persistenceProvider.getClass().getName();
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitName()
	 */
	@Override
	public String getPersistenceUnitName() {
		return persistenceUnitXml.getName();
	}

	/*
	 * @see
	 * javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitRootUrl()
	 */
	@Override
	public URL getPersistenceUnitRootUrl() {
		//
		// Check if we have an override
		//
		PersistenceProvider pp = sourceBundle.bridge.persistenceProvider;
		if (pp instanceof JPABridgePersistenceProvider) {
			URL rootUrl = ((JPABridgePersistenceProvider) pp).getRootUrl(
					sourceBundle.bundle, location);
			return rootUrl;
		}
		
		//
		// Make one that is OSGi based
		//
		
		String loc = location;
		int n = loc.lastIndexOf('/');
		if (n > 0) {
			loc = loc.substring(0, n);
		}
		if (loc.isEmpty())
			loc = "/";

		return sourceBundle.bundle.getResource(loc);
	}

	/*
	 * TODO handle also version 2.1. This btw seems to have the same schema
	 * exact for the version?
	 * 
	 * @see
	 * javax.persistence.spi.PersistenceUnitInfo#getPersistenceXMLSchemaVersion
	 * ()
	 */
	@Override
	public String getPersistenceXMLSchemaVersion() {
		return "2.0";
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#getProperties()
	 */
	@Override
	public Properties getProperties() {
		Properties properties = new Properties();
		if (persistenceUnitXml.getProperties() != null
				&& persistenceUnitXml.getProperties().getProperty() != null)
			for (Property p : persistenceUnitXml.getProperties().getProperty()) {
				properties.put(p.getName(), p.getValue());
			}
		properties.put("hibernate.transaction.manager_lookup_class", "xyz");
		return properties;
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
	 */
	@Override
	public SharedCacheMode getSharedCacheMode() {
		PersistenceUnitCachingType sharedCacheMode = persistenceUnitXml
				.getSharedCacheMode();
		if (sharedCacheMode == null)
			return null;

		return SharedCacheMode.valueOf(sharedCacheMode.name());
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#getTransactionType()
	 */
	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return PersistenceUnitTransactionType.JTA;
	}

	/*
	 * @see javax.persistence.spi.PersistenceUnitInfo#getValidationMode()
	 */
	@Override
	public ValidationMode getValidationMode() {
		PersistenceUnitValidationModeType validationMode = persistenceUnitXml
				.getValidationMode();
		if (validationMode == null)
			return null;

		return ValidationMode.valueOf(validationMode.name());
	}

}
