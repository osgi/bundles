# OSGi Base Provider

This bundle contains a number of services that are very plain and only depend on the JRE.

* Timer — A `java.util.Timer` service. This service properly cleans up TimerTasks when their bundles are stopped.
* Executor — A `java.util.concurrent.Executor` service that is shared

## Timer Service

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

## Executor Service

Erlang's law states that shared resources are more efficient than dedicated resources since the different shares can use eachother's reserve capacity. Of course there are worst case scenarios that maybe less beneficial. However, since the number of cores in a CPU is limited, there is a limit anyway.

The Executor service is a way to execute small tasks without having to create threads or local ExecutorService objects. The whole idea of this service is to make the different component unaware of a lot of low level details about scheduling; bundles in a composed systems are rarely able to make sensible choices since they depend on what the others are doing.

Now, that said. This is a shared resource. This implementation may change over time to become more intelligent. I.e. it can do some smarter scheduling based on the calling bundle, detect executors used for every busy tasks, and throttle bundles that try to push too many runnables. However, it is expected that the calllers limit the number of submitted tasks.
