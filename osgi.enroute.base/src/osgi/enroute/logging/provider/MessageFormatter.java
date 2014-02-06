package osgi.enroute.logging.provider;

import java.util.concurrent.Callable;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

import osgi.enroute.logging.messages.api.LogBook;

public abstract class MessageFormatter implements LogBook {
	final String		scope;
	final Bundle		bundle;
	LogAdminImpl		admin;
	LogService			log;

	volatile boolean	info		= true;
	volatile boolean	trace		= true;
	volatile boolean	debug		= true;
	volatile boolean	warn		= true;
	volatile boolean	error		= true;
	volatile boolean	exceptions	= true;

	MessageFormatter(LogAdminImpl admin, Bundle bundle, String scope) {
		this.admin = admin;
		this.bundle = bundle;
		this.scope = scope;
	}

	@Override
	public INFO info(String format, Object... arguments) {
		if (info)
			admin.message(this, LogService.LOG_INFO, format, arguments);
		return null;
	}

	@Override
	public DEBUG debug(String format, Object... arguments) {
		if (debug)
			admin.message(this, LogService.LOG_DEBUG, format, arguments);
		return null;
	}

	@Override
	public ERROR error(String format, Object... arguments) {
		if (error)
			admin.message(this, LogService.LOG_ERROR, format, arguments);
		return null;
	}

	@Override
	public TRACE trace(String format, Object... arguments) {
		if (trace)
			admin.message(this, LogAdminImpl.LOG_TRACE, format, arguments);
		return null;
	}

	@Override
	public WARN warn(String format, Object... arguments) {
		if (warn)
			admin.message(this, LogService.LOG_WARNING, format, arguments);
		return null;
	}

	@Override
	public String getName() {
		return scope;
	}

	@Override
	public boolean isInfoEnabled() {
		return info;
	}

	@Override
	public boolean isDebugEnabled() {
		return debug;
	}

	@Override
	public boolean isErrorEnabled() {
		return error;
	}

	@Override
	public boolean isTraceEnabled() {
		return trace;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	public void close() {
		info = trace = error = warn = debug = false;
		admin = null;
	}

	@Override
	public Runnable wrap(final Runnable r, final String name) {
		return new Runnable() {
			public void run() {
				debug("Starting %s in thread %s", name, Thread.currentThread().getName());
				try {
					r.run();
				}
				catch (Exception e) {
					error("Error in %s in thread %s", name, Thread.currentThread().getName());
				}
				finally {
					debug("Leaving %s in thread %s", name, Thread.currentThread().getName());
				}
			}
		};
	}

	@Override
	public <T> Callable<T> wrap(final Callable<T> c, final String name) {
		return new Callable<T>() {
			public T call() {
				debug("Starting %s in thread %s", name, Thread.currentThread().getName());
				try {
					return c.call();
				}
				catch (Exception e) {
					error("Error in %s in thread %s", name, Thread.currentThread().getName());
				}
				finally {
					debug("Leaving %s in thread %s", name, Thread.currentThread().getName());
				}
				return null;
			}
		};
	}
}
