/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.media;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 *
 * @author Fernando
 */
public interface CodePane {

	CodePane contentUpdated(URL newUrl);

	default void setCaretLocation(int caretLocation) {
	}

	JComponent getContent();

	Icon getIcon(boolean hasSources);

	default Map<String, Integer> getPriority(boolean focus) {
		return Collections.emptyMap();
	}

	default void handleFocus() {
	}

}
