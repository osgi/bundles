package osgi.jdbc.managed.aux;

import java.util.Properties;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ContainsMatcher extends BaseMatcher<Properties> {

	private String value;
	private String key;

	public ContainsMatcher(String key, String value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public boolean matches(Object arg0) {
		Properties p = (Properties) arg0;
		String url = (String) p.get(key);
		return value.equals(url);
	}

	@Override
	public void describeTo(Description arg0) {
	}

}
