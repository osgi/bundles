package osgi.jta.filter.aux;

import java.io.*;

import javax.servlet.*;
import javax.transaction.*;

import osgi.jta.filter.aux.JTACoordinator.Config;
import aQute.bnd.annotation.component.*;

@Component(properties = "pattern=.*", designate = Config.class)
public class JTACoordinator implements Filter {

	private TransactionManager tm;

	interface Config {
		String pattern();
	}

	@Override
	public void destroy() {

	}

	@Override
	public void doFilter(ServletRequest rq, ServletResponse rsp,
			FilterChain next) throws IOException, ServletException {
		try {
			tm.begin();
			try {
				next.doFilter(rq, rsp);
				tm.commit();
			} catch (IOException ie) {
				tm.rollback();
				throw ie;
			} catch (ServletException se) {
				tm.rollback();
				throw se;
			} catch (Throwable e) {
				tm.rollback();
				throw new RuntimeException(e);
			}
		} catch (IOException e) {
			throw e;
		} catch (ServletException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

	@Reference
	void setTransactionManager(TransactionManager tm) {
		this.tm = tm;
	}

}
