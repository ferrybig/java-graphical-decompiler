/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.LogRecord;

/**
 *
 * @author Fernando
 */
public class LogRecordPanel extends javax.swing.JPanel {

	private LogRecord record;
	
	/**
	 * Creates new form LogRecordPanel
	 */
	public LogRecordPanel() {
		initComponents();
	}

	public LogRecord getRecord() {
		return record;
	}

	public void setRecord(LogRecord record) {
		this.record = record;
		date.setText(format.format(record.getMillis()));
		message.setText(record.getMessage());
		String txt;
		if(record.getThrown() == null) {
			txt = "";
		} else {
			StringWriter writer = new StringWriter();
			record.getThrown().printStackTrace(new PrintWriter(writer));
			txt = writer.toString();
		}
		exception.setText(txt);
	}
	
	private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        icon = new javax.swing.JLabel();
        message = new javax.swing.JLabel();
        date = new javax.swing.JLabel();
        exception = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        icon.setMaximumSize(new java.awt.Dimension(32, 32));
        icon.setMinimumSize(new java.awt.Dimension(32, 32));
        icon.setPreferredSize(new java.awt.Dimension(32, 32));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        add(icon, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        add(message, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        add(date, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        add(exception, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel date;
    private javax.swing.JLabel exception;
    private javax.swing.JLabel icon;
    private javax.swing.JLabel message;
    // End of variables declaration//GEN-END:variables
}