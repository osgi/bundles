${Bundle-SymbolicName} v${Bundle-Version}
# OSGi Managed JPA/JDBC Tests
This bundle is an OSGi test bundle that tests the managed JDBC and JPA subsystem. It
will try out different combinations of H2, MySQL, Hibernate, Eclipselink, etc. If
an external database is not present, we will not test it.

## JDBC
Managed JDBC uses the [DataSourceFactory][1] and [Configuration Admin][2] to create a 
[Data Source][3] service. We are testing different configurations.

## JPA
Managed JPA uses [Configuration Admin][2] and a [Persistence Provider][4] to create
an [Entity Manager][5] service. This Entity Manager service can only be used in
transactions. We are testing different configurations. Some tests only run when the
appropriate database is reachable.

[1]: http://www.osgi.org/javadoc/r4v42/org/osgi/service/jdbc/DataSourceFactory.html
[2]: http://www.osgi.org/javadoc/r4v42/org/osgi/service/cm/ConfigurationAdmin.html
[3]: http://docs.oracle.com/javase/6/docs/api/javax/sql/DataSource.html
[4]: http://docs.oracle.com/javaee/6/api/javax/persistence/spi/PersistenceProvider.html
[5]: http://docs.oracle.com/javaee/6/api/javax/persistence/EntityManager.html