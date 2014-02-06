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

/**
 * The JPA Bridge combines 3 services:
 * <ul>
 * <li>XA DataSource - A Data source configured for the current database.
 * <li>Transaction Manager - Provides the transactional context
 * <li>Persistence Provider - The JPA provider.
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

package osgi.jpa.managed.support;

