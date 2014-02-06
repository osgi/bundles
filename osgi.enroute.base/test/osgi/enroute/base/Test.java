package osgi.enroute.base;

import java.io.InputStream;
import java.net.URL;

public class Test {

	public static void main(String[] args) throws Exception {
		System.out.println("Helo world");
		URL url = new URL(
				"https://bndtools.ci.cloudbees.com/job/bnd.master/lastSuccessfulBuild/artifact/dist/bundles/aQute.libg/aQute.libg-2.8.0.jar");
		InputStream in = url.openStream();
		System.out.println("got " + in);
		
		System.out.println("Helo world");
	}
}
