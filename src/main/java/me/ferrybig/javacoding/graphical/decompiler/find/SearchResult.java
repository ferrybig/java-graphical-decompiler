/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.find;

import java.util.List;
import java.util.regex.Pattern;

public class SearchResult {

	private final int caretOffset;
	private final String file;
	private final int lineNumber;
	private final int listLineNumber;
	private final List<String> match;
	private final Pattern pattern;

	public SearchResult(String file, List<String> match, Pattern pattern, int lineNumber, int listLineNumber, int caretOffset) {
		this.file = file;
		this.match = match;
		this.pattern = pattern;
		this.lineNumber = lineNumber;
		this.listLineNumber = listLineNumber;
		this.caretOffset = caretOffset;
	}

	public String getFile() {
		return file;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public int getListLineNumber() {
		return listLineNumber;
	}

	public List<String> getMatch() {
		return match;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public int getCaretOffset() {
		return caretOffset;
	}
}
