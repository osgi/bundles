package osgi.jdbc.managed.api;

import java.util.Map;
import javax.sql.XADataSource;
import aQute.bnd.annotation.ConsumerType;

@ConsumerType
public interface ConnectionPoolProvider {
	String	POOL	= "pool";
	String	DUMMY	= "dummy";

	XADataSource pool(XADataSource ds, Map<String, Object> properties);
}
