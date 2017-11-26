/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.media;

import java.net.URL;
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
	TEXT(SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_NONE), getResource("text.png"), "txt", "csv", "mf", "MF"),
	JAVA(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_JAVA), getResource("java.png"), "class", "java"),
	XML(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_XML), null, "xml"),
	YAML(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_YAML), null, "yml", "yaml"),
	CSS(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_CSS), null, "css"),
	LESS(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_LESS), null, "less"),
	HTML(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_HTML), null, "html"),
	PROPERTIES(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE), null, "properties"),
	INI(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_INI), null, "ini"),
	JSON(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS), null, "json"),
	SHELL(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL), null, "sh", "bash", "bsh", "shell"),
	JSP(TEXT, SyntaxPane.forSyntax(SyntaxConstants.SYNTAX_STYLE_JSP), null, "jsp"),
	IMAGE(ImagePane::new, getResource("image.png"), "png", "jpg", "jpeg", "gif"),;

	public static final URL LOADING_IMAGE = getResource("hourclass.png");
	public static final URL UNKNOWN_IMAGE = getResource("unknown.png");

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
		file = file.toLowerCase();
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

	private static URL getResource(String resource) {
		return FileType.class.getResource(resource);
	}

	private final List<String> aliases;
	private final URL image;
	private final Function<CodePaneConfig, CodePane> openPane;
	private final FileType parent;

	private FileType(Function<CodePaneConfig, CodePane> openPane, URL image, String... aliases) {
		this(null, openPane, image, aliases);
	}

	private FileType(FileType parent, Function<CodePaneConfig, CodePane> openPane, URL image, String... aliases) {
		this.openPane = openPane;
		this.parent = parent;
		if (image == null) {
			this.image = parent.image;
		} else {
			this.image = image;
		}
		this.aliases = Collections.unmodifiableList(Arrays.asList(aliases));
	}

	public List<String> getAliases() {
		return aliases;
	}

	public URL getImage() {
		return image;
	}

	public Function<CodePaneConfig, CodePane> getOpenPane() {
		return openPane;
	}

	public FileType getParent() {
		return parent;
	}

	public String toString() {
		if (this.parent != null) {
			return this.parent.toString() + '/' + this.name().toLowerCase();
		} else {
			return this.name().toLowerCase();
		}
	}

}
