package osgi.enroute.struct.util;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.IdentityHashMap;
import java.util.Map;

public class JSON {
	boolean	refs	= false;
	int		limit	= -1;
	int		indent	= 0;

	public Encoder enc(Appendable app) {
		return new Encoder(app);
	}

	public Encoder enc() {
		return new Encoder(null);
	}

	/**
	 * Encodes an object to a JSON stream.
	 */
	public class Encoder {
		int					level		= 0;
		Appendable			result;
		Map<Object,String>	objectRefs	= new IdentityHashMap<>();

		public Encoder(Appendable app) {
			this.result = app == null ? new StringBuilder() : app;
		}

		public Encoder put(Object value) throws IOException {
			append("#", value);
			return this;
		}

		/**
		 * Append the specified value's string representation to the specified
		 * StringBuilder.
		 * 
		 * @param result
		 *            StringBuilder to which the string representation is
		 *            appended.
		 * @param objectRefs
		 *            References to "seen" objects.
		 * @param refpath
		 *            The reference path of the specified value.
		 * @param value
		 *            The object whose string representation is to be appended.
		 * @return The specified StringBuilder.
		 * @throws IOException
		 */
		private void append(final String refpath, Object value) throws IOException {
			if (value == null) {
				result.append("null");
				return;
			}

			if (value instanceof CharSequence) {
				appendString(result, (CharSequence) value);
				return;
			}

			// Numbers
			if (value instanceof Number || value instanceof Boolean) {
				result.append(value.toString());
				return;
			}
			// Characters
			if (value instanceof Character) {
				appendString(result, (CharSequence) value);
				return;
			}

			// Complex types
			
			// Check the references
			
			final String path = objectRefs.get(value);
			if (path != null) {
				if (refs) {
					result.append("{\"$ref\":");
					appendString(result, path);
					result.append("}");
				} else
					throw new IllegalArgumentException(
							"Cycles are not allowed by default. You can override this in the JSON object setAllowReferences(). ");
			}

			objectRefs.put(value, refpath);

			try {

				if (value instanceof struct)
					value = ((struct) value).toMap();

				if (value instanceof Map) {
					
					Map< ? , ? > map = (Map< ? , ? >) value;
					incr('{');
					String delim = "";
					for (Map.Entry< ? , ? > entry : map.entrySet()) {
						result.append(delim);
						String name = String.valueOf(entry.getKey());
						indent();
						appendString(result, name);
						result.append(":");
						final Object v = entry.getValue();
						append(refpath + "/" + name, v);
						if ( indent > 0) {
							result.append('\n');
							indent();
						} else
							delim = ", ";
					}
					decr('}');
				} else if (value instanceof Iterable) {
					incr('[');
					int i = 0;
					for (Object item : (Iterable< ? >) value) {
						if (i > 0) {
							result.append(",");
						}
						append(refpath + "/" + i, item);
						i++;
					}
					result.append("]");
					decr(']');
				} else if (value.getClass().isArray()) {
					//
					// Handle arrays
					//
					incr('[');
					String del ="";
					final int length = Array.getLength(value);
					for (int i = 0; i < length; i++) {
						result.append(del);
						append(refpath + "/" + i, Array.get(value, i));
						del = ", ";
					}
					decr(']');
				} else {
					if (!(value instanceof CharSequence))
						value = value.toString();

					appendString(result, (CharSequence) value);
				}
			}
			finally {
				if (!refs)
					objectRefs.remove(value);
			}
		}

		private void incr(char c) throws IOException {
			result.append(c).append("\n");
			if (indent > 0) {
				level++;
				indent();
			}
		}

		private void indent() throws IOException {
			for ( int i=0; i<level*indent; i++) {
				result.append(' ');
			}
		}

		private void decr(char c) throws IOException {
			result.append(c).append("\n");
			if (indent > 0) {
				level--;
				indent();
			}
		}
		/**
		 * Append the specified string to the specified StringBuilder.
		 * 
		 * @param result
		 *            StringBuilder to which the string is appended.
		 * @param string
		 *            The string to be appended.
		 * @return The specified StringBuilder.
		 * @throws IOException
		 */
		private void appendString(final Appendable result, CharSequence string) throws IOException {
			if (limit > 0 && limit >= string.length()) {
				StringBuilder sb = new StringBuilder(string.subSequence(0, limit / 2));
				sb.append("...");
				sb.append(string.subSequence(limit / 2, string.length()));
				string = sb;
			}

			result.append("\"");

			for (int i = 0; i < string.length(); i++) {
				char c = string.charAt(i);
				switch (c) {
					case '"' :
					case '\\' :
						result.append('\\').append(c);
						break;

					// fall through

					default :
						if (c < 0x20) {
							String hex = Integer.toHexString(c | 0x10000);
							result.append("\\u").append(hex.substring(1));
						} else
							result.append(c);
				}
			}
			result.append("\"");
		}

		public String toString() {
			return result.toString();
		}
	}

	public JSON references() {
		refs = true;
		return this;
	}

	public JSON limit(int limit) {
		this.limit = limit;
		return this;
	}

	public JSON indent(int indent) {
		this.indent = indent;
		return this;
	}
}
