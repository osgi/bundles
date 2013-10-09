
package oow.jpa;

import java.io.File;
import javax.xml.bind.JAXB;
import junit.framework.TestCase;
import v2_0.Persistence;
import v2_0.Persistence.PersistenceUnit;

public class TestPersistence extends TestCase {

	static class MyP extends Persistence {
		String	location;

	}

	public void testA() {
		MyP persistence = JAXB.unmarshal(new File("testresources/schema_a.xml"), MyP.class);
		for (PersistenceUnit pu : persistence.getPersistenceUnit()) {
			System.out.println(pu.getJtaDataSource());
			System.out.println(pu.getClazz());
		}
	}
}
