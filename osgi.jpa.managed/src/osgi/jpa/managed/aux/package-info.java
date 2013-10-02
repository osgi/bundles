/**
 * The JPA Bridge combines 3 services:
 * <ul>
 * <li>XA DataSource Ñ A Data source configured for the current database.
 * <li>Transaction Manager Ñ Provides the transactional context
 * <li>Persistence Provider Ñ The JPA provider.
 * </ul>
 * 
 * This bridge then provides the Persistence Provider with a {@code PersistenceUnit} object. This
 * object is the sole interface for the JPA provider, it provides it with the (XA) Datasource, the 
 * class loaders, options, properties, etc. In return, the provider returns a Entity Manager Factory.
 * The bridge then  registers a managed Entity Manager proxy service for consumption by applications.
 * <p>
 * If the application starts to use the Entity Manager, then the proxy creates a new Entity Manager
 * through the Entity Manager Factory and makes it join the current transaction. The connections are
 * registered with the Transaction Manager so that they are automatically closed when the
 * Transaction is finished. 
 */
package osgi.jpa.managed.aux;