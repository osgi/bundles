package osgi.enroute.logging.messages.api;

import java.util.concurrent.Callable;


public interface LogBook {
	interface TRACE {}

	interface INFO {}

	interface DEBUG {}

	interface WARN {}

	interface ERROR {}

	INFO info(String format, Object... arguments);

	DEBUG debug(String format, Object... arguments);

	ERROR error(String format, Object... arguments);

	TRACE trace(String format, Object... arguments);

	WARN warn(String format, Object... arguments);

	String getName();

	boolean isInfoEnabled();

	boolean isDebugEnabled();

	boolean isErrorEnabled();

	boolean isTraceEnabled();

	boolean isWarnEnabled();


	<T extends LogBook> T scoped(Class<T> type, String prefix);
	
	Runnable wrap( Runnable r, String msg);
	<T> Callable<T> wrap( Callable<T> c, String msg);
}