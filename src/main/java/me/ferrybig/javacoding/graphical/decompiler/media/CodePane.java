/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.media;

import java.net.URL;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 *
 * @author Fernando
 */
public interface CodePane {

	public CodePane contentUpdated(URL newUrl);

	public JComponent getContent();

	public Icon getIcon(boolean hasSources);

}
