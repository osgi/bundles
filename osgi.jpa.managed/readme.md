# JPA Managed
## Problem
You want use Java Persistence API (JPA) in OSGi but your Persistence Provider does not
support OSGi, it partially implements OSGi, or it had no clue about OSGi. 

## Description


## Operation
The JPA bridge creates a *managed environment* for OSGi. It assumes that the Persistence Provider
has registered a Persistence Provider service. This is a passive service provider, it will not
look at bundles. 

The JPA bridge is created through Configuration Admin, it uses a factory so you can create
multiple bridges. A bridge then tracks bundles with persistence units that match the 
configuration. Multiple persistence unit and multiple providers are not supported yet.

For each persistence unit that matches, the bridge requests an Entity Manager Factory
from the Persistence Provider and registers an Entity Manager service for use by
the application/library code.

The Entity Manager service is lazy, it waits until it is called. When is called it verifies
if it is the first time for a given transaction. If so, it creates a new Entity Manager from
the Entity Manager Factory and registers itself with the transaction to get a callback
after completion so it can close the Entity Manager, and it remembers the Entity Manager
it created for future calls on the same transacton (which is on a single thread). Otherwise,
when it was called before for the same transaction, it delegates to the remembered Entity Manager.
When the transaction is finished, the Entity Manager is deleted.

The Persistence Provider leaves the  
