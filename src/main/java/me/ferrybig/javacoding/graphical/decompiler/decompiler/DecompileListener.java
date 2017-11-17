/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.decompiler;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 *
 * @author Fernando
 */
public interface DecompileListener {

	void fileFound(String name);

	void fileDecompiled(String name, URL url);

	void decompileDone();

	void decompilePerClassStarted(int total);

	void exceptionCaugth(Throwable ex);

	void setProgress(int progress, int totalFiles, int filesDecompiled);

	Path getTemporaryPath() throws IOException;
}
