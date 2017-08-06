/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import me.ferrybig.javacoding.graphical.decompiler.media.CodePane;
import me.ferrybig.javacoding.graphical.decompiler.media.CodePaneConfig;
import me.ferrybig.javacoding.graphical.decompiler.media.JavaPane;
import me.ferrybig.javacoding.graphical.decompiler.media.TextPane;

/**
 *
 * @author Fernando
 */
public class Config {

	private final Map<String, Function<CodePaneConfig, CodePane>> mediaTypes = new HashMap<>();
	private final Map<String, String> fileTypes = new LinkedHashMap<>();
	private final File crf = new File("cfr_0_119.jar");

	public Config() {
		mediaTypes.put("text", TextPane::new);
		mediaTypes.put("text/java", JavaPane::new);
		fileTypes.put(".java", "text/java");
		fileTypes.put(".class", "text/java");
		fileTypes.put(".yml", "text");
		fileTypes.put(".txt", "text");
		fileTypes.put(".properties", "text");
		fileTypes.put(".csv", "text");
		fileTypes.put(".xml", "text");
	}

	public Map<String, Function<CodePaneConfig, CodePane>> getMediaTypes() {
		return mediaTypes;
	}

	public Map<String, String> getFileTypes() {
		return fileTypes;
	}

	public File getCrf() {
		return crf;
	}
}
