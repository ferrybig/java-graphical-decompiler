/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.swing.SwingWorker;
import me.ferrybig.javacoding.graphical.decompiler.support.CFRTalker;

/**
 *
 * @author Fernando
 */
public class Decompiler {

	private static final Logger LOG = Logger.getLogger(Decompiler.class.getName());
	private final Config config;

	private final File jarFile;
	private final DecompileListener listener;
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

	public synchronized void setOptions(List<String> options) {
		worker.setOptions(options);
	}

	public synchronized void setPriority(Map<String, Integer> prio) {
		worker.setPriority(prio);
	}

	public void start() {
		worker.execute();
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

	private class Worker extends SwingWorker<Object, FilePair> {

		@GuardedBy(value = "this")
		private List<String> baseOptions = Collections.emptyList();
		@GuardedBy(value = "this")
		private boolean streamStarted = false;
		@GuardedBy(value = "this")
		@Nullable
		private OutputStream out;
		@GuardedBy(value = "this")
		private List<String> extraOptions = new ArrayList<>();
		@GuardedBy(value = "this")
		private boolean exiting = false;
		@GuardedBy(value = "this")
		private boolean waitingForOptionsAck = false;
		@GuardedBy(value = "this")
		private boolean newOptions = false;

		private synchronized void childStarted(OutputStream out, Path tmp, List<String> decompileInsteadOfCopy) throws IOException {
			this.streamStarted = true;
			this.out = out;
			this.baseOptions = Arrays.asList(
					"--outputdir",
					tmp.toAbsolutePath().toString(),
					jarFile.getAbsolutePath());
			if (out == null) {
				return;
			}
			this.waitingForOptionsAck = true;
			out.write(("options "
					+ Stream.concat(baseOptions.stream(), extraOptions.stream()).collect(Collectors.joining(" "))
					+ "\nclasses "
					+ decompileInsteadOfCopy.stream().map(s -> s.replace(".class", "").replace('/', '.')).collect(Collectors.joining(" "))
					+ "\nstart\n").getBytes(StandardCharsets.UTF_8)
			);
			out.flush();
		}

		public synchronized void setPriority(Map<String, Integer> prio) {
			if (exiting || out == null) {
				return;
			}
			try {
				out.write(("setPrio "
						+ prio.entrySet().stream().map(e -> e.getValue() + ":" + e.getKey()).collect(Collectors.joining(" "))
						+ "\n").getBytes(StandardCharsets.UTF_8)
				);
				out.flush();
			} catch (IOException ex) {
				Logger.getLogger(Decompiler.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		public synchronized void setOptions(List<String> options) {
			extraOptions = options;
			if (!waitingForOptionsAck) {
				try {
					out.write(("options "
							+ Stream.concat(baseOptions.stream(), extraOptions.stream()).collect(Collectors.joining(" "))
							+ "\n").getBytes(StandardCharsets.UTF_8)
					);
					out.flush();
					waitingForOptionsAck = true;
				} catch (IOException ex) {
					Logger.getLogger(Decompiler.class.getName()).log(Level.SEVERE, null, ex);
				}
			} else {
				newOptions = true;
			}
		}

		@Override
		protected Object doInBackground() throws Exception {
			List<String> decompileInsteadOfCopy = new ArrayList<>();
			List<String> decompileClassPath = new ArrayList<>();
			decompileClassPath.add(System.getProperty("java.home") + "/lib/rt.jar");
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
			if (decompileInsteadOfCopy.isEmpty()) {
				return null;
			}
			Path tmp = Files.createTempDirectory(jarFile.getName());
			tmp.toFile().deleteOnExit();

			List<String> runClassPath = new ArrayList<>();
			boolean advancedDecode;
			String mainClass;
			final Class<CFRTalker> cfrClass = CFRTalker.class;

			final URL url = cfrClass.getResource(cfrClass.getSimpleName().replace(".", "/") + ".class");
			if (url.getProtocol().equals("jar") && url.toString().startsWith("jar:file")) {
				final JarURLConnection connection
						= (JarURLConnection) url.openConnection();
				final URL fileurl = connection.getJarFileURL();
				assert fileurl.getProtocol().equals("file");
				runClassPath.add(new File(fileurl.toURI()).getAbsolutePath());
				mainClass = cfrClass.getCanonicalName();
				advancedDecode = true;
			} else if (url.getProtocol().equals("file")) {
				String file = new File(url.toURI()).getAbsolutePath();
				int parentDirs = cfrClass.getPackage().getName().split("\\.").length + (cfrClass.getPackage().getName().isEmpty() ? 0 : 1);
				for (int i = 0; i < parentDirs; i++) {
					file = new File(file).getParent();
				}
				runClassPath.add(file);
				mainClass = cfrClass.getCanonicalName();
				advancedDecode = true;
			} else {
				mainClass = "org.benf.cfr.reader.Main";
				advancedDecode = false;
			}
			runClassPath.add(config.getCfr().getAbsolutePath());

			System.out.println(url.getFile());
			List<String> cmd = new ArrayList<>();
			cmd.add(System.getProperty("java.home") + "/bin/java");
			cmd.add("-cp");
			cmd.add(runClassPath.stream().collect(Collectors.joining(File.pathSeparator)));
			cmd.add(mainClass);
			if (!advancedDecode) {
				cmd.add("--outputdir");
				cmd.add(tmp.toAbsolutePath().toString());
				cmd.add(jarFile.getAbsolutePath());
			}
			LOG.log(Level.INFO, "Executing: \"''{0}''\"", cmd.stream().collect(Collectors.joining("' '")));
			ProcessBuilder builder = new ProcessBuilder(cmd);
			builder.redirectErrorStream(true);
			Process p = builder.start();
			try {
				if (advancedDecode) {
					childStarted(p.getOutputStream(), tmp, decompileInsteadOfCopy);
				} else {
					childStarted(null, tmp, decompileInsteadOfCopy);
					p.getOutputStream().close();
				}
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
						if (line.equals("[CFRTalker] Taskpool: options-done")) {
							if (waitingForOptionsAck) {
								if (newOptions) {
									out.write(("options "
											+ Stream.concat(baseOptions.stream(), extraOptions.stream()).collect(Collectors.joining(" "))
											+ "\n").getBytes(StandardCharsets.UTF_8)
									);
									out.flush();
									newOptions = false;
								} else {
									waitingForOptionsAck = false;
								}
							}
						} else if (line.equals("[CFRTalker] Taskpool: done")) {
							synchronized (this) {
								if (waitingForOptionsAck) {
									continue;
								}
								exiting = true;
							}
							p.getOutputStream().write("exit\n".getBytes(StandardCharsets.UTF_8));
							p.getOutputStream().flush();
							LOG.info("Sending exit notification");
						} else if (line.startsWith("Processing ")
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
				if (advancedDecode) {
					p.getOutputStream().close();
				}
			} finally {
				p.destroy();
			}
			if (p.waitFor() != 0) {
				throw new IllegalStateException("Invalid exit state: " + p.exitValue());
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
}
