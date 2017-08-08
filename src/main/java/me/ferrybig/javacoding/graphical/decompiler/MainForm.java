/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 *
 * @author Fernando
 */
public class MainForm extends javax.swing.JFrame {

	/**
	 * Creates new form MainForm
	 */
	public MainForm() {
		initComponents();
		mainBody1.setConfig(new Config());
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

        files = new JFileChooser();
        mainBody1 = new MainBody();
        jToolBar1 = new JToolBar();
        jButton1 = new JButton();
        mainMenu = new JMenuBar();
        fileMenu = new JMenu();
        openButton = new JMenuItem();
        saveResourcesButton = new JMenuItem();
        menuSeperator = new JPopupMenu.Separator();
        exitButton = new JMenuItem();
        editMenu = new JMenu();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
        setSize(new Dimension(1024, 768));
        getContentPane().setLayout(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        getContentPane().add(mainBody1, gridBagConstraints);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        jButton1.setText("Open");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(SwingConstants.BOTTOM);
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        getContentPane().add(jToolBar1, gridBagConstraints);

        fileMenu.setText("File");

        openButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK));
        openButton.setText("Open jar");
        openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });
        fileMenu.add(openButton);

        saveResourcesButton.setText("jMenuItem1");
        fileMenu.add(saveResourcesButton);
        fileMenu.add(menuSeperator);

        exitButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
        exitButton.setText("Exit");
        fileMenu.add(exitButton);

        mainMenu.add(fileMenu);

        editMenu.setText("Edit");
        mainMenu.add(editMenu);

        setJMenuBar(mainMenu);
    }// </editor-fold>//GEN-END:initComponents

    private void openButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
		int f = files.showOpenDialog(this);
		if (f == JFileChooser.CANCEL_OPTION) {
			return;
		}
		this.mainBody1.openFile(files.getSelectedFile());
    }//GEN-LAST:event_openButtonActionPerformed

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		/* Set the Nimbus look and feel */
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
		 */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(() -> {
			MainForm mainForm = new MainForm();
			mainForm.mainBody1.registerLoggingHandler();
			mainForm.setVisible(true);
			LOG.info("Started fully!");
			for (String s : args) {
				mainForm.mainBody1.openFile(new File(s));
			}
		});
	}
	private static final Logger LOG = Logger.getLogger(MainForm.class.getName());

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JMenu editMenu;
    private JMenuItem exitButton;
    private JMenu fileMenu;
    private JFileChooser files;
    private JButton jButton1;
    private JToolBar jToolBar1;
    private MainBody mainBody1;
    private JMenuBar mainMenu;
    private JPopupMenu.Separator menuSeperator;
    private JMenuItem openButton;
    private JMenuItem saveResourcesButton;
    // End of variables declaration//GEN-END:variables
}
