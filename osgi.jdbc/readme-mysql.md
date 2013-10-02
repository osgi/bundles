# MySQL Data Source Factory Driver
## Problem
You want to use a MySQL database in OSGi but this database does not support the
[OSGi Data Source Factory service][1] specification.

## Description
This bundle provides an adapter for the [MySQL Java Driver][2]. This driver has the 
necessary headers to run as an OSGi bundle but it does not support the OSGi
JDBC specification. This bundle will register an OSGi Data Source Factory service
that is based on the MySQL bundle.

This Data Source Factory is mapped to the `com.mysql.jdbc.jdbc2.optional.MysqlXADataSource`. 
The driver is `com.mysql.jdbc.Driver`, the service property `osgi.jdbc.driver.class` is set to this 
name. You can ensure a reference to the MySQL driver by setting the reference filter to dataSourceFactory.target.
 
## How to Use
Just install this bundle together with the [MySQL][2] bundle. This will make it available for
use with the [Data Source Producer bundle][3]. 

## Configuration
This bundle does not require configuration.

## Service Properties
This bundle registers the Data Source Factory service with the following properties:

* `osgi.jdbc.driver.class=com.mysql.jdbc.Driver`
* `osgi.jdbc.driver.name=MySql`
* `osgi.jdbc.driver.version=${Bundle-Version}`

    
[1]: http://www.osgi.org/javadoc/r4v42/org/osgi/service/jdbc/DataSourceFactory.html
[2]: http://repo.jpm4j.org/#!/p/osgi/com.mysql.jdbc
[3]: http://repo.jpm4j.org/#!/p/osgi/enRoute.jdbc.dsp