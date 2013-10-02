# JDBC in OSGi 
## Version ${Bundle-Version}
## Problem
You need a configured DataSource that is bound to a given database that is
(potentially) shared between multiple components. For example, you need
to access the `test` database to run a test.

## Description
Since OSGi is service based it is recommended to also use JDBC in service mode. In
service mode, a database driver registers a Data Source Factory service, see for
example the H2 database. However, in a component oriented system you do not want
to consume this service in your application or library code since it still needs
configuration data and that should be in the realm of the deployer. Components
should use a configured Data Source instead.

This project provides a bundle that does exactly that. If you create a factory configuration
for the `enRoute.component.jdbc.XADataSourceFactory` than the properties are used
to find a matching Data Source Factory service and then with the `url`, and optionally
the `user` and `.password` properties, an XADataSource is registered. 

The persistence world is crazy about flexibility, making many simple things a hassle
in figuring out what combinations actually work (and in which order). To keep things
simple, this service only registers an XA Data Source service. The reason is that an XA
Data Source works fine as a Data Source ( an XAConnection is just paring the connection
with an XA resource) and it is assumed that the overhead of using an XA connection
without transactions has virtually no overhead.

The service has a Metatype so that the configuration properties can be edited in e.g. the
Webconsole. 

## How to Use
So this bundle effectively turns a Data Source Factory service to any configured number 
of XA Data Source services. Applications should depend on the XA Data Source service 
as follows:

    @Component
    public class MyApp {
        XADataSource xds;

       @Reference
	   void setXDS( XADataSource xds) {
	     this.xds = xds;
	   }
    }

## Configuration
The PID is:

    enRoute.component.jdbc.XADataSourceFactory

### Properties
The following properties are supported.

* `url` — The database URL, the schema for this url is defined by the actual database.
* `user` — The user id. In general, this can also be specified in the url.
* `.password` — The (secret) password. In general, this can also be specified in the url.
* `database` — (optional) You can specify a database name and refer to this name when selecting the resulting XA Data Source service.
* `dataSourceFactory.target` — (optional) An optional OSGi filter when there are multiple Data Source Factory services.

### Example Configuration Record for Configurer

    {
        "service.factoryPid"          : "oow.impl.jdbc.XADataSourceImpl",
        "service.pid"                 : "MySQLDataSource",
        "url"                         : "jdbc:mysql:///test?user=root&password=",
        "dataSourceFactory.target"    : "(osgi.jdbc.driver.class=com.mysql.jdbc.Driver)"
    } 

## Service Properties
No additional service properties other than the configuration properties are registered.

## Tests
The JDBC XA Data Source Provider has been tested with the following drivers:

* H2 — Out of the  box [link](http://jpm4j.org/#!/p/com.h2database/h2)
* MySQL — Requires currently an adapter since the latest versions are OSGi compatible but are not supporting the [Data Source Factory][1] service. You can 
the adapter [here](http://jpm4j.org/#!/p/osgi/enRoute.jdbc.mysql).

[1]: http://www.osgi.org/javadoc/r4v42/org/osgi/service/jdbc/DataSourceFactory.html

