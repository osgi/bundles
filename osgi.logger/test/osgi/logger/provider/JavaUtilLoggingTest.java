package osgi.logger.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.osgi.service.log.LogService;

public class JavaUtilLoggingTest extends TestCase {

	public void testJUL() throws Exception {
		LoggerDispatcher.dispatcher = new LoggerDispatcher();
		
		LoggerAdminImpl admin = new LoggerAdminImpl();
		admin.setDaemon(false);
		Map<String,Object> map = new HashMap<>();
		map.put("level", osgi.enroute.logger.api.Level.TRACE);
		admin.activate(map);


		Logger m = Logger.getLogger("");
		m.setLevel(Level.FINEST);
		JavaUtilLoggingHandler jul = new JavaUtilLoggingHandler();
		Logger.getLogger("").addHandler(jul);
		
		Logger logger = Logger.getLogger("a.b.c");
		logger.severe("Bad api");
		
		assertEquals( 1, LoggerDispatcher.dispatcher.queue.size());
		Entry take = LoggerDispatcher.dispatcher.queue.take();
		assertEquals( LogService.LOG_ERROR, take.level);

		
		logger.fine("Bad trace");
		
		// TODO test if log was called
	}
}
