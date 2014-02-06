package osgi.enroute.logging.provider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.osgi.service.log.LogService;

import osgi.enroute.logging.messages.api.LogBook;

public class LogBookHandler extends MessageFormatter implements InvocationHandler {

	final LogBookImpl	parent;

	public LogBookHandler(LogAdminImpl admin, String scope, LogBookImpl parent) {
		super(admin, parent.bundle, scope);
		this.parent = parent;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		if (method.getDeclaringClass() == LogBook.class)
			return method.invoke(this, args);
		else {
			Class< ? > type = method.getReturnType();
			if (type == DEBUG.class && isDebugEnabled())
				return admin.message(this, LogService.LOG_DEBUG, method, args);
			if (type == TRACE.class && isTraceEnabled())
				return admin.message(this, LogAdminImpl.LOG_TRACE, method, args);
			if (type == WARN.class && isWarnEnabled())
				return admin.message(this, LogService.LOG_WARNING, method, args);
			if (type == ERROR.class && isErrorEnabled())
				return admin.message(this, LogService.LOG_ERROR, method, args);

			// method without a proper return type
			// treat as info

			if (isInfoEnabled())
				return admin.message(this, LogService.LOG_INFO, method, args);

			return null;
		}
	}

	@Override
	public <T extends LogBook> T scoped(Class<T> type, String prefix) {
		return admin.scoped(parent, type, prefix);
	}


}
