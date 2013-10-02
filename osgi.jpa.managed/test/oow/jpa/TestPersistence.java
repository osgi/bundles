package oow.jpa;

import java.io.*;

import javax.xml.bind.*;

import v2_0.*;
import v2_0.Persistence.*;
import junit.framework.*;

public class TestPersistence extends TestCase {

	static class MyP extends Persistence {
		String location;
		
	}
	public void testA() {
		MyP persistence = JAXB.unmarshal(new File("testresources/schema_a.xml"), MyP.class);
		for ( PersistenceUnit pu : persistence.getPersistenceUnit() ) {
			System.out.println(pu.getJtaDataSource());
			System.out.println(pu.getClazz());
		}
	}
}
