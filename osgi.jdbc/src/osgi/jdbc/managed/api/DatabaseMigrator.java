package osgi.jdbc.managed.api;

import javax.sql.XADataSource;
import aQute.bnd.annotation.ConsumerType;

@ConsumerType
public interface DatabaseMigrator {
	String MIGRATOR = "migrator";
	String DUMMY = "dummy";
	
	void migrate(String name, XADataSource ds) throws Exception;
}
