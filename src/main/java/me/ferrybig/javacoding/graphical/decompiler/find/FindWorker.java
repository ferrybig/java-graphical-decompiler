/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.find;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;

public class FindWorker extends SwingWorker<Void, SearchResult> {

	public static final String PROPERTY_TOTAL_PROGRESS = "totalProgress";
	private static final Logger LOG = Logger.getLogger(FindWorker.class.getName());
	private volatile int filesSeen;
	private final int filesTotal;
	private volatile String lastFile = "";
	private final FindListener listener;
	private final Pattern pattern;
	private volatile boolean shouldFireTotal = true;
	private final Map<String, URL> urls;

	public FindWorker(Map<String, URL> urls, Pattern pattern, Consumer<List<SearchResult>> results, Consumer<Future<Void>> finish,
			IntConsumer total, IntConsumer progress) {
		this(urls, pattern, FindListener.of(results, finish, total, progress));
	}

	public FindWorker(Map<String, URL> urls, Pattern pattern, FindListener listener) {
		this.urls = new HashMap<>(urls);
		this.pattern = pattern;
		this.filesTotal = urls.size();
		this.listener = listener;
		listener.totalCalculated(filesTotal);
		this.addPropertyChangeListener(e -> {
			if (PROPERTY_TOTAL_PROGRESS.equals(e.getPropertyName())) {
				listener.madeProgress(filesSeen, lastFile);
				shouldFireTotal = true;
			}
		});
	}

	public void updateUrl(String file, URL url) {
		synchronized (this) {
			this.urls.replace(file, url);
			this.notifyAll();
		}
	}

	private void searchInFile(String name, URL url) throws IOException {
		assert url != null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			String line;
			int lineNumber = 1;
			int offset = 0;
			while ((line = reader.readLine()) != null) {
				if (pattern.matcher(line).find()) {
					this.publish(new SearchResult(name, Collections.singletonList(line), pattern, lineNumber, 0, offset));
				}
				lineNumber++;
				offset += line.length() + 1;
			}
		} catch (IOException err) {
			LOG.log(Level.WARNING, "Exception during searching of files", err);
		}
	}

	@Override
	protected Void doInBackground() throws Exception {
		int seen = 0;
		Map<String, URL> scanMap = null;
		do {
			LOG.log(Level.FINE, "Searchloop with {0} items", seen);
			synchronized (this) {
				if (scanMap != null) {
					this.urls.keySet().retainAll(scanMap.keySet());
					scanMap.clear();
				} else {
					assert scanMap == null;
					scanMap = new HashMap<>();
				}
				boolean nonNull = false;
				do {
					for (Map.Entry<String, URL> entry : this.urls.entrySet()) {
						if (entry.getValue() != null) {
							nonNull = true;
						}
						scanMap.put(entry.getKey(), entry.getValue());
					}
					if (!nonNull) {
						LOG.fine("Waiting for more files...");
						this.wait();
					} else {
						LOG.log(Level.FINE, "Resuming with {0} remaining", scanMap.size());
						break;
					}
				} while (true);
			}
			assert !scanMap.isEmpty();
			Iterator<Map.Entry<String, URL>> iterator
					= scanMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, URL> entry = iterator.next();
				if (entry.getValue() == null) {
					continue;
				}
				iterator.remove();
				LOG.log(Level.FINER, "Searching in", entry.getValue());
				lastFile = entry.getKey();
				searchInFile(entry.getKey(), entry.getValue());
				seen++;
				if (shouldFireTotal) {
					shouldFireTotal = false;
					this.firePropertyChange(PROPERTY_TOTAL_PROGRESS, this.filesSeen, seen);
					this.filesSeen = seen;
				}
				this.setProgress(Math.min(99, seen * 100 / filesTotal));
			}
			LOG.log(Level.FINE, "Done searchloop with {0} items", seen);
		} while (seen < filesTotal);
		assert seen == filesTotal;
		return null;
	}

	@Override
	protected void done() {
		listener.done(this);
		LOG.log(Level.INFO, "Searchworker done");
	}

	@Override
	protected void process(List<SearchResult> chunks) {
		listener.onResult(chunks);
	}

}
