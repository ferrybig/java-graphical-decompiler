/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.decompiler;

import java.io.Closeable;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import me.ferrybig.javacoding.graphical.decompiler.Config;

public interface DecompileTask {

	public boolean decompile(DecompileTask.DecompileData data) throws Exception;

	public static interface DecompileData {

		public void fileFound(String file);

		public void fileDecompiled(String file, URL url);

		public void fileListDecompile(List<String> file);

		public Config getConfig();

		public Closeable registerPriorityListener(Consumer<Map<String, Integer>> priority);

		public Closeable registerCancelListener(Runnable task);

		public boolean checkCancelation();
	}

}
