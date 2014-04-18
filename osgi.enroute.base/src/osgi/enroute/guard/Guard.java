package osgi.enroute.guard;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.log.LogService;

/**
 * This bundle is a very small bundle that should be installed in any enRoute
 * system supporting a capability profile. It will check continuously if all the
 * elements of base are present and functioning.
 */
@Component
public class Guard extends Thread implements BundleActivator {
	static AtomicBoolean		components	= new AtomicBoolean(false);
	private BundleContext		context;

	Map<String,String>			packages;
	Map<String,String>			services;
	final Map<String,String>	alerts		= new HashMap<>();
	volatile boolean			quit;

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;

		packages = load(context.getBundle(), "guardinfo/packages.properties", packages);
		services = load(context.getBundle(), "guardinfo/services.properties", services);

		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("command.scope", "guard");
		properties.put("command.function", new String[] {
				"restart", "clear"
		});
		context.registerService(Object.class, new Object(), properties);

		final AtomicBoolean started = new AtomicBoolean(false);

		//
		// Wait before polling
		context.addFrameworkListener(new FrameworkListener() {

			@Override
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.STARTED && started.getAndSet(true)) {
					start();
				}
			}
		});
		Thread.sleep(5000);
		if (started.getAndSet(true))
			start();
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	private Map<String,String> load(Bundle source, String path, Map<String,String> dest) throws UnsupportedEncodingException, IOException {
		Properties p = new Properties();
		URL url = source.getResource(path);
		if ( url == null)
			throw new IllegalStateException("Resources for guard not in bundle " + path);
		
		Reader reader = new InputStreamReader(url.openStream(), "UTF-8");
		p.load(reader);
		
		return (Map<String,String>)(Map)p;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		quit = true;
		interrupt();
	}

	@Activate
	void activate() {
		components.set(true);
	}

	public void run() {

		while (!quit)
			try {
				alerts.clear();
				while (!quit) {
					checkServices();
					checkPackages();
					Thread.sleep(5000);
				}
			}
			catch (InterruptedException e) {
				return;
			}
	}

	void checkServices() {
		for (String service : services.keySet()) {
			checkService(service);
		}
	}

	void checkService(String serviceName) {
		try {
			Class< ? > c = context.getBundle().loadClass(serviceName);
			ServiceReference< ? >[] references = context.getServiceReferences(serviceName, null);

			if (references == null || references.length == 0) {
				//
				// Check if it is in another class space
				//
				ServiceReference< ? >[] srfs = context.getAllServiceReferences(c.getName(), null);
				if (srfs != null && srfs.length > 0) {
					alert(serviceName, "Service not found, but it does exist in another class space.");
				} else {
					alert(serviceName, "Service does not exist.");
				}
				return;
			}

			//
			// Service does exist, see if we can get it
			//

			List<String> failures = new ArrayList<>();

			for (ServiceReference< ? > ref : references) {
				Object s = context.getService(ref);
				if (s == null)
					failures.add("fetch from " + ref.getBundle());

				context.ungetService(ref);
			}
		}
		catch (ClassNotFoundException | InvalidSyntaxException e) {
			alert(serviceName, e.getMessage());
		}
	}

	private void alert(String name, String message) {
		String old = alerts.get(name);
		if (old != null && old.equals(message))
			return;

		alerts.put(name, message);

		try {
			ServiceReference<LogService> ref = context.getServiceReference(LogService.class);
			if (ref != null) {
				LogService log = context.getService(ref);
				log.log(LogService.LOG_WARNING, name + ": " + message);
				context.ungetService(ref);
				return;
			}
		}
		catch (Throwable e) {
			// ignore
		}
		System.err.println(name + ": " + message);
	}

	void checkPackages() {
		for (Entry<String,String> e : packages.entrySet()) {
			checkPackage(e.getKey(), e.getValue());
		}
	}

	private void checkPackage(String packageName, String versionRange) {
		try {
			Class< ? > pinfo = context.getBundle().loadClass(packageName + ".package-info");

			Bundle exporter = FrameworkUtil.getBundle(pinfo);
			if (exporter == null) {
				alert(packageName, "Not exported by a bundle");
				return;
			}

			if (versionRange == null) {
				alert(packageName, "Exporter has no version");
				return;
			}

			BundleWiring wiring = exporter.adapt(BundleWiring.class);
			BundleCapability export = getCapability(wiring, PackageNamespace.PACKAGE_NAMESPACE, packageName);
			Object v = export.getAttributes().get("version");
			if (v == null) {
				alert(packageName, "Exporter has no version");
				return;
			}

			if (!(v instanceof Version)) {
				alert(packageName, "Exporter has version but it is not a Version type " + v);
				return;
			}

			Version version = (Version) v;
			VersionRange range = new VersionRange(versionRange);
			if (!range.includes(version)) {
				alert(packageName, "Exporter " + exporter + " has version " + version + " outside the set range "
						+ range);
				return;
			}
		}
		catch (ClassNotFoundException e) {
			alert(packageName, "Not found");
		} catch( Exception e) {
			alert(packageName, e.getMessage());
		}

	}

	private BundleCapability getCapability(BundleWiring wiring, String ns, String name) {
		List<BundleCapability> capabilities = wiring.getCapabilities(ns);
		for (BundleCapability capability : capabilities) {
			Object object = capability.getAttributes().get(ns);
			if (name.equals(object))
				return capability;
		}
		return null;
	}

	public void clear() {
		
	}
	public void restart() {
		
	}
}
