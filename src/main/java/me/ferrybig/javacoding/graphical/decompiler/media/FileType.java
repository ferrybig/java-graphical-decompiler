/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.media;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Fernando
 */
public enum FileType {
	TEXT(TextPane::new, "txt", "properties", "yml", "csv", "xml", "mf"),
	JAVA(TEXT, JavaPane::new, "class", "java"),;

	private static final Map<String, FileType> byAlias;

	static {
		Map<String, FileType> byAlias0 = new HashMap<>();
		for (FileType val : values()) {
			for (String alias : val.getAliases()) {
				byAlias0.put(alias, val);
			}
		}
		byAlias = Collections.unmodifiableMap(byAlias0);
	}

	public static FileType findFileType(String file) {
		int subString = 0;
		do {
			FileType type = byAlias.get(subString == 0 ? file : file.substring(subString));
			if (type != null) {
				return type;
			}
			subString = file.indexOf('.', subString) + 1;
		} while (subString >= 0);
		return null;
	}

	private final Function<CodePaneConfig, CodePane> openPane;

	private final FileType parent;

	private final List<String> aliases;

	private FileType(Function<CodePaneConfig, CodePane> openPane, String... aliases) {
		this(null, openPane, aliases);
	}

	private FileType(FileType parent, Function<CodePaneConfig, CodePane> openPane, String... aliases) {
		this.openPane = openPane;
		this.parent = parent;
		this.aliases = Collections.unmodifiableList(Arrays.asList(aliases));
	}

	public Function<CodePaneConfig, CodePane> getOpenPane() {
		return openPane;
	}

	public FileType getParent() {
		return parent;
	}

	public List<String> getAliases() {
		return aliases;
	}

}
