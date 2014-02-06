package osgi.enroute.executor.provider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import osgi.enroute.logging.messages.api.Format;
import osgi.enroute.logging.messages.api.LogBook;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * This bundle provides a java.util.concurrent.Executor service that can be
 * configured and is shared between all bundles.
 * 
 */
@Component(designate = ExecutorImpl.Config.class, name = "osgi.enroute.executor.provider", configurationPolicy = ConfigurationPolicy.require)
public class ExecutorImpl implements Executor {
	ExecutorService			es;
	BlockingQueue<Runnable>	queue	= new LinkedBlockingQueue<Runnable>();

	/*
	 * Configuration parameters expected from the Config Admin
	 */
	@OCD(description = "Configuration for the enRoute::Executor")
	interface Config {
		@AD(description = "The minimum number of threads allocated to this pool", deflt = "20")
		int coreSize();

		@AD(description = "Maximum number of threads allocated to this pool", deflt = "0")
		int maximumPoolSize();

		@AD(description = "Nr of seconds an idle free thread should survive before being destroyed", deflt = "60")
		long keepAliveTime();
	}

	/*
	 * Defines the log messages 
	 */
	interface EnRouteExecutor extends LogBook {

		@Format("Was shutdown while there were still tasks running: %s")
		INFO shutdownWhileTasksRunning(List<Runnable> running);

	}

	EnRouteExecutor	log;

	/*
	 * Creates a new instance of the underlying implementation of the executor
	 * service (depending on the configuration parameters) if needed, or returns
	 * a pre-existing instance of this service, shared by all bundles.
	 * 
	 * @param properties
	 *            Configuration parameters, passed by the framework
	 */
	@Activate
	void activate(Map<String,Object> properties) {

		Config config = Configurable.createConfigurable(Config.class, properties);

		es = new ThreadPoolExecutor( //
				Math.max(config.coreSize(), 10), //
				Math.max(Runtime.getRuntime().availableProcessors() * 2, config.maximumPoolSize()),//
				Math.max(config.keepAliveTime(), 10), //
				TimeUnit.SECONDS, //
				queue, //
				new ThreadPoolExecutor.CallerRunsPolicy());
	}

	/*
	 * Cancels the tasks submitted by the exiting bundle, shutting down the
	 * executor service if no more bundle is using it
	 * 
	 */
	@Deactivate
	void deactivate() {
		List<Runnable> running = es.shutdownNow();

		if (!running.isEmpty())
			log.shutdownWhileTasksRunning(running);
	}

	/*
	 * Execute a runnable
	 */
	@Override
	public void execute(Runnable command) {
		es.submit(command);
	}

	/*
	 * Reference to the log
	 */
	@Reference
	void setLogBook(LogBook log) {
		this.log = log.scoped(EnRouteExecutor.class, null);
	}

}