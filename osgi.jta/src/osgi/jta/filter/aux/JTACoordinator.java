/*
 * Copyright (c) OSGi Alliance (2013). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package osgi.jta.filter.aux;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.transaction.TransactionManager;
import osgi.jta.filter.aux.JTACoordinator.Config;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

/**
 * Begin a transaction and commit it if the request does not throw an Exception.
 * Otherwise, roll the transaction back.
 */
@Component(properties = "pattern=.*", designate = Config.class)
public class JTACoordinator implements Filter {

	private TransactionManager	tm;

	interface Config {
		String pattern();
	}

	@Override
	public void destroy() {

	}

	/**
	 * Filter call.
	 */
	@Override
	public void doFilter(ServletRequest rq, ServletResponse rsp, FilterChain next) throws IOException, ServletException {
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
