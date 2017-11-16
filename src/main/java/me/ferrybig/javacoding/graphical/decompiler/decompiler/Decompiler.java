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

	void setOptions(List<String> options);

	void setPriority(Map<String, Integer> prio);

	boolean isSmart();

	boolean isStarted();

	boolean isDone();

	void start();

	void stop();

}
