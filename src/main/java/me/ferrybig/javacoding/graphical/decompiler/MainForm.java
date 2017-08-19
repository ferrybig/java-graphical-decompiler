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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
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

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);
        setSize(new Dimension(1024, 768));
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent evt) {
                formWindowClosed(evt);
            }
        });
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

    private void formWindowClosed(WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
		try {
			mainBody1.programClosed();
		} catch (InterruptedException ex) {
			LOG.log(Level.SEVERE, null, ex);
		}
		LOG.info("Exiting..");
		System.exit(0);
    }//GEN-LAST:event_formWindowClosed

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
			mainForm.checkCFRExists(args);
			LOG.info("Started fully!");
		});
	}

	private void checkCFRExists(String[] openAfterLaunch) {
		if (mainBody1.getConfig().getCfr() == null) {
			int showConfirmDialog = JOptionPane.showConfirmDialog(this, "CFR missing, either download manually from http://www.benf.org/other/cfr/ or press ok to download it");
			if (showConfirmDialog == JOptionPane.OK_OPTION) {
				new CfrDownloader().execute();
			}
		}
		for (String s : openAfterLaunch) {
			mainBody1.openFile(new File(s));
		}
	}

	private final class CfrDownloader extends SwingWorker<Void, Void> {

		@Override
		protected void done() {
			super.done();
			JOptionPane.showMessageDialog(MainForm.this, "CFR download complete");
			MainForm.this.mainBody1.getConfig().rescanCfr();
		}

		@Override
		protected Void doInBackground() throws Exception {
			URL homePage = new URL("http://www.benf.org/other/cfr/");
			URL cfrDownload = null;
			String cfrName = null;
			Pattern pageDownload = Pattern.compile("<a href=\"cfr_0_\\d*.jar\">(cfr_0_\\d*.jar)</a>");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(homePage.openStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					Matcher matcher = pageDownload.matcher(line);
					if (matcher.find()) {
						cfrDownload = new URL("http://www.benf.org/other/cfr/" + matcher.group(1));
						cfrName = matcher.group(1);
					}
				}
			}
			if (cfrDownload == null) {
				throw new IOException("CFR download pattern not found");
			}
			URLConnection openConnection = cfrDownload.openConnection();
			long contentLength = openConnection.getContentLengthLong();
			File target = new File("lib");
			if (!target.exists()) {
				target = new File(".");
			}
			target = new File(target, cfrName);
			try (BufferedInputStream reader = new BufferedInputStream(openConnection.getInputStream())) {
				Files.copy(reader, target.toPath());
			}
			return null;
		}

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
