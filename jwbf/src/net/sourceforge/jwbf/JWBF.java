/*
 * Copyright 2007 Thomas Stock.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors:
 * 
 */
package net.sourceforge.jwbf;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 
 * @author Thomas Stock
 *
 */
public final class JWBF {

	private static final Map<String, String> MODULES = new HashMap<String, String>();

	
	static {
		String packagename = JWBF.class.getPackage().getName().replace('.',
				File.separatorChar);
		URL url = JWBF.class.getClassLoader().getResource(packagename);
		final String jarFileIndex = "jar:file:";
		boolean isJar = url.toExternalForm().toLowerCase().contains(jarFileIndex);
		if (isJar) {
			try {
				int jarEnd = url.toExternalForm().indexOf("!/");
				String jarFileName = url.toExternalForm().substring(jarFileIndex.length(), jarEnd);
				JarFile jar = new JarFile(jarFileName);
				Enumeration<JarEntry> je =  jar.entries();
				while (je.hasMoreElements()) {
					JarEntry jarEntry = (JarEntry) je.nextElement();
					String slashCount =  jarEntry.getName().replaceAll("[a-zA-Z0-9]", "");
					if (jarEntry.isDirectory() && jarEntry.getName().contains(packagename) 
							&& slashCount.length() == 4 ) {
						System.out.println(jarEntry.getName());
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				File root = new File(url.toURI());
				File[] dirs = root.listFiles();
				for (int i = 0; i < dirs.length; i++) {
					if (dirs[i].isDirectory()) {
						System.out.println(dirs[i]); // TODO RM
						registerModule(readArtifactId("file:" + url.toURI()),
								readVersion("file:" + url.toURI()));
					}

				}
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		/*
		String[] cp = System.getProperty("java.class.path").split(":");
		for (int i = 0; i < cp.length; i++) {
			try {

				if (cp[i].endsWith(".jar") && cp[i].contains("jwbf")) {
					registerModule(readArtifactId("file:" + cp[i]),
							readVersion("file:" + cp[i]));

				} else if (cp[i].contains("jwbf")) {
					registerModule(readArtifactId("file:" + cp[i]),
							readVersion("file:" + cp[i]));
				}
			} catch (Exception e) {
				System.err.println(cp[i] + " seems to be no regular module");
			}

		}
		*/

	}

	/**
	 * 
	 *
	 */
	private JWBF() {
		// do nothing
	}

	/**
	 * 
	 * @param artifactId
	 *            a
	 * @param version
	 *            a
	 */
	private static void registerModule(String artifactId, String version) {
		MODULES.put(artifactId, version);

	}

	/**
	 * @param clazz
	 *            a class of the module
	 * @return the version
	 */
	public static String getVersion(Class<?> clazz) {
		try {
			return readVersion(clazz);
		} catch (Exception e) {
			return "Version Unknown";
		}
	}

	/**
	 * @param clazz
	 *            a class of the module
	 * @return the version
	 */
	public static String getArtifactId(Class<?> clazz) {
		try {
			return readArtifactId(clazz);
		} catch (Exception e) {
			return "No Module for " + clazz.getName();
		}
	}

	/**
	 * Prints the JWBF Version.
	 */
	public static void printVersion() {
		System.out.println(MODULES);
	}

	public static void main(String[] args) {
		printVersion();
	}

	/**
	 * @return the JWBF Version.
	 */
	public static Map<String, String> getVersion() {
		return MODULES;
	}

	/**
	 * @param clazz
	 *            a
	 * @return the version from manifest
	 * @throws IOException
	 *             if path to class is invalid
	 */
	private static String readVersion(Class<?> clazz) throws IOException {

		String classContainer = clazz.getProtectionDomain().getCodeSource()
				.getLocation().toString();

		return readVersion(classContainer);

	}

	/**
	 * 
	 * @param path
	 *            a
	 * @return the version from manifest
	 * @throws IOException
	 *             if path invalid
	 */
	private static String readVersion(String path) throws IOException {

		String implementationVersion = null; // =
												// clazz.getPackage().getImplementationVersion();

		implementationVersion = readFromManifest(path, "Implementation-Version");

		if (implementationVersion == null) {
			return "DEVEL";
		} else {
			return implementationVersion;
		}

	}

	/**
	 * @param clazz
	 *            a
	 * @return the version from manifest
	 * @throws IOException
	 *             if path to class is invalid
	 */
	private static String readArtifactId(Class<?> clazz) throws IOException {

		String classContainer = clazz.getProtectionDomain().getCodeSource()
				.getLocation().toString();
		return readArtifactId(classContainer);

	}

	/**
	 * 
	 * @param path
	 *            a
	 * @return the
	 * @throws IOException
	 *             if path invalid
	 */
	private static String readArtifactId(String path) throws IOException {

		String implementationTitle = null; // =
											// clazz.getPackage().getImplementationTitle();
		implementationTitle = readFromManifest(path, "Implementation-Title");

		if (implementationTitle == null) {
			return "jwbf-generic";
		} else {
			return implementationTitle;
		}
	}

	/**
	 * 
	 * @param path
	 *            a
	 * @param key
	 *            a
	 * @return value
	 * @throws IOException
	 *             if path invalid
	 */
	private static String readFromManifest(String path, String key)
			throws IOException {

		URL manifestUrl = null;
		if (path.endsWith(".jar")) {

			manifestUrl = new URL("jar:" + path + "!/META-INF/MANIFEST.MF");
		} else {
			if (!path.endsWith("/"))
				path += "/";
			manifestUrl = new URL(path + "../../../../target/MANIFEST.MF");
		}
		Manifest manifest = new Manifest(manifestUrl.openStream());
		return manifest.getMainAttributes().getValue(key);
	}

}
