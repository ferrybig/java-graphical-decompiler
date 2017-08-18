/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.decompiler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Fernando
 */
public class FileDecompiler implements DecompileTask {

	private static final Logger LOG = Logger.getLogger(FileDecompiler.class.getName());

	public FileDecompiler(File jarFile) {
		this.jarFile = jarFile;
	}

	private final File jarFile;

	@Override
	public boolean decompile(DecompileTask.DecompileData data) throws IOException {
		try (JarFile j = new JarFile(jarFile)) {
			Enumeration<JarEntry> entries = j.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				String name = entry.getName();
				URL url;
				if (name.endsWith(".class")) {
					if (name.contains("$")) {
						// Java subclass
						continue;
					}
					url = null;
				} else {
					url = new URL("jar:" + jarFile.getAbsoluteFile().toURI().toURL() + "!/" + name);
					LOG.log(Level.INFO, "Path: {0}", url);
				}
				if (url == null) {
					data.fileFound(name);
				}
				data.fileDecompiled(name, url);
			}
		}
		return true;
	}

}
