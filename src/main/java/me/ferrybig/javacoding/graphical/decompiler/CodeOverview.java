/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import me.ferrybig.javacoding.graphical.decompiler.decompiler.AdvancedDecompiler;
import me.ferrybig.javacoding.graphical.decompiler.decompiler.DecompileListener;
import me.ferrybig.javacoding.graphical.decompiler.find.FindResults;
import me.ferrybig.javacoding.graphical.decompiler.find.FindWorker;
import me.ferrybig.javacoding.graphical.decompiler.media.CodePane;
import me.ferrybig.javacoding.graphical.decompiler.media.CodePaneConfig;
import me.ferrybig.javacoding.graphical.decompiler.media.FileType;
import me.ferrybig.javacoding.graphical.decompiler.media.UnknownCodePane;

/**
 *
 * @author Fernando
 */
public class CodeOverview extends javax.swing.JPanel implements DecompileListener {

	private static final Logger LOG = Logger.getLogger(CodeOverview.class.getName());
	private final String base;
	private final Map<String, URL> knownFiles = new HashMap<>();
	private final Map<String, DefaultMutableTreeNode> filesMapping = new HashMap<>();
	private final Map<String, CodePane> openFiles = new HashMap<>();
	private final Consumer<Path> pathRegistration;
	private final Map<String, Integer> priorityCache = new HashMap<>();
	private final DefaultTreeModel treeModel;
	private final DefaultMutableTreeNode parent;
	private final String fullName;
	private final Config config;
	private final List<BiConsumer<String, URL>> decompileListeners = new CopyOnWriteArrayList<>();
	private Path tmp;
	private WeakReference<AdvancedDecompiler> decompiler = new WeakReference<>(null);
	private boolean expanded = false;
	private long startTime = 0;
	private boolean done = false;

	public CodeOverview(String base, String fullName, Config config, Consumer<Path> pathRegistration) {
		assert SwingUtilities.isEventDispatchThread();
		this.base = base;
		this.parent = new DefaultMutableTreeNode(base, true);
		this.treeModel = new DefaultTreeModel(parent);
		this.fullName = fullName;
		this.config = config;
		this.pathRegistration = pathRegistration;
		initComponents();
	}

	@Override
	public void decompilePerClassStarted(int total) {
		startTime = System.nanoTime();
		progressFiles.setText("0/" + total);
		this.firePropertyChange("startTime", 0, startTime);
		this.firePropertyChange("total", 0, total);
	}

	public void fileUrlUpdated(CodePaneConfig conf, URL url) {
		assert SwingUtilities.isEventDispatchThread();
		knownFiles.put(conf.getPath(), url);
		if (openFiles.containsKey(conf.getPath())) {
			CodePane old = openFiles.get(conf.getPath());
			CodePane now = old.contentUpdated(url);
			if (old != now && now != null) {
				int index = tabs.indexOfTab(conf.getPath());
				tabs.setComponentAt(index, now.getContent());
				tabs.setIconAt(index, now.getIcon(true));
			}
		}
	}

	@Override
	public Path getTemporaryPath() throws IOException {
		Path tmp = this.tmp;
		if (tmp != null) {
			return tmp;
		}
		synchronized (this) {
			tmp = this.tmp;
			if (tmp != null) {
				return tmp;
			}
			this.tmp = Files.createTempDirectory(base);
			pathRegistration.accept(this.tmp);
			return this.tmp;
		}
	}

	public Path createTempFile(String path) throws IOException {
		final Path target = getTemporaryPath().resolve(path);
		Files.createDirectories(target.getParent());
		return target;
	}

	public void openAs(CodePaneConfig conf, FileType fileType) {
		assert SwingUtilities.isEventDispatchThread();
		int index = tabs.indexOfTab(conf.getPath());
		tabs.setComponentAt(index, fileType.getOpenPane().apply(conf).getContent());
	}

	public void openFile(String file) {
		openFile(file, 0);
	}

	public void openFile(String file, int caretLocation) {
		if (!openFiles.containsKey(file)) {
			LOG.info(file);
			assert knownFiles.containsKey(file);
			URL url = knownFiles.get(file);
			CodePaneConfig conf = new CodePaneConfig(file, url, config);
			CodePane page = conf.createPane();
			if (page == null) {
				page = new UnknownCodePane(conf, this);
			}
			tabs.addTab(file, page.getIcon(url != null), page.getContent());
			TitleBar titleBar = new TitleBar(((PathPart) this.filesMapping.get(file).getUserObject()).getPart());
			int index = tabs.indexOfTab(file);
			tabs.setTabComponentAt(index, titleBar);
			titleBar.addActionListener((ActionEvent e) -> {
				int newIndex = tabs.indexOfTab(file);
				tabs.removeTabAt(newIndex);
				openFiles.remove(file);
			});
			openFiles.put(file, page);
			tabs.setSelectedIndex(index);
			resendPriorityLists();
		} else {
			tabs.setSelectedIndex(tabs.indexOfTab(file));
		}
		if(caretLocation != 0) {
			openFiles.get(file).setCaretLocation(caretLocation);
		}
	}

	public void registerDecompiler(AdvancedDecompiler decompiler) {
		assert SwingUtilities.isEventDispatchThread();
		this.decompiler = new WeakReference<>(decompiler);
	}

	public boolean addDecompileListener(BiConsumer<String, URL> listener) {
		return this.decompileListeners.add(listener);
	}

	public boolean removeDecompileListener(BiConsumer<String, URL> listener) {
		return this.decompileListeners.remove(listener);
	}

	@Override
	public void decompileDone() {
		assert SwingUtilities.isEventDispatchThread();
		this.done = true;
		this.firePropertyChange("done", false, true);
		this.progress.setValue(this.progress.getMaximum());
		this.progress.setString("Done!");
		SwingUtilities.invokeLater(() -> {
			this.remove(progressPanel);
			this.progress = null;
			this.progressFiles = null;
			this.progressPanel = null;
			this.progressTimeleft = null;
		});
	}

	@Override
	public void exceptionCaugth(Throwable ex) {
		assert SwingUtilities.isEventDispatchThread();
		// TODO
	}

	@Override
	public void fileFound(String path) {
		assert SwingUtilities.isEventDispatchThread();
		if (!knownFiles.containsKey(path)) {
			knownFiles.put(path, null);
			String[] splitted = path.split("/");
			DefaultMutableTreeNode node = parent;
			for (int j = 0; j < splitted.length; j++) {
				String s = splitted[j];
				boolean last = splitted.length - 1 == j;
				PathPart p = new PathPart(Arrays.stream(splitted, 0, j + 1).collect(Collectors.joining("/")), s, last);
				DefaultMutableTreeNode child = null;
				int size = node.getChildCount();
				int low = 0;
				int high = size - 1;
				int mid = 0;
				while (low <= high) {
					mid = (low + high) >>> 1;
					DefaultMutableTreeNode next = (DefaultMutableTreeNode) node.getChildAt(mid);
					int cmp = ((PathPart) next.getUserObject()).compareTo(p);
					if (cmp < 0) {
						low = mid + 1;
					} else if (cmp > 0) {
						high = mid - 1;
					} else {
						child = next;
						break;
					}
				}
				if (child == null) {
					child = new DefaultMutableTreeNode(p, !last);
					((DefaultTreeModel) files.getModel()).insertNodeInto(child, node, size == 0 ? 0 : low);
				}
				node = child;
			}
			filesMapping.put(path, node);
		}
	}

	@Override
	public void fileDecompiled(String file, URL url) {
		fileFound(file);
		if (!expanded) {
			this.files.expandRow(0);
			expanded = true;
		}
		this.progress.setString("Decompiled: " + file);
		if (knownFiles.get(file) != null) {
			LOG.log(Level.WARNING, "Dublicate decoding of file {0}", file);
		}
		knownFiles.put(file, url);
		if (openFiles.containsKey(file)) {
			CodePane old = openFiles.get(file);
			CodePane now = old.contentUpdated(url);
			if (old != now && now != null) {
				int index = tabs.indexOfTab(file);
				tabs.setComponentAt(index, now.getContent());
				tabs.setIconAt(index, now.getIcon(true));
			}
		}
		((DefaultTreeModel) files.getModel()).nodeChanged(filesMapping.get(file));
		for (BiConsumer<String, URL> listener : decompileListeners) {
			listener.accept(file, url);
		}
	}

	@Override
	public void setProgress(int progress, int totalFiles, int filesDecompiled) {
		this.progress.setMaximum(totalFiles);
		this.progress.setValue(filesDecompiled);
		this.progressFiles.setText(filesDecompiled + "/" + totalFiles);
		long elapsedTime = System.nanoTime() - startTime;
		long allTimeForDownloading = (elapsedTime * totalFiles / filesDecompiled);
		long remainingTime = allTimeForDownloading - elapsedTime;
		this.progressTimeleft.setText(remainingTime / 1000000 / 1000d + "s left");

	}

	private void resendPriorityLists() {
		AdvancedDecompiler get = this.decompiler.get();
		if (get == null) {
			return;
		}
		this.priorityCache.clear();
		for (CodePane pane : openFiles.values()) {
			for (Map.Entry<String, Integer> entry : pane.getPriority(pane.getContent() == tabs.getSelectedComponent()).entrySet()) {
				this.priorityCache.merge(entry.getKey(), entry.getValue(), (a, b) -> a + b);
			}
		}
		get.setPriority(priorityCache);
	}

	public void startSearch(Pattern pattern, Pattern filePattern) {
		FindResults results = new FindResults((Frame) SwingUtilities.getWindowAncestor(this), pattern, this);
		if (this.startTime == 0) {
			PropertyChangeListener listener = new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (!evt.getPropertyName().equals("startTime")) {
						return;
					}
					startSearch0(pattern, filePattern, results);
					CodeOverview.this.removePropertyChangeListener(this);
				}
			};
			this.addPropertyChangeListener(listener);
		} else {
			startSearch0(pattern, filePattern, results);
		}
		results.setVisible(true);
	}

	private void startSearch0(Pattern pattern, Pattern filePattern, FindResults results) {
		Map<String, URL> files = new LinkedHashMap<>();
		Predicate<String> matches = filePattern.asPredicate();
		boolean needListener = false;
		for (Map.Entry<String, URL> file : this.knownFiles.entrySet()) {
			if (!matches.test(file.getKey())) {
				continue;
			}
			if (file.getValue() == null) {
				needListener = true;
			}
			files.put(file.getKey(), file.getValue());
		}
		FindWorker worker = new FindWorker(files, pattern, results);
		if (needListener) {
			BiConsumer<String, URL> decompileListener = worker::updateUrl;
			PropertyChangeListener listener = new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (!"done".equals(evt.getPropertyName())) {
						return;
					}
					CodeOverview.this.removeDecompileListener(decompileListener);
					CodeOverview.this.removePropertyChangeListener(this);
				}
			};
			this.addDecompileListener(decompileListener);
			this.addPropertyChangeListener(listener);
		}
		results.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				worker.cancel(true);
			}
		});

		worker.execute();
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

        mainSplit = new JSplitPane();
        filesScrollPane = new JScrollPane();
        files = new JTree();
        tabs = new JTabbedPane();
        progressPanel = new JPanel();
        progress = new JProgressBar();
        Box.Filler progressFillerLeft = new Box.Filler(new Dimension(80, 0), new Dimension(80, 0), new Dimension(80, 32767));
        Box.Filler progressFillerRigth = new Box.Filler(new Dimension(80, 0), new Dimension(80, 0), new Dimension(80, 32767));
        progressTimeleft = new JLabel();
        progressFiles = new JLabel();

        setLayout(new GridBagLayout());

        mainSplit.setDividerLocation(300);

        files.setModel(this.treeModel);
        files.setCellRenderer(new CustomTreeCellRenderer());
        files.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                filesMousePressed(evt);
            }
        });
        filesScrollPane.setViewportView(files);

        mainSplit.setLeftComponent(filesScrollPane);

        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                tabsStateChanged(evt);
            }
        });
        mainSplit.setRightComponent(tabs);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        add(mainSplit, gridBagConstraints);

        progressPanel.setLayout(new GridBagLayout());

        progress.setStringPainted(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.weightx = 0.1;
        progressPanel.add(progress, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        progressPanel.add(progressFillerLeft, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        progressPanel.add(progressFillerRigth, gridBagConstraints);

        progressTimeleft.setText("??? s left");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.BASELINE;
        progressPanel.add(progressTimeleft, gridBagConstraints);

        progressFiles.setText("0/?");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        progressPanel.add(progressFiles, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(progressPanel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void filesMousePressed(MouseEvent evt) {//GEN-FIRST:event_filesMousePressed
		int selRow = files.getRowForLocation(evt.getX(), evt.getY());
		TreePath selPath = files.getPathForLocation(evt.getX(), evt.getY());
		if (selRow != -1) {
			DefaultMutableTreeNode last = (DefaultMutableTreeNode) selPath.getLastPathComponent();
			if (last.getAllowsChildren()) {
				return; // Not allowed for now
			}
			PathPart part = (PathPart) last.getUserObject();
			String total = part.getTotal();
			assert total != null;
			if (evt.getClickCount() == 1) {
				if (openFiles.containsKey(total)) {
					tabs.setSelectedIndex(tabs.indexOfTab(total));
				}
			} else if (evt.getClickCount() == 2) {
				this.openFile(total);
			}
		}
    }//GEN-LAST:event_filesMousePressed

    private void tabsStateChanged(ChangeEvent evt) {//GEN-FIRST:event_tabsStateChanged
		resendPriorityLists();
    }//GEN-LAST:event_tabsStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JTree files;
    private JScrollPane filesScrollPane;
    private JSplitPane mainSplit;
    private JProgressBar progress;
    private JLabel progressFiles;
    private JPanel progressPanel;
    private JLabel progressTimeleft;
    private JTabbedPane tabs;
    // End of variables declaration//GEN-END:variables

	private static class PathPart implements Comparable<PathPart> {

		private final String total;
		private final String part;
		private final boolean directory;

		@Override
		public int compareTo(PathPart o) {
			int c = Boolean.compare(directory, o.directory);
			if (c != 0) {
				return c;
			}
			c = part.compareToIgnoreCase(o.part);
			if (c != 0) {
				return c;
			}
			c = part.compareTo(o.part);
			return c;
		}

		public String getTotal() {
			return total;
		}

		public String getPart() {
			return part;
		}

		public PathPart(String total, String part, boolean directory) {
			this.total = total;
			this.part = part;
			this.directory = directory;
		}

		@Override
		public String toString() {
			return part;
		}
	}

	@SuppressWarnings("serial")
	private class CustomTreeCellRenderer extends DefaultTreeCellRenderer {

		private final Map<URL, ImageIcon> imageCache = new HashMap<>();

		@Override
		public Component getTreeCellRendererComponent(JTree tree,
				Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {

			Component ret = super.getTreeCellRendererComponent(tree, value,
					selected, expanded, leaf, row, hasFocus);

			JLabel label = (JLabel) ret;

			final URL imageUrl;
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			final Object userObject = node.getUserObject();
			if (userObject instanceof PathPart && !node.getAllowsChildren()) {
				PathPart part = (PathPart) userObject;
				boolean isLoaded = knownFiles.get(part.getTotal()) != null;
				if (!isLoaded) {
					imageUrl = FileType.LOADING_IMAGE;
				} else {
					FileType type = FileType.findFileType(part.getTotal());
					if (type == null) {
						imageUrl = FileType.UNKNOWN_IMAGE;
					} else {
						imageUrl = type.getImage();
					}
				}
				label.setIcon(imageCache.computeIfAbsent(imageUrl, u -> new ImageIcon(u)));
			}

			return ret;
		}
	}
}
