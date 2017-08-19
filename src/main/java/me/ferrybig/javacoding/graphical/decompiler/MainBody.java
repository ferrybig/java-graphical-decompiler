/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.awt.Component;
import me.ferrybig.javacoding.graphical.decompiler.decompiler.AdvancedDecompiler;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.Timer;

/**
 *
 * @author Fernando
 */
public class MainBody extends javax.swing.JPanel {

	private static final Logger LOG = Logger.getLogger(MainBody.class.getName());

	private Config config;
	private final List<Path> tempPaths = new CopyOnWriteArrayList<>();

	public MainBody() {
		initComponents();
	}

	public void registerTempPath(Path path) {
		synchronized (this) {
			tempPaths.add(path);
		}
	}

	public void programClosed() throws InterruptedException {
		int count = mainPane.getTabCount();
		for (int i = 0; i < count; i++) {
			Component tabComponentAt = mainPane.getTabComponentAt(i);
			if (tabComponentAt instanceof TitleBar) {
				TitleBar titleBar = (TitleBar) tabComponentAt;
				titleBar.fireActionPerformed(new ActionEvent(this, i, ""));
			}
		}
		int failedTimes = 0;
		while (!tempPaths.isEmpty() && failedTimes < 10) {
			LOG.info("Trying to delete temp files...");
			for (Path path : tempPaths) {
				try {
					LOG.log(Level.INFO, "Deleting {0}", path);
					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

						@Override
						public FileVisitResult visitFile(Path file,
								BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir,
								IOException exc) throws IOException {
							if (exc != null) {
								throw exc;
							}
							Files.delete(dir);
							return CONTINUE;
						}

					});
					tempPaths.remove(path);
					if (failedTimes > 0) {
						failedTimes--;
					}
				} catch (IOException ex) {
					LOG.log(Level.SEVERE, "Failed deleting " + path, ex);
					failedTimes++;
				}
			}
			Thread.sleep(failedTimes * 500);
		}
	}

	public void registerLoggingHandler() {
		Logger.getGlobal().getParent().addHandler(debugPane.getHandler());
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public void openFile(File file) {
		CodeOverview codeOverview = new CodeOverview(file.getName(), file.getAbsolutePath(), config, this::registerTempPath);
		AdvancedDecompiler decompiler = new AdvancedDecompiler(file, codeOverview, config);
		codeOverview.registerDecompiler(decompiler);
		addTab(file.getAbsolutePath(), file.getName(), codeOverview, decompiler::stop);
		decompiler.start();
	}

	public void addTab(String title, String friendly, JComponent com, Runnable cancelable) {

		mainPane.addTab(title, com);
		int index = mainPane.indexOfTab(title);
		TitleBar titleBar = new TitleBar(friendly);
		mainPane.setTabComponentAt(index, titleBar);
		titleBar.addActionListener((ActionEvent e) -> {
			//mainPane.removeTabAt(mainPane.indexOfTab(title));
			mainPane.remove(com);
			cancelable.run();
		});
		// Race condition in the creation of the contents of the JTree, 
		// and the loading of the tab, to lazy to figure out how to update the 
		// tree after the model changes
		Timer t = new Timer(200, e -> mainPane.setSelectedIndex(mainPane.indexOfTab(title)));
		t.setRepeats(false);
		t.start();
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

        mainPane = new JTabbedPane();
        debugPane = new DebugPane();

        setLayout(new GridBagLayout());

        mainPane.addTab("Debug", debugPane);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        add(mainPane, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private DebugPane debugPane;
    private JTabbedPane mainPane;
    // End of variables declaration//GEN-END:variables
}
