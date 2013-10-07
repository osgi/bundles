package osgi.jpa.managed.appl.test;

import javax.persistence.*;

@Entity
public class Domain {

	@Override
	public String toString() {
		return "Domain [id=" + id + ", name=" + name + "] " + getClass();
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private long	id;	// SPEC: this was a String in OpenJPA
	private String	name;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
