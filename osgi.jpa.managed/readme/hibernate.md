# Hibernate Adapter for an OSGi Managed JPA Environment
This bundle adapts an existing hibernate bundle (from version 4.3) to the OSGi managed
JPA model. Since the JPA spec is rather weak and has a lot of optionality it is
necessary to adapt the existing hibernate to play nice. This could have been done
in the manager, but that would make the manager depend on hibernate.

This bundle should disappear over time when the functionality is taken over by
hibernate itself.

## Responsibilities
The primary responsibility of this bundle is register a Persistence Provider so that
the JPA manager can pick it up. It will register a proxy so that crucial calls can be
overridden to provide hibernat specific adaptations.

## Configuration
There is no configuration, the only thing needed is an hibernate.