package osgi.logger.provider;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;
import org.slf4j.LoggerFactory;

import osgi.enroute.logger.api.Level;
import osgi.enroute.logger.api.LoggerAdmin.Control;
import osgi.enroute.logger.api.LoggerAdmin.Settings;

public class Slf4jTest extends TestCase {

	public void testFunctional() throws Exception {
		LoggerDispatcher.dispatcher = new LoggerDispatcher();
		
		Bundle bundle = mock(Bundle.class);
		AbstractLogger l= new AbstractLogger(bundle, "test");
		l.error("Error 1");
		assertEquals( 1, LoggerDispatcher.dispatcher.queue.size());
		assertTrue( l.error);
		assertTrue( l.warn);
		assertFalse( l.debug);
		assertFalse( l.trace);
		assertFalse( l.info);

		LoggerDispatcher.dispatcher.queue.clear();		
	}
	
	
	public void testAdmin() throws Exception {
		LoggerDispatcher.dispatcher = new LoggerDispatcher();
		LoggerAdminImpl admin = new LoggerAdminImpl();
		admin.setDaemon(false);
		Map<String,Object> map = new HashMap<>();
		map.put("level", Level.DEBUG);
		admin.activate(map);
		
		assertEquals( admin, LoggerDispatcher.dispatcher.admin);
		
		LogService log = mock(LogService.class);
		admin.addLogService(log);
		
		AbstractLogger logger = (AbstractLogger) LoggerFactory.getLogger("test");
		logger.error("Error={}", 1);
		logger.trace("Trace 1");
		logger.debug("Debug 1");
		
		TimeUnit.MILLISECONDS.sleep(500);
		
		verify(log).log(LogService.LOG_ERROR, "test :: Error=1");
		verify(log).log(LogService.LOG_DEBUG, "test :: Debug 1");
		
		reset(log);
		
		Settings s = new Settings();
		Control c = new Control();
		c.pattern = "*";
		c.level = Level.ERROR;
		s.controls.add(c);
		
		admin.setSettings(s);
		
		logger.error("Error={}", 2);
		logger.trace("Trace 2");
		logger.debug("Debug 2");
		
		TimeUnit.MILLISECONDS.sleep(200);
		
		verify(log).log(LogService.LOG_ERROR, "test :: Error=2");
		verifyNoMoreInteractions(log);
	}

	public void testSimple() {
		LoggerDispatcher.dispatcher = new LoggerDispatcher();
		AbstractLogger logger = (AbstractLogger) LoggerFactory.getLogger("test");

		// By default not initialized, all flags true
		assertTrue(logger.debug);
		assertTrue(logger.error);
		assertTrue(logger.warn);
		assertTrue(logger.info);
		assertTrue(logger.trace);
		assertFalse(logger.init);
		assertFalse(logger.registered);

		// Should not be registered yet
		assertEquals(0, LoggerDispatcher.dispatcher.loggers.size());

		logger.trace("Trace");
		assertEquals("Trace disables, so no msg in queue", 0, LoggerDispatcher.dispatcher.queue.size());
		assertEquals("By default, trace is disabled but should register anyway", 1,
				LoggerDispatcher.dispatcher.loggers.size());
		assertFalse(logger.debug);
		assertFalse(logger.info);
		assertFalse(logger.trace);
		assertTrue(logger.error);
		assertTrue(logger.warn);
		assertTrue(logger.init);
		assertTrue(logger.registered);

		logger.error("Error");
		assertEquals(1, LoggerDispatcher.dispatcher.queue.size());

		logger.close();
		assertEquals("No more loggers", 0, LoggerDispatcher.dispatcher.loggers.size());

	}
}
