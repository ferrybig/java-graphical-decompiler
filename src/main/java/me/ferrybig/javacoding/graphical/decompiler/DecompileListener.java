/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.net.URL;

/**
 *
 * @author Fernando
 */
public interface DecompileListener {
	
	public void fileFound(String name);
	
	public void fileDecompiled(String name, URL url);
	
	public void decompileDone();
	
	public void exceptionCaugth(Throwable ex);
	
	public void setProgress(int progress);
}