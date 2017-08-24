/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;

public class FindWorker extends SwingWorker<Void, SearchResult> {

	private static final Logger LOG = Logger.getLogger(FindWorker.class.getName());
	private final Consumer<List<SearchResult>> results;
	private final Consumer<Future<Void>> finish;
	private final Map<String, URL> urls;
	private final Pattern pattern;

	public FindWorker(Map<String, URL> urls, Pattern pattern, Consumer<List<SearchResult>> results, Consumer<Future<Void>> finish) {
		this.urls = new LinkedHashMap<>(urls);
		this.pattern = pattern;
		this.results = results;
		this.finish = finish;
	}

	public void updateUrl(String file, URL url) {
		synchronized (this) {
			this.urls.replace(file, url);
			this.notifyAll();
		}
	}

	@Override
	protected void done() {
		finish.accept(this);
	}

	@Override
	protected void process(List<SearchResult> chunks) {
		results.accept(chunks);
	}

	private void searchInFile(String name, URL url) throws IOException {
		assert url != null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			String line;
			int lineNumber = 1;
			while ((line = reader.readLine()) != null) {
				if (pattern.matcher(line).find()) {
					this.publish(new SearchResult(name, Collections.singletonList(line), pattern, lineNumber, 0));
				}
				lineNumber++;
			}
		}
	}

	@Override
	protected Void doInBackground() throws Exception {
		int seen = 0;
		int total = -1;
		Map<String, URL> scanMap = null;
		do {
			LOG.log(Level.INFO, "Searchloop with {0} items", seen);
			synchronized (this) {
				if (scanMap != null) {
					assert total != -1;
					this.urls.keySet().retainAll(scanMap.keySet());
					scanMap.clear();
				} else {
					assert total == -1;
					assert scanMap == null;
					scanMap = new LinkedHashMap<>();
					total = this.urls.size();
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
						this.wait();
						LOG.info("Waiting for more files...");
					} else {
						break;
					}
				} while (true);
			}
			assert !scanMap.isEmpty();
			assert total != -1;
			Iterator<Map.Entry<String, URL>> iterator
					= scanMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, URL> entry = iterator.next();
				if (entry.getValue() == null) {
					continue;
				}
				searchInFile(entry.getKey(), entry.getValue());
				seen++;
				this.setProgress(seen * 100 / total);
			}
			LOG.log(Level.INFO, "Done searchloop with {0} items", seen);
		} while (seen < total);
		assert seen == total;
		return null;
	}
}
