/*
 * Copyright (c) OSGi Alliance (2013). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package osgi.jdbc.managed.support;

import java.util.Properties;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ContainsMatcher extends BaseMatcher<Properties> {

	private String	value;
	private String	key;

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
