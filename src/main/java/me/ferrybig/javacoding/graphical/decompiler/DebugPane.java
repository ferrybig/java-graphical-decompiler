/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import javax.swing.AbstractListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

/**
 *
 * @author Fernando
 */
public class DebugPane extends javax.swing.JPanel {

	private final LogRecordModel model = new LogRecordModel();
	private final Handler handler = new Handler() {
		@Override
		public void close() throws SecurityException {
		}

		@Override
		public void flush() {
		}

		@Override
		public void publish(LogRecord record) {
			if(SwingUtilities.isEventDispatchThread()) {
				model.addRecord(record);
			} else {
				record.getSourceClassName();
				SwingUtilities.invokeLater(() -> model.addRecord(record));
			}
			
		}
	};

	public Handler getHandler() {
		return handler;
	}

	/**
	 * Creates new form DebugPane
	 */
	public DebugPane() {
		initComponents();
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

        jScrollPane1 = new JScrollPane();
        jList1 = new JList<>();
        clearButton = new JButton();
        verbosityList = new JComboBox<>();

        setLayout(new GridBagLayout());

        jList1.setModel(model);
        jList1.setCellRenderer(new LogRecordRenderer());
        jScrollPane1.setViewportView(jList1);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        add(jScrollPane1, gridBagConstraints);

        clearButton.setText("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.BASELINE_TRAILING;
        add(clearButton, gridBagConstraints);

        verbosityList.setModel(new DefaultComboBoxModel<>(new String[] { "Off", "Severe", "Warning", "Info", "Fine", "Finer", "Finest", "All" }));
        verbosityList.setSelectedIndex(3);
        verbosityList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                verbosityListActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = GridBagConstraints.BASELINE_LEADING;
        add(verbosityList, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void verbosityListActionPerformed(ActionEvent evt) {//GEN-FIRST:event_verbosityListActionPerformed
		handler.setLevel(Level.parse(verbosityList.getSelectedItem().toString().toUpperCase()));
    }//GEN-LAST:event_verbosityListActionPerformed

    private void clearButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
		this.model.clear();
    }//GEN-LAST:event_clearButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton clearButton;
    private JList<LogRecord> jList1;
    private JScrollPane jScrollPane1;
    private JComboBox<String> verbosityList;
    // End of variables declaration//GEN-END:variables

	private class LogRecordModel extends AbstractListModel<LogRecord> {

		private final LinkedList<LogRecord> records = new LinkedList<>();

		@Override
		public LogRecord getElementAt(int index) {
			return records.get(index);
		}

		@Override
		public int getSize() {
			return records.size();
		}

		public void addRecord(LogRecord record) {
			if (records.size() > 100) {
				records.remove();
				this.fireIntervalRemoved(record, 0, 0);
			}
			records.add(record);
			int size = records.size();
			this.fireIntervalAdded(record, size, size);
		}

		public void clear() {
			this.fireIntervalRemoved(this, 0, records.size());
			records.clear();
		}
	}

	private static class LogRecordRenderer extends LogRecordPanel implements ListCellRenderer<LogRecord> {

		@Override
		public Component getListCellRendererComponent(JList<? extends LogRecord> list, LogRecord value, int index, boolean isSelected, boolean cellHasFocus) {
			this.setRecord(value);
			if (isSelected) {
				this.setBackground(list.getSelectionBackground());
				this.setForeground(list.getSelectionForeground());
			} else {
				this.setBackground(list.getBackground());
				this.setForeground(list.getForeground());
			}
			return this;
		}

	}

}