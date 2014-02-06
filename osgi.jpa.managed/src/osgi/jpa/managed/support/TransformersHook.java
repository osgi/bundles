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

package osgi.jpa.managed.support;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import javax.persistence.spi.ClassTransformer;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;
import aQute.lib.collections.MultiMap;

/**
 * This class is a WeavingHook service which is used to handle the transformers.
 */
class TransformersHook implements WeavingHook {
	private static final ClassTransformer[]				empty				= new ClassTransformer[0];
	private final MultiMap<Bundle, ClassTransformer>	transformers		= new MultiMap<Bundle, ClassTransformer>();
	private final List<String>							imports;
	private final ClassTransformer						DUMMY_TRANSFORMER	= new ClassTransformer() {

																				@Override
																				public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
																						ProtectionDomain protectionDomain, byte[] classfileBuffer)
																						throws IllegalClassFormatException {
																					return null;
																				}

																			};

	TransformersHook(List<String> imports) {
		this.imports = imports;
	}

	@Override
	public void weave(WovenClass clazz) {
		try {
			if (transformers.isEmpty())
				return;

			BundleWiring wiring = clazz.getBundleWiring();
			Bundle b = wiring.getBundle();

			ClassTransformer trfs[];
			synchronized (transformers) {
				List<ClassTransformer> list = transformers.get(b);
				if (list == null)
					return;
				trfs = list.toArray(empty);
			}
			System.out.println("transforming " + Arrays.toString(trfs) + " " + clazz);
			for (ClassTransformer ctf : trfs) {
				if (ctf != null) {
					ctf.transform(wiring.getClassLoader(), clazz.getClassName(), clazz.getDefinedClass(),
							clazz.getProtectionDomain(), clazz.getBytes());
				}

			}

			if (!imports.isEmpty())
				clazz.getDynamicImports().addAll(imports);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	boolean register(Bundle b, ClassTransformer ctf) {
		System.out.println("register transformer " + ctf + " on bundle " + b);
		if (ctf == null) {
			ctf = DUMMY_TRANSFORMER;
		}
		synchronized (transformers) {
			return transformers.add(b, ctf);
		}
	}

	boolean unregister(Bundle b, ClassTransformer ctf) {
		System.out.println("unregister transformer " + ctf + " on bundle " + b);
		synchronized (transformers) {
			return transformers.remove(b, ctf);
		}
	}
}
