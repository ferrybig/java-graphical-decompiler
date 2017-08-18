/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.decompiler;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Fernando
 */
public interface Decompiler {

	public void setOptions(List<String> options);

	public void setPriority(Map<String, Integer> prio);

	public boolean isSmart();

	public boolean isStarted();

	public boolean isDone();

	public void start();

	public void stop();

}
