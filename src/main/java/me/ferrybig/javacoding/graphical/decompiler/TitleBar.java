/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

/**
 *
 * @author Fernando
 */
public class TitleBar extends javax.swing.JPanel {

	public TitleBar() {
		initComponents();
	}
	
	public TitleBar(String title) {
		this();
		setTitle(title);
	}

	public final void setTitle(String title) {
		this.title.setText(title);
	}

	public void addActionListener(ActionListener l) {
		close.addActionListener(l);
	}

	public void removeActionListener(ActionListener l) {
		close.removeActionListener(l);
	}

	public ActionListener[] getActionListeners() {
		return close.getActionListeners();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        close = new JButton();
        title = new JLabel();

        setOpaque(false);
        setLayout(new GridBagLayout());

        close.setText("x");
        close.setBorderPainted(false);
        close.setMargin(new Insets(2, 2, 2, 2));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.BASELINE;
        add(close, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.BASELINE;
        gridBagConstraints.weightx = 0.1;
        add(title, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton close;
    private JLabel title;
    // End of variables declaration//GEN-END:variables
}