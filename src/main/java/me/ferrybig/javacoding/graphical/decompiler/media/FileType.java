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
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

/**
 *
 * @author Fernando
 */
public enum FileType {
	TEXT(SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_NONE), "txt", "csv", "mf", "MF"),
	JAVA(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_JAVA), "class", "java"),
	XML(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_XML), "xml"),
	YAML(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_YAML), "yml", "yaml"),
	CSS(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_CSS), "css"),
	LESS(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_LESS), "less"),
	HTML(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_HTML), "html"),
	PROPERTIES(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE), "properties"),
	INI(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_INI), "ini"),
	JSON(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS), "json"),
	SHELL(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL), "sh", "bash", "bsh", "shell"),
	JSP(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_JSP), "jsp"),
	IMAGE(ImagePane::new, "png", "jpg", "jpeg", "gif"),;

	private static final Map<String, FileType> byAlias;

	static {
		Map<String, FileType> byAlias0 = new HashMap<>();
		for (FileType val : values()) {
			for (String alias : val.getAliases()) {
				FileType old = byAlias0.put(alias, val);
				assert old == null;
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
		} while (subString > 0);
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
