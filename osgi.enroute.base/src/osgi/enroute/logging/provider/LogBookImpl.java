package osgi.enroute.logging.provider;

import java.util.List;

import org.osgi.framework.Bundle;

import osgi.enroute.logging.messages.api.LogBook;

public class LogBookImpl extends MessageFormatter {
	List<LogBookHandler>	handlers;

	public LogBookImpl(LogAdminImpl impl, Bundle b, String scope) {
		super(impl, b, scope);
	}

	public void close() {
		if (handlers == null)
			return;
		
		for ( LogBookHandler h : handlers) {
			h.close();
		}
		handlers.clear();
	}

	@Override
	public <T extends LogBook> T scoped(Class<T> type, String prefix) {
		return admin.scoped(this,type, prefix);
	}
}
