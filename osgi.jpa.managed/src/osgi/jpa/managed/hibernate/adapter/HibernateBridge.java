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

package osgi.jpa.managed.hibernate.adapter;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import javax.transaction.TransactionManager;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import osgi.jpa.managed.api.JPABridgePersistenceProvider;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

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

	@Override
	public int hashCode() {
		return pp.hashCode();
	}

	@Override
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			return pp.createEntityManagerFactory(persistenceUnitName, properties);
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return pp.equals(obj);
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			//
			// This is necessary to ensure the imports?
			//
			info.addTransformer(null);

			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			properties.put("hibernate.jdbc.use_get_generated_keys", "true");
			properties.put("hibernate.hbm2ddl.auto", "create");
			final EntityManagerFactory emf = pp.createContainerEntityManagerFactory(info, properties);
			return new DelegatedEntityManagerFactory(emf) {

			};
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	@Override
	public void generateSchema(PersistenceUnitInfo info, Map map) {
		pp.generateSchema(info, map);
	}

	@Override
	public boolean generateSchema(String persistenceUnitName, Map map) {
		return pp.generateSchema(persistenceUnitName, map);
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return pp.getProviderUtil();
	}

	@Override
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
