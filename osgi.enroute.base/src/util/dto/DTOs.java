package osgi.enroute.util.dto;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class that makes it look like Java got structs. It allows fast and
 * efficient handling of field based classes.
 */
public class DTOs {

	/**
	 * Marks a field as a primary key. Only fields with primary keys are used in
	 * the equals comparison and hash code.
	 */
	public @interface Primary {}

	/**
	 * Utility to set a list field.
	 */
	protected <T> List<T> list() {
		return new ArrayList<T>();
	}

	/**
	 * Utility to set a set field.
	 */
	protected <T> Set<T> set() {
		return new LinkedHashSet<T>();
	}

	/**
	 * Utility to set a map field.
	 */
	protected <K, V> Map<K,V> map() {
		return new LinkedHashMap<K,V>();
	}

	/**
	 * Used to sort the names since the order in the class files is undefined.
	 */
	private static Comparator<Field>	fieldComparator	= new Comparator<Field>() {

															@Override
															public int compare(Field a, Field b) {
																return a.getName().compareTo(b.getName());
															}
														};

	/**
	 * A structure to keep our reflection data more efficient than the VM can do
	 * it
	 */

	static class Def {
		final Field[]		fields;
		final Field[]		primary;
		final Class< ? >	clazz;

		/*
		 * Construct a Def from a class, will look at the fields,
		 */
		Def(Class< ? > c) {
			this.clazz = c;

			List<Field> fields = new ArrayList<Field>();
			List<Field> primary = new ArrayList<Field>();

			for (Field f : c.getFields()) {
				if (Modifier.isStatic(f.getModifiers()))
					continue;

				fields.add(f);
				if (f.getAnnotation(Primary.class) != null)
					primary.add(f);
			}
			Collections.sort(fields, fieldComparator);
			this.fields = fields.toArray(new Field[fields.size()]);

			if (primary.isEmpty()) {
				this.primary = null;
				return;
			} else {
				this.primary = primary.toArray(new Field[primary.size()]);
			}
		}

		/**
		 * Calculate a hash code for this struct. If no primary keys are set, we
		 * use the whole object
		 * 
		 * @param target
		 *            the target to calc the hashcode for
		 * @return
		 */
		int hashCode(Object target) {
			int hashCode = 0;

			Field fields[] = this.primary;
			if (fields == null)
				fields = this.fields;

			for (Field f : fields) {
				Object value;
				try {
					value = f.get(target);
					if (value == null)
						hashCode ^= 0xAA554422;
					else
						hashCode ^= value.hashCode();
				}
				catch (Exception e) {
					// cannot happen
					e.printStackTrace();
				}
			}
			return hashCode;
		}

		/**
		 * Calculate the equals for two objects.
		 * 
		 * @param local
		 * @param other
		 * @return
		 */
		boolean equals(Object local, Object other) {
			Field fields[] = this.primary;
			if (fields == null)
				fields = this.fields;

			for (Field f : fields) {
				try {
					Object lv = f.get(local);
					Object ov = f.get(other);
					if (lv != ov) {
						if (lv == null)
							return false;

						if (!lv.equals(ov))
							return false;
					}
				}
				catch (Exception e) {
					// cannot happen
					e.printStackTrace();
				}
			}
			return true;
		}

		/**
		 * Assuming that we do not have lots of keys, the binary search is very
		 * fast.
		 * 
		 * @param key
		 *            the name of the method
		 * @return the field with the given name
		 */
		public Field getField(String key) {
			int lo = 0;
			int hi = fields.length - 1;
			while (lo <= hi) {
				// Key is in a[lo..hi] or not present.
				int mid = lo + (hi - lo) / 2;
				int cmp = key.compareTo(fields[mid].getName());
				if (cmp < 0)
					hi = mid - 1;
				else if (cmp > 0)
					lo = mid + 1;
				else
					return fields[mid];
			}
			return null;
		}

		public Field[] getFields() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	/*
	 * Stores the def fields.
	 */
	private static ConcurrentHashMap<Class< ? >,Def>	defs	= new ConcurrentHashMap<Class< ? >,DTOs.Def>();

	private Def def() {
		return def(getClass());
	}

	private static Def def(Class< ? > c) {
		Def def = defs.get(c);
		if (def != null)
			return def;

		// this can potentially happen multiple
		// times but that is not worth the optimization
		def = new Def(c);
		defs.put(c, def);

		return def;
	}

	/**
	 * Should never be created directly, has no meaning
	 */
	protected DTOs() {}

	/**
	 * Defined to use extra values. This is used by the bnd JSONCodec to store
	 * values not available in a struct
	 */
	public Map<String,Object>	__extra;

	/**
	 * Return a string representation of this struct suitable for use when
	 * debugging.
	 * <p>
	 * The format of the string representation is not specified and subject to
	 * change.
	 * 
	 * @return A string representation of this struct suitable for use when
	 *         debugging.
	 */
	@Override
	public String toString() {
		try {
			return new JSON().enc().put(this).toString();
		}
		catch (IOException e) {
			return e + ": " + super.toString();
		}
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return def().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (this == other)
			return true;

		if (other.getClass() != this.getClass())
			return false;

		return def().equals(this, other);
	}

	public Field[] getFields() {
		return def().fields;
	}

	/*
	 * Compare 2 objects and report true if they are different.
	 */
	public static <T> String diff(T older, T newer) {
		if (older == newer || older.equals(newer))
			return null;

		if (older != null && newer != null) {

			Class< ? > oc = older.getClass();
			Class< ? > nc = newer.getClass();
			if (oc != nc)
				return "different classes " + oc + ":" + nc;

			if (older instanceof Collection< ? >) {
				Collection< ? > co = (Collection< ? >) older;
				Collection< ? > cn = (Collection< ? >) newer;
				if (co.size() != cn.size()) {
					return "#" + co.size() + ":" + cn.size();
				}

				Iterator< ? > io = co.iterator();
				Iterator< ? > in = cn.iterator();
				while (io.hasNext()) {
					Object ioo = io.next();
					Object ino = in.next();
					String diff = diff(ioo, ino);
					if (diff != null)
						return "[" + diff + "]";
				}
				return null;
			}

			if (older instanceof Map< ? , ? >) {
				Map< ? , ? > co = (Map< ? , ? >) older;
				Map< ? , ? > cn = (Map< ? , ? >) newer;
				if (co.size() != cn.size())
					return "#" + co.size() + ":" + cn.size();

				Set< ? > keys = new HashSet<Object>(co.keySet());
				keys.removeAll(cn.keySet());
				if (!keys.isEmpty())
					return "+" + keys;

				for (Map.Entry< ? , ? > e : co.entrySet()) {
					Object no = cn.get(e.getKey());
					if (no == null)
						return "-" + e.getKey();

					String diff = diff(e.getValue(), no);
					if (diff != null)
						return "{" + diff + "}";
				}
			}

			Field[] fields = older.getClass().getFields();
			if (fields.length > 0) {
				for (Field of : older.getClass().getFields()) {
					try {
						Field nf = nc.getField(of.getName());
						String diff = diff(of.get(older), nf.get(newer));
						if (diff != null)
							return nf.getName() + "=" + diff;
					}
					catch (Exception e) {
						return e.toString();
					}
				}
				return null;
			}
		}
		return older + ":" + newer;
	}

	/**
	 * Utility to copy fields from one struct into another, they do not have to
	 * be the same type.
	 * 
	 * @param other
	 *            the struct containing the values
	 */
	public void copyFrom(DTOs other) throws Exception {
		Def def = def();
		for (Field from : other.def().fields) {
			Field to = def.getField(from.getName());

			if (to != null) {
				to.set(this, from.get(other));
			}
		}
	}

	public Map<String,Object> toMap() {
		final Field[] fields = def().fields;

		return Collections.unmodifiableMap(new AbstractMap<String,Object>() {

			@Override
			public Set<java.util.Map.Entry<String,Object>> entrySet() {

				return new AbstractSet<Map.Entry<String,Object>>() {

					@Override
					public Iterator<java.util.Map.Entry<String,Object>> iterator() {
						return new Iterator<java.util.Map.Entry<String,Object>>() {
							int	n	= 0;

							@Override
							public boolean hasNext() {
								return n < fields.length;
							}

							@Override
							public java.util.Map.Entry<String,Object> next() {
								final Field field = fields[n++];

								Map.Entry<String,Object> e = new Entry<String,Object>() {

									@Override
									public Object setValue(Object value) {
										try {
											Object result = getValue();
											field.set(DTOs.this, value);
											return result;
										}
										catch (Exception e) {
											throw new RuntimeException(e);
										}
									}

									@Override
									public Object getValue() {
										try {
											return field.get(DTOs.this);
										}
										catch (Exception e) {
											throw new RuntimeException(e);
										}
									}

									@Override
									public String getKey() {
										return field.getName();
									}
								};
								return e;
							}

							@Override
							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
					}

					@Override
					public int size() {
						return fields.length;
					}
				};
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <T extends DTOs> T shallowCopy(String... keys) throws Exception {
		Arrays.sort(keys);
		DTOs copy = getClass().newInstance();
		for (Field f : def().fields) {
			int n = Arrays.binarySearch(keys, f.getName());
			if (n >= 0) {
				f.set(copy, f.get(this));
			}
		}
		return (T) copy;
	}

	@Override
	public Object clone() {
		return copy(this);
	}

	@SuppressWarnings("unchecked")
	public static Object copy(Object value) {
		try {
			if (value == null)
				return null;

			if ( isImmutable(value.getClass()))
				return value;
			
			if (value instanceof Collection) {
				
				Collection<Object> original = (Collection<Object>) value;
				Collection<Object> result = (Collection<Object>) value.getClass().newInstance();
				for (Object member : original) {
					result.add(copy(member));
				}
				return result;
				
			} else if (value instanceof Map) {
				
				Map<Object,Object> original = (Map<Object,Object>) value;
				Map<Object,Object> result = (Map<Object,Object>) value.getClass().newInstance();
				result.putAll(original);
				return result;

			} else if (value.getClass().isArray()) {
				
				Class< ? > ct = value.getClass().getComponentType();
				int length = Array.getLength(value);
				if (ct.isPrimitive()) {
					if (ct == boolean.class)
						return Arrays.copyOf((boolean[]) value, length);
					if (ct == byte.class)
						return Arrays.copyOf((byte[]) value, length);
					if (ct == char.class)
						return Arrays.copyOf((char[]) value, length);
					if (ct == short.class)
						return Arrays.copyOf((short[]) value, length);
					if (ct == int.class)
						return Arrays.copyOf((int[]) value, length);
					if (ct == long.class)
						return Arrays.copyOf((long[]) value, length);
					if (ct == float.class)
						return Arrays.copyOf((float[]) value, length);

					return Arrays.copyOf((double[]) value, length);
				}

				if (isImmutable(ct))
					return Arrays.copyOf((Object[]) value, length);

				Object[] original = (Object[]) value;
				Object[] result = (Object[]) Array.newInstance(ct, length);
				for (int i = 0; i < length; i++) {
					result[i] = copy(original[i]);
				}
				return result;

			} else if (value instanceof DTOs) {
				
				DTOs original = (DTOs) value;
				DTOs result = (DTOs) value.getClass().newInstance();

				for (Field f : original.def().fields) {
					Object v = f.get(original);
					f.set(result, copy(v));
				}
				return result;
			}

			// We hope the object is immutable ...

			return value;
		}
		catch (Exception e) {
			throw new RuntimeException(e); // cannot happen
		}
	}

	public static boolean isImmutable(Class< ? > ct) {
		return Number.class.isAssignableFrom(ct) || String.class == ct || Enum.class.isAssignableFrom(ct);
	}
}
