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

package osgi.jpa.managed.support;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import v2_0.Persistence;

/**
 * This class represents a bundle with one or more valid Persistence Units. It
 * maintains a the service registrations for the TransactionalEntityManager for
 * each persistence units and closes them when applicable.
 */
class PersistentBundle {

	private static final String										OSGI_UNIT_PROVIDER	= "osgi.unit.provider";
	private static final String										OSGI_UNIT_VERSION	= "osgi.unit.version";
	private static final String										OSGI_UNIT_NAME		= "osgi.unit.name";

	private final Set<ServiceRegistration<? extends EntityManager>>	units				= new HashSet<ServiceRegistration<? extends EntityManager>>();
	final JPAManager												bridge;
	final Bundle													bundle;

	/**
	 * We found some persistence units for this bridge so create this manager.
	 * 
	 * @param bridge the bridge we work for
	 * @param bundle the actual bundle for the persistence units
	 * @param set a set of persistence units.
	 */
	PersistentBundle(JPAManager bridge, Bundle bundle, Set<Persistence.PersistenceUnit> set) throws Exception {
		this.bridge = bridge;
		this.bundle = bundle;

		for (Persistence.PersistenceUnit pu : set) {
			units.add(createEM(pu));
		}
	}

	/**
	 * Create an Entity Manager that is configured for a given persistence unit.
	 * 
	 * @param pu The persistence unit we want the Entity Manager for.
	 * @return A Service Registration of the Entity Manager
	 */

	private ServiceRegistration<EntityManager> createEM(Persistence.PersistenceUnit pu) throws Exception {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl(this, pu);
		EntityManagerFactory emf = bridge.persistenceProvider.createContainerEntityManagerFactory(pui,
				bridge.bridgeProperties);

		Hashtable<String, Object> properties = new Hashtable<String, Object>(bridge.bridgeProperties);
		properties.put(OSGI_UNIT_NAME, pu.getName());
		properties.put(OSGI_UNIT_VERSION, bundle.getVersion());
		properties.put(OSGI_UNIT_PROVIDER, bridge.persistenceProvider.getClass().getName());

		bridge.log.step("Register Entity Manager for " + emf);
		return bridge.context.registerService(EntityManager.class, new TransactionalEntityManager(
				bridge.transactionManager, emf), properties);
	}

	/**
	 * Unregister all registrations and close the Entity Manager.
	 */
	public void close() {
		bridge.log.step("closing " + this);
		for (ServiceRegistration<? extends EntityManager> emi : units)
			try {
				EntityManager em = bridge.context.getService(emi.getReference());
				emi.unregister();
				em.close();
			} catch (Exception e) {
				bridge.log.failed("Closing " + emi, e);
			}
	}

	@Override
	public String toString() {
		return "PersistentBundle [bridge=" + bridge.config.name() + ", bundle=" + bundle.getBundleId() + "]";
	}
}
