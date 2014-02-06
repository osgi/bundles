package osgi.enroute.configurer.extender;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.util.tracker.BundleTracker;

import osgi.enroute.configurer.api.ConfigurationDone;
import osgi.enroute.logging.messages.api.Format;
import osgi.enroute.logging.messages.api.LogBook;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.lib.collections.ExtList;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.lib.properties.PropertiesParser;
import aQute.lib.settings.Settings;
import aQute.libg.sed.Domain;
import aQute.libg.sed.ReplacerAdapter;

/**
 * This bundle is an extender that reads the bundle's configuration directory.
 * Any resources in this directory that and in .json files are treated as
 * configurations.
 */

@Component(provide = {
		ConfigurationDone.class, Object.class
}, immediate = true)
public class Configurer implements ConfigurationDone, Domain {

	private static final String	SERVICE_FACTORY_PID			= "service.factoryPid";
	private static final String	OSGI_ENROUTE_CONFIGURER_PID	= "osgi.enroute.configurer.pid";
	static final JSONCodec		codec						= new JSONCodec();
	static final Converter		converter					= new Converter();
	static Pattern				PROFILE_PATTERN				= Pattern.compile("\\[([a-zA-Z0-9]+)\\](.*)");
	static Pattern				RESOURCE_PATTERN			= Pattern.compile("(@\\{resource:([^}]+)\\})");
	static Pattern				MACRO						= Pattern.compile("\\$\\{[^}]+\\}");
	final List<String>			properties					= new ArrayList<String>();
	final Settings				settings					= new Settings("~/.enroute/settings.json");
	BundleTracker< ? >			tracker;
	ConfigurationAdmin			cm;
	String						profile;
	File						dir;
	Map<String,String>			basicProperties;

	interface EnRouteConfigurer extends LogBook {

		@Format("Failed to set the configuration %3$s for %1$s: %2$s")
		ERROR failedToSetConfigurationFor(Bundle bundle, Exception e, URL url);

		@Format("Found a configuration without a pid: %s")
		ERROR configurationWithoutPid(Map<String,Object> map);

		@Format("Unexpected error while parsing bundle %s: %s")
		ERROR parsingBundleFailed(Bundle bundle, Exception e);

		@Format("Update configuration from bundle %s for factory=%s, pid=%s")
		INFO updatedConfiguration(Bundle bundle, String factory, String pid);

		@Format("Log message from bundle %s, pid %s: %s")
		INFO logMessage(Bundle bundle, String pid, String message);

		WARN unresolvedMacro(String key, Object value);

		DEBUG delta(Object object, String key, Object value, Object value2);

		@Format("Property files were specified from the command line but no file could be found: %s")
		ERROR missingProperties(File f);

	}

	EnRouteConfigurer	log;
	Coordinator			coordinator;

	/*
	 * Activate this extender and start looking for bundles
	 */

	@Activate
	void activate(BundleContext context) throws Exception {
		dir = context.getDataFile("resources");
		dir.mkdirs();

		//
		// Collect properties about our context
		// so the configuration can use it
		//
		basicProperties = new HashMap<String,String>();
		basicProperties.putAll(toMap(System.getProperties()));
		basicProperties.putAll(settings);

		for (String path : properties) {
			File f = IO.getFile(path);
			if (!f.isFile()) {
				log.missingProperties(f);
			} else {
				Properties props = PropertiesParser.parse(f.toURI());
				basicProperties.putAll(toMap(props));
			}
		}

		if (profile == null)
			profile = basicProperties.containsKey("profile") ? basicProperties.get("profile") : "debug";

		tracker = new BundleTracker<Object>(context, Bundle.ACTIVE | Bundle.STARTING, null) {

			@Override
			public Object addingBundle(Bundle bundle, BundleEvent event) {
				Coordination c = coordinator.begin("enroute::configurer", 0);
				try {
					Domain domain = Configurer.this;

					URL vars = bundle.getEntry("vars.properties");
					if (vars != null) {
						Properties props = PropertiesParser.parse(vars.toURI());
						domain = new MapDomain(domain, toMap(props));
					}

					Map<String,String> bprops = new HashMap<String,String>();
					bprops.put("osgi.bundle.id", bundle.getBundleId() + "");
					bprops.put("osgi.bundle.location", bundle.getLocation());
					bprops.put("osgi.bundle.bsn", bundle.getSymbolicName());
					bprops.put("osgi.bundle.version", bundle.getVersion() + "");
					bprops.put("osgi.bundle.lastmodified", bundle.getLastModified() + "");
					bprops.put("osgi.current.time", System.currentTimeMillis() + "");
					domain = new MapDomain(domain, bprops);

					Enumeration<URL> entries = bundle.findEntries("configuration", "*.json", false);
					while (entries.hasMoreElements()) {
						URL url = entries.nextElement();
						try {
							String s = IO.collect(url);
							configure(bundle, s, domain);
						}
						catch (Exception e) {
							log.failedToSetConfigurationFor(bundle, e, url);
						}
					}
				}
				catch (Exception e) {
					c.fail(e);
					log.parsingBundleFailed(bundle, e);
				}
				finally {
					c.end();
				}
				return null;
			}
		};
		tracker.open();

		//
		// We also support local configurations that are set as System
		// properties
		//

		String s = System.getProperty("enroute.configuration");
		if (s != null) {
			Coordination c = coordinator.begin("enroute::configurer-system", 0);
			try {
				configure(context.getBundle(), s, this);
			}
			catch (Throwable e) {
				c.fail(e);
			}
			finally {
				c.end();
			}
		}
	}

	@SuppressWarnings({
			"rawtypes", "unchecked"
	})
	private Map<String,String> toMap(Map props) {
		return props;
	}

	/*
	 * Just close the tracker on deactivate
	 */
	void deactivate() {
		tracker.close();
	}

	/*
	 * This function takes the configuration data strings, replaces all macros,
	 * gets the records, and for each record processes it deeper.
	 */

	void configure(Bundle ctx, String s, Domain domain) throws Exception {

		ReplacerAdapter ra = new ReplacerAdapter(domain) {
			@SuppressWarnings("unused")
			public String _resource(String args[]) {
				return null;
				// TODO
			}
		};

		//
		// In bnd we use ${} macros as well. Sometimes it is nice
		// to have both macro processors. So we replace all @{ to
		// to ${ since the @{ will not be replaced by bnd.
		//

		s = s.replaceAll("(?!\\\\)@\\{", "${");

		//
		// Preprocess the source configuration. Note that we need
		// to do this on source level since the JSON will consist
		// of real objects
		//

		String processed = ra.process(s);

		//
		// Convert the input to a list of maps.
		//

		List<Hashtable<String,Object>> list = codec.dec().from(processed)
				.get(new TypeReference<List<Hashtable<String,Object>>>() {});

		for (Hashtable<String,Object> map : list)
			configure(ctx, map);

	}

	/*
	 * We have a configuration record for this bundle. We clean up the record.
	 */
	void configure(Bundle bundle, Map<String,Object> map) throws Exception {

		//
		// Check if this is a valid configuration
		//

		String factory = (String) map.get(SERVICE_FACTORY_PID);
		String pid = (String) map.get(Constants.SERVICE_PID);

		if (pid == null) {
			log.configurationWithoutPid(map);
			return;
		}

		//
		// Need to clean this up, and we have a couple of comments
		// and log messages
		//

		Hashtable<String,Object> dictionary = fixup(bundle, pid, map);

		//
		// We need to handle symbolic pids when factories are used since
		// the PIDs are assigned by the CM. We need to remember which
		// symbolic PID we used for an instance so that we can update
		// that specific factory instance when we get an update. To
		// address this, we store an extra property in the dict.
		//

		dictionary.put(OSGI_ENROUTE_CONFIGURER_PID, pid);
		Configuration configuration;

		if (factory != null) {

			//
			// If we have a factory then we need to find out if there
			// is already a record available. We need to search for
			// our symbolic name
			//

			Configuration instances[] = cm.listConfigurations("(" + OSGI_ENROUTE_CONFIGURER_PID + "=" + pid + ")");
			if (instances == null) {

				//
				// New factory configuration. Make sure it has multiple
				// locations
				//

				configuration = cm.createFactoryConfiguration(factory, "?");
			} else {

				//
				// Existing factory configuration
				//

				configuration = instances[0];
			}
		} else {

			//
			// normal target configuration, will be created
			//

			configuration = cm.getConfiguration(pid, "?");
		}

		// System.out.println("Updating " + dictionary);

		configuration.setBundleLocation(null);

		Dictionary< ? , ? > current = configuration.getProperties();
		if (current != null && isEqual(dictionary, current))
			return;

		configuration.update(dictionary);
		log.updatedConfiguration(bundle, factory, pid);
	}

	Hashtable<String,Object> fixup(Bundle bundle, String pid, Map<String,Object> map) throws Exception {
		Hashtable<String,Object> dictionary = new Hashtable<String,Object>();

		for (Entry<String,Object> e : map.entrySet()) {

			Matcher m = PROFILE_PATTERN.matcher(e.getKey());
			if (m.matches()) {

				//
				// Profile prefixed variables are ignored when they
				// are not current (not the profile set right now)
				// and otherwise the profile prefix is removed
				//

				String profile = m.group(1);
				if (profile.equals(this.profile))
					dictionary.put(m.group(2), e.getValue());

			} else if (e.getKey().equals(".log")) {
				//
				// We allow a record to define a log message
				//

				log.logMessage(bundle, pid, converter.convert(String.class, e.getValue()));
			} else if (e.getKey().equals(".comment")) {

				//
				// Provides space for comments
				// so we better ignore them
				//

			} else {
				Object value = e.getValue();
				if (value != null && value instanceof String) {
					Matcher matcher = MACRO.matcher((String) value);
					while (matcher.find()) {
						log.unresolvedMacro(e.getKey(), value);
					}
				}
				dictionary.put(e.getKey(), e.getValue());
			}
		}

		return dictionary;
	}

	@SuppressWarnings("unchecked")
	private boolean isEqual(Hashtable<String,Object> a, Dictionary< ? , ? > b) {
		for (Entry<String,Object> e : a.entrySet()) {
			if (e.getKey().equals("service.pid"))
				continue;

			Object value = b.get(e.getKey());
			if (value == e.getValue())
				continue;

			if (value == null)
				return false;

			if (e.getValue() == null)
				return false;

			if (value.equals(e.getValue()))
				continue;

			if (value.getClass().isArray()) {
				Object[] aa = {
					value
				};
				Object[] bb = {
					e.getValue()
				};
				if (!Arrays.deepEquals(aa, bb))
					return false;
			} else if (value instanceof Collection && e.getValue() instanceof Collection) {
				ExtList<Object> aa = new ExtList<Object>((Collection<Object>) value);
				ExtList<Object> bb = new ExtList<Object>((Collection<Object>) e.getValue());
				if (!aa.equals(bb))
					return false;
			} else {
				log.delta(a.get("service.pid"), e.getKey(), value, e.getValue());
				return false;
			}
		}
		return true;
	}

	@Override
	public Map<String,String> getMap() {
		return basicProperties;
	}

	@Override
	public Domain getParent() {
		return null;
	}

	@Reference
	void setLogService(LogBook log) {
		this.log = log.scoped(EnRouteConfigurer.class, "enroute::configurer");
	}

	@Reference
	void setCM(ConfigurationAdmin cm) {
		this.cm = cm;
	}

	@Reference
	void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}

	@Reference(type = '?', target = "(launcher.arguments=*)")
	synchronized void setLauncher(Object obj, Map<String,Object> props) {
		String[] args = (String[]) props.get("launcher.arguments");
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals("--enroute-profile")) {
				if (this.profile != null)
					System.err.println("Profile set multiple times, used first one " + this.profile);
				else
					this.profile = args[i++];
			} else if (args[i].equals("--enroute-properties")) {
				this.properties.add(args[++i]);
			}
		}
	}

	synchronized void unsetLauncher(Object obj) {

	}

}
