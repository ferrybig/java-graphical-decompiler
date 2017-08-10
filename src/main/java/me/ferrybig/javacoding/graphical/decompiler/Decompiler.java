/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;

/**
 *
 * @author Fernando
 */
public class Decompiler {

	private final File jarFile;
	private final DecompileListener listener;
	private final Config config;
	private final Worker worker = new Worker();

	public Decompiler(File jarFile, DecompileListener listener, Config config) {
		this.jarFile = jarFile;
		this.listener = listener;
		this.config = config;
		this.worker.addPropertyChangeListener(e -> {
			if (e.getPropertyName().equals("progress")) {
				listener.setProgress((int) e.getNewValue());
			}
		});
	}

	public void start() {
		worker.execute();
	}

	private class Worker extends SwingWorker<Object, FilePair> {

		@Override
		protected Object doInBackground() throws Exception {
			List<String> decompileInsteadOfCopy = new ArrayList<>();
			List<String> cp = new ArrayList<>();
			cp.add(System.getProperty("java.home") + "/lib/rt.jar");
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
						decompileInsteadOfCopy.add(name);
					}
					this.publish(new FilePair(name, url));
				}
			}
			if (!decompileInsteadOfCopy.isEmpty()) {
				Path tmp = Files.createTempDirectory(jarFile.getName());
				tmp.toFile().deleteOnExit();
				List<String> cmd = new ArrayList<>();
				cmd.add(System.getProperty("java.home") + "/bin/java");
				cmd.add("-jar");
				cmd.add(config.getCfr().getAbsolutePath());
				cmd.add("--outputdir");
				cmd.add(tmp.toAbsolutePath().toString());
				cmd.add(jarFile.getAbsolutePath());
				LOG.log(Level.INFO, "Executing: \"'{0}'\"", cmd.stream().collect(Collectors.joining("' '")));
				ProcessBuilder builder = new ProcessBuilder(cmd);
				builder.redirectErrorStream(true);
				Process p = builder.start();
				try {
					p.getOutputStream().close();
					try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
						String line;
						int read = 0;
						int total = decompileInsteadOfCopy.size();

						// This is needed because the processing lines "lagg" behind,
						// they are printed when CFR starts processing the class, and
						// not when its done processing the file.
						FilePair pub = null;

						while ((line = r.readLine()) != null) {

							LOG.log(Level.INFO, "Read: {0}", line);
							if (line.startsWith("Processing ")
									&& !line.equals("Processing " + jarFile.getAbsolutePath() + " (use silent to silence)")) {
								if (pub != null) {
									this.publish(pub);
									pub = null;
								}
								read++;
								String decompiled = line.substring("Processing ".length()).replace('.', '/');
								this.setProgress(Math.min(read * 100 / total, 99));
								pub = new FilePair(decompiled + ".class", new File(tmp + "/" + decompiled + ".java").toURI().toURL());
								// Calculate progress using decompileInsteadOfCopy size
							}
							if (this.isCancelled()) {
								p.destroy();
							}
						}
						if (pub != null) {
							this.publish(pub);
						}
					}
				} finally {
					p.destroy();
				}
				if (p.waitFor() != 0) {
					throw new IllegalStateException("Invalid exit state: " + p.exitValue());
				}
			}
			return null;
		}

		@Override
		protected void done() {
			super.done();
			try {
				this.get();
			} catch (ExecutionException | InterruptedException e) {
				LOG.log(Level.WARNING, "Exception during decompilation:", e);
				listener.exceptionCaugth(e);
			}
			listener.decompileDone();
		}

		@Override
		protected void process(List<FilePair> chunks) {
			super.process(chunks);
			for (FilePair p : chunks) {
				if (p.getUrl() == null) {
					listener.fileFound(p.getName());
					LOG.log(Level.FINE, "Render decompiling: {0}", p);
				} else {
					listener.fileDecompiled(p.getName(), p.getUrl());
					LOG.log(Level.FINE, "Render normal: {0}", p);
				}
			}
		}

	}

	public void stop() {
		this.worker.cancel(true);
	}

	private class FilePair {

		private final String name;
		private final URL url;

		public FilePair(String name, URL url) {
			this.name = name;
			this.url = url;
		}

		public String getName() {
			return name;
		}

		public URL getUrl() {
			return url;
		}

		@Override
		public String toString() {
			return "FilePair{" + "name=" + name + ", url=" + url + '}';
		}

	}
	private static final Logger LOG = Logger.getLogger(Decompiler.class.getName());
}
