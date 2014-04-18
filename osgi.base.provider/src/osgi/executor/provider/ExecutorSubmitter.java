package osgi.executor.provider;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This is the front end for the Executor service. This is a service factory so
 * we provide a unique object to our callers. This allows us to throttle the
 * requests for task execution
 */
@Component(servicefactory = true, property={"service.ranking=-1000"})
public class ExecutorSubmitter implements Executor {
	final Semaphore			limiter	= new Semaphore(0, true);
	private ExecutorImpl	executor;

	@Activate
	void activate() {
		
		//
		// Hmm, not sure but a bundle should not be able to monopolize the 
		// current CPU
		//
		
		int cores = Runtime.getRuntime().availableProcessors();
		if (cores > 2)
			cores = cores - 1;

		limiter.release(cores);
	}

	@Override
	public void execute(Runnable command) {
		try {
			limiter.acquire();
			try {
				executor.execute(command);
			}
			finally {
				limiter.release();
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Reference
	void setExecutor(ExecutorImpl ei) {
		this.executor = ei;

	}
}
