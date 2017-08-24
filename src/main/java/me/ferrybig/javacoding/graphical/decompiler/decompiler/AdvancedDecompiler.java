/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.decompiler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import me.ferrybig.javacoding.graphical.decompiler.Config;

/**
 *
 * @author Fernando
 */
public class AdvancedDecompiler implements Decompiler {

	private static final Logger LOG = Logger.getLogger(AdvancedDecompiler.class.getName());
	private final Config config;

	private final File jarFile;
	private final DecompileListener listener;
	private boolean started;
	private final Worker worker = new Worker();

	public AdvancedDecompiler(File jarFile, DecompileListener listener, Config config) {
		this.jarFile = jarFile;
		this.listener = listener;
		this.config = config;
		this.worker.addPropertyChangeListener(new PropertyChangeListener() {
			int total = 0;
			int decompiled = 0;
			int progress = 0;

			@Override
			public void propertyChange(PropertyChangeEvent e) {
				switch (e.getPropertyName()) {
					case "progress":
						progress = (int) e.getNewValue();
						listener.setProgress(progress, total, decompiled);
						break;
					case "total":
						total = (int) e.getNewValue();
						break;
					case "decompiled":
						decompiled = (int) e.getNewValue();
						break;
					case "decompiler":
						if (e.getOldValue() instanceof FileDecompiler) {
							listener.decompilePerClassStarted(total);
						}
						break;
				}
			}
		});
	}

	@Override
	public synchronized void setOptions(List<String> options) {
		//worker.setOptions(options);
	}

	@Override
	public synchronized void setPriority(Map<String, Integer> prio) {
		worker.priority.forEach(c -> c.accept(prio));
	}

	@Override
	public boolean isDone() {
		return worker.isDone();
	}

	@Override
	public boolean isSmart() {
		return !(worker.activeDecompiler instanceof DumbDecompiler);
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public void start() {
		started = true;
		worker.execute();
	}

	@Override
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

	private class Worker extends SwingWorker<Object, FilePair> implements DecompileTask.DecompileData {

		private volatile DecompileTask activeDecompiler = null;
		private boolean noticedCancelation = false;
		private final List<Runnable> cancelation = new CopyOnWriteArrayList<>();
		private final List<Consumer<Map<String, Integer>>> priority = new CopyOnWriteArrayList<>();
		private final List<String> remaining = new ArrayList<>();
		private final Map<String, Integer> prio = new HashMap<>();
		private int total = 0;
		private int decompiled = 0;

		@Override
		public boolean checkCancelation() {
			if (noticedCancelation) {
				return true;
			}
			if (this.isCancelled()) {
				noticedCancelation = true;
				cancelation.forEach(Runnable::run);
				return true;
			}
			return false;
		}

		@Override
		public void fileDecompiled(String file, URL url) {
			this.publish(new FilePair(file, url));
			if (total != 0) {
				int old = decompiled++;
				this.firePropertyChange("decompiled", old, decompiled);
				this.setProgress(decompiled * 100 / total);
			}
		}

		@Override
		public void fileFound(String file) {
			remaining.add(file);
			this.publish(new FilePair(file, null));
		}

		@Override
		public void fileListDecompile(List<String> file) {
		}

		@Override
		public Config getConfig() {
			return config;
		}

		@Override
		public Closeable registerCancelListener(Runnable task) {
			this.cancelation.add(task);
			return () -> this.cancelation.remove(task);
		}

		@Override
		public Closeable registerPriorityListener(Consumer<Map<String, Integer>> priority) {
			synchronized (this) {
				priority.accept(prio);
				this.priority.add(priority);
			}
			return () -> {
				synchronized (this) {
					this.priority.remove(priority);
				}
			};
		}

		@Override
		protected Object doInBackground() throws Exception {
			{
				FileDecompiler decompiler = new FileDecompiler(jarFile);
				this.firePropertyChange("decompiler", null, decompiler);
				activeDecompiler = decompiler;
				decompiler.decompile(this);
			}
			int size = remaining.size();
			this.firePropertyChange("total", 0, size);
			total = size;
			if (size == 0) {
				return null;
			}
			Path tmp = listener.getTemporaryPath();
			if (size > 32) {
				SmartDecompiler decompiler = new SmartDecompiler(Collections.emptyList(), tmp, jarFile.toPath(), remaining);
				firePropertyChange("decompiler", activeDecompiler, decompiler);
				activeDecompiler = decompiler;
				if (decompiler.decompile(this)) {
					return null;
				}
			}
			DumbDecompiler decompiler = new DumbDecompiler(Collections.emptyList(), tmp, jarFile.toPath());
			firePropertyChange("decompiler", activeDecompiler, decompiler);
			activeDecompiler = decompiler;
			decompiler.decompile(this);
			return null;
		}

		@Override
		protected void done() {
			super.done();
			try {
				this.get();
			} catch (CancellationException e) {
				LOG.log(Level.INFO, "Cancelled task");
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
