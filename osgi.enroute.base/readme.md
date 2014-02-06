

## Timer bundle

This bundle provides *java.util.Timer* as an OSGi service, with an automatically managed lifecycle.

The benefits are:

* The timer thread gets automatically cleaned up when it is not needed anymore.
* Only one timer instance per bundle. Other implementations could optimize this. 

### Details
One instance of this service is provided per requesting bundle using the declarative service's service factory. 
This service instance is automatically cleaned up (via the `deactivate` method of the service) when the requesting 
bundle is stopped or ungets the service. As a result of cleaning up the service instance, `cancel()` is called 
on the underlying *java.util.Timer*, which in turns discards any pending timer task for this timer.  
Once the service instance is cleaned up, it is dereferenced for garbage collection.

### Timer Configuration
The timer cannot be configured.
