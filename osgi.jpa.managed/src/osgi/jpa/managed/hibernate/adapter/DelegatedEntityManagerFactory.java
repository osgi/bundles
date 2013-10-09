
package osgi.jpa.managed.hibernate.adapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.SynchronizationType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

@SuppressWarnings("rawtypes")
public class DelegatedEntityManagerFactory implements EntityManagerFactory {

	private final EntityManagerFactory	emf;

	public EntityManager createEntityManager() {
		return createEntityManager(SynchronizationType.UNSYNCHRONIZED, Collections.emptyMap());
	}

	public EntityManager createEntityManager(Map map) {
		return emf.createEntityManager(SynchronizationType.UNSYNCHRONIZED, map);
	}

	public EntityManager createEntityManager(SynchronizationType synchronizationType) {
		return emf.createEntityManager(synchronizationType, Collections.emptyMap());
	}

	public EntityManager createEntityManager(final SynchronizationType synchronizationType, final Map map) {
		return new EntityManager() {
			final EntityManager	em	= emf.createEntityManager(synchronizationType, map);
			final ClassLoader	cl	= getClass().getClassLoader();
			ClassLoader			tccl;

			private void begin() {
				tccl = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(cl);
			}

			private void end() {
				Thread.currentThread().setContextClassLoader(tccl);
			}

			public void persist(Object entity) {
				begin();
				try {
					em.persist(entity);
				} finally {
					end();
				}
			}

			public <T> T merge(T entity) {
				begin();
				try {
					return em.merge(entity);
				} finally {
					end();
				}
			}

			public void remove(Object entity) {
				em.remove(entity);
			}

			public <T> T find(Class<T> entityClass, Object primaryKey) {
				begin();
				try {
					return em.find(entityClass, primaryKey);
				} finally {
					end();
				}
			}

			public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
				begin();
				try {
					return em.find(entityClass, primaryKey, properties);
				} finally {
					end();
				}
			}

			public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
				begin();
				try {
					return em.find(entityClass, primaryKey, lockMode);
				} finally {
					end();
				}
			}

			public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode,
					Map<String, Object> properties) {
				begin();
				try {
					return em.find(entityClass, primaryKey, lockMode, properties);
				} finally {
					end();
				}
			}

			public <T> T getReference(Class<T> entityClass, Object primaryKey) {
				begin();
				try {
					return em.getReference(entityClass, primaryKey);
				} finally {
					end();
				}
			}

			public void flush() {
				em.flush();
			}

			public void setFlushMode(FlushModeType flushMode) {
				em.setFlushMode(flushMode);
			}

			public FlushModeType getFlushMode() {
				return em.getFlushMode();
			}

			public void lock(Object entity, LockModeType lockMode) {
				em.lock(entity, lockMode);
			}

			public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
				em.lock(entity, lockMode, properties);
			}

			public void refresh(Object entity) {
				begin();
				try {
					em.refresh(entity);
				} finally {
					end();
				}
			}

			public void refresh(Object entity, Map<String, Object> properties) {
				begin();
				try {
					em.refresh(entity, properties);
				} finally {
					end();
				}
			}

			public void refresh(Object entity, LockModeType lockMode) {
				begin();
				try {
					em.refresh(entity, lockMode);
				} finally {
					end();
				}
			}

			public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
				begin();
				try {
					em.refresh(entity, lockMode, properties);
				} finally {
					end();
				}
			}

			public void clear() {
				em.clear();
			}

			public void detach(Object entity) {
				em.detach(entity);
			}

			public boolean contains(Object entity) {
				return em.contains(entity);
			}

			public LockModeType getLockMode(Object entity) {
				return em.getLockMode(entity);
			}

			public void setProperty(String propertyName, Object value) {
				em.setProperty(propertyName, value);
			}

			public Map<String, Object> getProperties() {
				return em.getProperties();
			}

			public Query createQuery(String qlString) {
				return em.createQuery(qlString);
			}

			public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
				return em.createQuery(criteriaQuery);
			}

			public Query createQuery(CriteriaUpdate updateQuery) {
				return em.createQuery(updateQuery);
			}

			public Query createQuery(CriteriaDelete deleteQuery) {
				return em.createQuery(deleteQuery);
			}

			public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
				return em.createQuery(qlString, resultClass);
			}

			public Query createNamedQuery(String name) {
				return em.createNamedQuery(name);
			}

			public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
				return em.createNamedQuery(name, resultClass);
			}

			public Query createNativeQuery(String sqlString) {
				return em.createNativeQuery(sqlString);
			}

			public Query createNativeQuery(String sqlString, Class resultClass) {
				return em.createNativeQuery(sqlString, resultClass);
			}

			public Query createNativeQuery(String sqlString, String resultSetMapping) {
				return em.createNativeQuery(sqlString, resultSetMapping);
			}

			public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
				return em.createNamedStoredProcedureQuery(name);
			}

			public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
				return em.createStoredProcedureQuery(procedureName);
			}

			public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
				return em.createStoredProcedureQuery(procedureName, resultClasses);
			}

			public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
				return em.createStoredProcedureQuery(procedureName, resultSetMappings);
			}

			public void joinTransaction() {
				em.joinTransaction();
			}

			public boolean isJoinedToTransaction() {
				return em.isJoinedToTransaction();
			}

			public <T> T unwrap(Class<T> cls) {
				return em.unwrap(cls);
			}

			public Object getDelegate() {
				return em.getDelegate();
			}

			public void close() {
				em.close();
			}

			public boolean isOpen() {
				return em.isOpen();
			}

			public EntityTransaction getTransaction() {
				return em.getTransaction();
			}

			public EntityManagerFactory getEntityManagerFactory() {
				return em.getEntityManagerFactory();
			}

			public CriteriaBuilder getCriteriaBuilder() {
				return em.getCriteriaBuilder();
			}

			public Metamodel getMetamodel() {
				begin();
				try {
					return em.getMetamodel();
				} finally {
					end();
				}
			}

			public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
				return em.createEntityGraph(rootType);
			}

			public EntityGraph<?> createEntityGraph(String graphName) {
				return em.createEntityGraph(graphName);
			}

			public EntityGraph<?> getEntityGraph(String graphName) {
				return em.getEntityGraph(graphName);
			}

			public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
				return em.getEntityGraphs(entityClass);
			}

		};
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return emf.getCriteriaBuilder();
	}

	public Metamodel getMetamodel() {
		return emf.getMetamodel();
	}

	public boolean isOpen() {
		return emf.isOpen();
	}

	public void close() {
		emf.close();
	}

	public Map<String, Object> getProperties() {
		return emf.getProperties();
	}

	public Cache getCache() {
		return emf.getCache();
	}

	public PersistenceUnitUtil getPersistenceUnitUtil() {
		return emf.getPersistenceUnitUtil();
	}

	public void addNamedQuery(String name, Query query) {
		emf.addNamedQuery(name, query);
	}

	public <T> T unwrap(Class<T> cls) {
		return emf.unwrap(cls);
	}

	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		emf.addNamedEntityGraph(graphName, entityGraph);
	}

	public DelegatedEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}
}
