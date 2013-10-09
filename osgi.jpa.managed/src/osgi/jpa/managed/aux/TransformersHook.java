
package osgi.jpa.managed.aux;

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
	private static final ClassTransformer[]				empty			= new ClassTransformer[0];
	private final MultiMap<Bundle, ClassTransformer>	transformers	= new MultiMap<Bundle, ClassTransformer>();
	private final List<String>							imports;

	TransformersHook(List<String> imports) {
		this.imports = imports;
	}

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
