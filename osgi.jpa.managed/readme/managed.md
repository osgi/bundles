# OSGi JPA Managed
## Problem
You want use Java Persistence API (JPA) in OSGi but your Persistence Provider does not
support OSGi, it partially implements OSGi, or it had, which is common, no clue about OSGi. 

## Description
OSGi has a comprehensive standard for JPA, however, this is the _unmanaged_ JPA. Unmanaged means that
it is assumed to be running outside an Application Server container. The original idea was that a 
JPA provider would provide the application with an Entity Manager Factory service. However, in practice
no provider implemented this, this has caused [Eclipse Gemini][1] and [Apache Aries][2] to provide
the missing pieces. 

Currently the result, as reported by many people trying to use JPA, is messy to say the least. This is
to a very large extent caused by the unportable nature of the JPA spec (test cases seem less than minimal)
but the OSGi situation is messy to say the least. This bundle, which owes a lot to the Apache Aries
JPA Container, is a an attempt to make this situation less messy and maybe even workable.

The intent is to create a common model and put pressure on JPA providers to support it. Until that
time, adapters are provided for the common implementations. 

* Data Nucleus
* Eclipse Persistence
* Hibernate 
* OpenJPA (once it is moved to JPA 2.1)
* ...

## Usage
Usage is without any OSGi overhead. An application should depend on an Entity Manager service. This
service is registered by this JPA Manager, based on the following information/services:

* A JPA Managed configuration record. This record provides the names of the persistence units that 
  should be managed and, optionally, a filter for the Data Source service and a filter for the Persistence 
  Provider service.
* A Data Source service
* A Persistence Provider

The persistence XML should not try to configure the database or control the JPA provider, this is all
managed through Configuration Admin.

The JPA Manager creates a *managed environment* for OSGi. It is an extender that looks for the
Meta-Persistence header and uses its content to find _persistent units_. These are parsed
and mapped to configuration records, which then associates a persistence unit with a A Data Source
service. Persistence Providers are looked for in the service registry.

After it matches a persistence unit with a Data Source service it creates an Entity Manager 
service. Though a self created Entity Manager is not thread safe, this service actually is
since it is a proxy that will create the actual EM on demand. Each such EMs are only associated
with a single transaction. This means that applications in general never begin a transaction. It is
assumed that the entry points of the system (background task managers, web servers) start a
Transaction and commit it when the request or task has been executed.

The Entity Manager service is lazy, it waits until it is called. When is called it verifies
if it is the first time for a given transaction. If so, it creates a new Entity Manager from
the Entity Manager Factory and registers itself with the transaction to get a callback
after completion so it can close the Entity Manager, and it remembers the Entity Manager
it created for future calls on the same transacton (which is on a single thread). Otherwise,
when it was called before for the same transaction, it delegates to the remembered Entity Manager.
When the transaction is finished, the Entity Manager is deleted.

### Example
An example `persistence.xml`

    persisten.xml: 
      <persistence xmlns="http://java.sun.com/xml/ns/persistence"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance version="2.0">
        <persistence-unit name="tutorial" transaction-type="JTA">
          <class>org.example.Domain</class>
        </persistence-unit>
      </persistence>
    
    MyPersistenceDomain.java
      @Component
      public class MyPersistenceDomain implements MyService {
         EntityManager em;
       
         @Reference
         void setEM( EntityManager em) {
           this.em = em;
         }
       
         public void doService(Domain domain) {
           em.persist(domain);
           em.flush();
         }
      }
    
    Configuration:
	    {
	        "service.factoryPid" : "osgi.jpa.managed.aux.JPAManager",
	        "service.pid" : "All",
	        "name" : "*"
	    }

Since this setup requires a Data Source service, look [here][3].
 
## Configuration

* service.factoryPid — `osgi.jpa.managed.aux.JPAManager`
* name — The name of the persistence unit, globbing/wildcarding allowed. Default is '*'
* dataSource.target — Filter for the Data Source service (default any)
* persistenceProvider.target — Filter for the Persistence Provider (default any)

### Example:

	    {
	        "service.factoryPid" : "osgi.jpa.managed.aux.JPAManager",
	        "service.pid" : "All",
	        "name" : "*"
	    }
	    
[1]: http://www.eclipse.org/gemini/web/
[2]: http://aries.apache.org/
[3]: http://jpm4j.org/#!/p/osgi/osgi.jdbc.managed.aux?tab=readme
