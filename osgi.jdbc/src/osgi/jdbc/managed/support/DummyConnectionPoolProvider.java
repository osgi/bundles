package osgi.jdbc.managed.support;

import java.util.Map;
import javax.sql.XADataSource;
import osgi.jdbc.managed.api.ConnectionPoolProvider;
import aQute.bnd.annotation.component.Component;

@Component
public class DummyConnectionPoolProvider implements ConnectionPoolProvider {
	public XADataSource pool(XADataSource ds) throws Exception {
		return ds;
	}

	@Override
	public XADataSource pool(XADataSource ds, Map<String, Object> properties) {
		// TODO Auto-generated method stub
		return null;
	}

}
