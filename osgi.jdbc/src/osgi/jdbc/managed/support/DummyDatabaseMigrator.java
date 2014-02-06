package osgi.jdbc.managed.support;

import javax.sql.XADataSource;
import osgi.jdbc.managed.api.DatabaseMigrator;
import aQute.bnd.annotation.component.Component;

@Component(properties={"migrator=dummy"})
public class DummyDatabaseMigrator implements DatabaseMigrator{

	@Override
	public void migrate(String name, XADataSource ds) throws Exception {
		// we're bailing ....
	}

}
