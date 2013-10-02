package osgi.jdbc.managed.aux;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import junit.framework.TestCase;

import org.osgi.service.jdbc.DataSourceFactory;

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
		XAConnection conn = mock(XAConnection.class);

		when(
				dsf.createXADataSource(argThat(new ContainsMatcher(
						DataSourceFactory.JDBC_URL, JDBC_TEST))))
				.thenReturn(ds);
		when(ds.getXAConnection()).thenReturn(conn);

		xad.setDataSourceFactory(dsf);

		Map<String, Object> props = new HashMap<String, Object>();
		props.put(DataSourceFactory.JDBC_URL, JDBC_TEST);
		xad.activate(props);

		XAConnection xaConnection = xad.getXAConnection();

		assertEquals(conn, xaConnection);
	}
}
