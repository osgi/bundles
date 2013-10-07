package osgi.jdbc.managed.aux;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import javax.sql.*;

import junit.framework.*;

import org.osgi.framework.*;
import org.osgi.service.jdbc.*;

/**
 * Whitebox (arguably simplistic so far) test for the XADataSourceFactory
 * 
 */
public class XADataSourceFactoryTest extends TestCase {

	private static final String JDBC_TEST = "jdbc:test";

	public void testSimple() throws Exception {
		XADataSourceFactory xad = new XADataSourceFactory();

		DataSourceFactory dsf = mock(DataSourceFactory.class);
		XADataSource ds = mock(XADataSource.class);
		BundleContext context = mock(BundleContext.class);
		
		XAConnection conn = mock(XAConnection.class);

		when(
				dsf.createXADataSource(argThat(new ContainsMatcher(
						DataSourceFactory.JDBC_URL, JDBC_TEST))))
				.thenReturn(ds);
		when(ds.getXAConnection()).thenReturn(conn);

		xad.setDataSourceFactory(dsf);

		Map<String, Object> props = new HashMap<String, Object>();
		props.put(DataSourceFactory.JDBC_URL, JDBC_TEST);
		xad.activate(context,props);

		XAConnection xaConnection = ds.getXAConnection();

		assertEquals(conn, xaConnection);
	}
}
