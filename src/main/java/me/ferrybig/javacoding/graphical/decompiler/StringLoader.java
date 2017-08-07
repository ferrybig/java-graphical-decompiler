/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

public class StringLoader extends SwingWorker<Void, char[]> {

	private final Consumer<Throwable> error;
	private final URL in;
	private final Consumer<String> update;
	private final StringBuilder build = new StringBuilder();

	public StringLoader(URL in, Consumer<String> update, Consumer<Throwable> error) {
		this.in = in;
		this.update = update;
		this.error = error;
	}

	@Override
	protected void done() {
		try {
			this.get();
		} catch (InterruptedException ex) {
			throw new AssertionError(ex);
		} catch (ExecutionException ex) {
			error.accept(ex.getCause());
		}
	}

	@Override
	protected void process(List<char[]> chunks) {
		for (char[] c : chunks) {
			build.append(c);
		}
		update.accept(build.toString());
	}

	@Override
	protected Void doInBackground() throws Exception {
		try (final InputStream i = in.openStream()) {
			BufferedReader r = new BufferedReader(new InputStreamReader(i));
			char[] c = new char[1024 * 32];
			int read;
			int readOffset = 0;
			do {
				read = r.read(c, readOffset, c.length - readOffset);
				readOffset += read;
				if (readOffset * 2 > c.length) {
					char[] copy = new char[readOffset];
					System.arraycopy(c, 0, copy, 0, readOffset);
					this.publish(new char[][]{(char[]) copy});
					readOffset = 0;
				}
			} while (read > 0);
			if (readOffset > 0) {
				char[] copy = new char[readOffset];
				System.arraycopy(c, 0, copy, 0, readOffset);
				this.publish(new char[][]{(char[]) copy});
			}
		}
		return null;
	}

}
