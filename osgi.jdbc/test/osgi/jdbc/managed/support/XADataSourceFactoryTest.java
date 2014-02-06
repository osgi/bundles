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

package osgi.jdbc.managed.support;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.Map;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import junit.framework.TestCase;
import org.osgi.framework.BundleContext;
import org.osgi.service.jdbc.DataSourceFactory;
import osgi.jdbc.managed.support.XADataSourceFactory;

/**
 * Whitebox (arguably simplistic so far) test for the XADataSourceFactory
 */
public class XADataSourceFactoryTest extends TestCase {

	private static final String	JDBC_TEST	= "jdbc:test";

	public void testSimple() throws Exception {
		XADataSourceFactory xad = new XADataSourceFactory();

		DataSourceFactory dsf = mock(DataSourceFactory.class);
		XADataSource ds = mock(XADataSource.class);
		BundleContext context = mock(BundleContext.class);

		XAConnection conn = mock(XAConnection.class);

		when(dsf.createXADataSource(argThat(new ContainsMatcher(DataSourceFactory.JDBC_URL, JDBC_TEST))))
				.thenReturn(ds);
		when(ds.getXAConnection()).thenReturn(conn);

		xad.setDataSourceFactory(dsf);

		Map<String, Object> props = new HashMap<String, Object>();
		props.put(DataSourceFactory.JDBC_URL, JDBC_TEST);
		xad.activate(context, props);

		XAConnection xaConnection = ds.getXAConnection();

		assertEquals(conn, xaConnection);
	}
}
