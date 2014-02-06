package osgi.enroute.logging.provider;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import osgi.enroute.logging.messages.api.Format;
import osgi.enroute.logging.messages.api.LogBook;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

/**
 * 
 */
@Component
public class LogAdminImpl {
	final static int				LOG_TRACE		= LogService.LOG_DEBUG + 1;
	final static Pattern			CUSTOM_CLASSES	= Pattern
															.compile("(?!com\\.sun|sun|java\\.|osgi\\.enroute\\.logging\\.provider)(.+\\.)+(.*)");

	ComponentContext				ctx;
	ServiceRegistration< ? >		registration;
	ServiceReference<LogService>	log;
	boolean							printStackTraces;
	PrintStream						out				= System.err;
	LogService						logService;

	@Activate
	void activate(ComponentContext ctx) throws Exception {
		this.ctx = ctx;
		logService = ctx.getBundleContext().getService(log);
		Hashtable<String,Object> properties = new Hashtable<>();

		//
		// We want to share a single object (this LogAdminImpl) but still
		// want to use a service factory so each bundle gets its own. So we
		// need to register it manually :-(
		//

		registration = ctx.getBundleContext().registerService(LogBook.class.toString(), new ServiceFactory<LogBook>() {

			@Override
			public LogBook getService(Bundle bundle, ServiceRegistration<LogBook> registration) {
				return new LogBookImpl(LogAdminImpl.this, bundle, bundle.getSymbolicName());
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<LogBook> registration, LogBook service) {
				((LogBookImpl) service).close();
			}
		}, properties);
	}

	@Deactivate
	void deactivate() {
		registration.unregister();
	}

	void message(MessageFormatter msf, int level, String format, Object[] arguments) {
		try {
			ServiceReference< ? > ref = null;
			Throwable throwable = null;
			int n = 0;

			//
			// Adjust the arguments since arrays print badly and we can do
			// better
			// for
			// some other objects as well.
			//

			for (int i = 0; i < arguments.length; i++)
				if (arguments[i] != null) {

					if (ref == null && arguments[i] instanceof ServiceReference< ? >) {
						ref = (ServiceReference< ? >) arguments[i];
						n++;
					}

					else if (throwable == null && arguments[i] instanceof Throwable) {
						throwable = (Throwable) arguments[i];
						n += 2;
					} else if (!(arguments[i] instanceof String))
						arguments[i] = toString(arguments[i]);
				}

			//
			// Add a few more places so that errors in the format would refer to
			// non-existent args. Logging should not throw exceptions.
			//

			Object nargs[] = new Object[arguments.length + 10];
			System.arraycopy(arguments, 0, nargs, 0, arguments.length);

			final StringBuilder sb = new StringBuilder();
			if (msf.scope != null) {
				sb.append(msf.scope).append(":: ");
			}

			where(sb);

			try (Formatter formatter = new Formatter(sb)) {
				formatter.format(format, nargs);
			}

			LogService log = msf.log;
			if (log == null)
				log = msf.bundle.getBundleContext().getService(this.log);

			if (log != null) {

				switch (n) {

					case 1 :
						log.log(ref, level, sb.toString());
						break;
					case 2 :
						log.log(level, sb.toString(), throwable);
						break;
					case 3 :
						log.log(ref, level, sb.toString(), throwable);
						break;

					case 0 :
					default :
						log.log(level, sb.toString());
						break;
				}
			}
			if (throwable != null && printStackTraces) {
				sb.append("\n");
				try (PrintWriter sw = getWriter(sb)) {
					throwable.printStackTrace(sw);
				}
				out.println(sb.toString());
			}
		}
		catch (Exception e) {
			logService.log(LogService.LOG_ERROR, "Shamefully have to admit the log service failed " + e);
		}
	}

	private PrintWriter getWriter(final StringBuilder sb) {
		return new PrintWriter(new Writer() {

			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				for (int i = 0; i < len; i++)
					sb.append(cbuf[i + off]);
			}

			@Override
			public void flush() throws IOException {}

			@Override
			public void close() throws IOException {}

		});
	}

	/**
	 * Create a more suitable text presentation for array objects
	 * 
	 * @param object
	 * @return
	 */
	private Object toString(Object object) {
		if (object.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			String del = "[";
			for (int i = 0; i < Array.getLength(object); i++) {
				sb.append(del).append(toString(Array.get(object, i)));
				del = ", ";
			}
			sb.append("]");
			return sb;
		}
		return object;
	}

	/**
	 * Get the current location of where the error was reported.
	 */
	private void where(StringBuilder sb) {
		try {
			throw new Exception();
		}
		catch (Exception e) {
			StackTraceElement[] stackTrace = e.getStackTrace();
			for (int i = 2; i < stackTrace.length; i++) {
				Matcher matcher = CUSTOM_CLASSES.matcher(stackTrace[i].getClassName());

				if (matcher.matches()) {
					String logMethod = stackTrace[i].getMethodName();
					String logClass = matcher.group(2);
					int line = stackTrace[i].getLineNumber();
					sb.append("[").append(logClass).append(".").append(logMethod);
					if (line != 0)
						sb.append(":").append(line);
					sb.append("] ");
				}
			}
		}
	}

	Object message(MessageFormatter msf, int level, Method method, Object[] args) {
		Format f = method.getAnnotation(Format.class);
		String format;
		if (f == null)
			format = makeup(method.getName(), args.length);
		else
			format = f.value();

		message(msf, level, format, args);
		return null;
	}

	/**
	 * Use a method name and turn it into a reasonable format
	 * 
	 * @param name
	 * @param length
	 * @return
	 */
	private String makeup(String id, int length) {
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toUpperCase(id.charAt(0)));

		int i = 1;
		char c;
		boolean upper = true;

		while (i < id.length()) {
			c = id.charAt(i);
			if (Character.isUpperCase(c)) {
				if (upper) {
					sb.append(c);
				} else {
					sb.append(" ").append(c);
				}
				upper = true;
			} else {
				upper = false;
				if (c == '_') {
					if (length > 0) {
						sb.append(" %s ");
						length--;
					}
				} else {
					sb.append(c);
				}
			}
		}
		while (length > 0) {
			sb.append(" %s ");
			length--;
		}
		return sb.toString();
	}

	<T> T scoped(LogBookImpl book, Class<T> type, String prefix) {
		LogBookHandler handler = new LogBookHandler(this, prefix, book);
		synchronized (book) {
			if (book.handlers == null)
				book.handlers = new ArrayList<>();
			book.handlers.add(handler);
		}
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class< ? >[] {
			type
		}, handler));
	}

	@Reference(service = LogService.class)
	void setLogService(ServiceReference< LogService > ref) {
		this.log = ref;
	}
}
