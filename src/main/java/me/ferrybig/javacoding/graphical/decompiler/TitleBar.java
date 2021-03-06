/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 *
 * @author Fernando
 */
public class TitleBar extends javax.swing.JPanel {

	public TitleBar() {
		initComponents();
	}

	protected void fireActionPerformed(ActionEvent event) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		ActionEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ActionListener.class) {
				// Lazily create the event:
				if (e == null) {
					String actionCommand = event.getActionCommand();
					e = new ActionEvent(this,
							ActionEvent.ACTION_PERFORMED,
							actionCommand,
							event.getWhen(),
							event.getModifiers());
				}
				((ActionListener) listeners[i + 1]).actionPerformed(e);
			}
		}
	}

	public TitleBar(String title) {
		this();
		setTitle(title);
	}

	public final void setTitle(String title) {
		this.title.setText(title);
	}

	public void addActionListener(ActionListener l) {
		listenerList.add(ActionListener.class, l);
	}

	public void removeActionListener(ActionListener l) {
		listenerList.remove(ActionListener.class, l);
	}

	public ActionListener[] getActionListeners() {
		return listenerList.getListeners(ActionListener.class);
	}

	// TODO: figure out why adding this listener disabled the normal click action, and see if we can add it back
	private void onMouse(MouseEvent evt) {
		if (evt.getButton() == 3) {
			fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
		}
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
        close.setHorizontalTextPosition(SwingConstants.CENTER);
        close.setMargin(new Insets(1, 1, 1, 1));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                closeActionPerformed(evt);
            }
        });
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

    private void closeActionPerformed(ActionEvent evt) {//GEN-FIRST:event_closeActionPerformed
		fireActionPerformed(evt);
    }//GEN-LAST:event_closeActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton close;
    private JLabel title;
    // End of variables declaration//GEN-END:variables
}
