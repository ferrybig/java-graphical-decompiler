/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import me.ferrybig.javacoding.graphical.decompiler.decompiler.AdvancedDecompiler;
import me.ferrybig.javacoding.graphical.decompiler.decompiler.DecompileListener;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import me.ferrybig.javacoding.graphical.decompiler.media.CodePane;
import me.ferrybig.javacoding.graphical.decompiler.media.CodePaneConfig;
import me.ferrybig.javacoding.graphical.decompiler.media.FileType;

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
	private final Map<String, Integer> priorityCache = new HashMap<>();
	private final DefaultTreeModel treeModel;
	private final DefaultMutableTreeNode parent;
	private final String fullName;
	private final Config config;
	private Path tmp;
	private WeakReference<AdvancedDecompiler> decompiler = new WeakReference<>(null);
	private boolean expanded = false;

	public CodeOverview(String base, String fullName, Config config) {
		this.base = base;
		this.parent = new DefaultMutableTreeNode(base, true);
		this.treeModel = new DefaultTreeModel(parent);
		this.fullName = fullName;
		this.config = config;
		initComponents();
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
			this.tmp.toFile().deleteOnExit();
			return this.tmp;
		}
	}

	public OutputStream createTempFile(String path) throws IOException {
		return Files.newOutputStream(getTemporaryPath().resolve(path));
	}

	public void registerDecompiler(AdvancedDecompiler decompiler) {
		this.decompiler = new WeakReference<>(decompiler);
	}

	@Override
	public void decompileDone() {
		this.progress.setValue(100);
		this.progress.setString("Done!");
		this.remove(progressPanel);
	}

	@Override
	public void exceptionCaugth(Throwable ex) {
		// TODO
	}

	@Override
	public void fileFound(String path) {
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
					node.insert(child, size == 0 ? 0 : low); // Maybe use low here...
				}
				node = child;
			}
			filesMapping.put(path, node);
		}
	}

	@Override
	public void fileDecompiled(String file, URL url) {
		if (!expanded) {
			new Timer(1000, e -> this.files.expandRow(0)).start();
			expanded = true;
		}
		fileFound(file);
		this.progress.setString("Decompiled: " + file);
		if (knownFiles.get(file) != null) {
			LOG.warning("Dublicate decoding of file " + file);
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
	}

	@Override
	public void setProgress(int progress) {
		this.progress.setValue(progress);
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

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        jSplitPane1 = new JSplitPane();
        jScrollPane1 = new JScrollPane();
        files = new JTree();
        tabs = new JTabbedPane();
        progressPanel = new JPanel();
        progress = new JProgressBar();

        setLayout(new GridBagLayout());

        jSplitPane1.setDividerLocation(300);

        files.setModel(this.treeModel);
        files.setCellRenderer(new CustomTreeCellRenderer());
        files.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                filesMousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(files);

        jSplitPane1.setLeftComponent(jScrollPane1);

        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                tabsStateChanged(evt);
            }
        });
        jSplitPane1.setRightComponent(tabs);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        add(jSplitPane1, gridBagConstraints);

        progressPanel.setLayout(new GridBagLayout());

        progress.setStringPainted(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        progressPanel.add(progress, gridBagConstraints);

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
				if (!openFiles.containsKey(total)) {
					LOG.info(total);
					assert knownFiles.containsKey(total);
					URL url = knownFiles.get(total);
					CodePane page = new CodePaneConfig(total, url, config).createPane();

					tabs.addTab(total, page.getIcon(url != null), page.getContent());
					TitleBar titleBar = new TitleBar(part.getPart());
					int index = tabs.indexOfTab(total);
					tabs.setTabComponentAt(index, titleBar);
					titleBar.addActionListener((ActionEvent e) -> {
						tabs.removeTabAt(index);
						openFiles.remove(total);
					});
					openFiles.put(total, page);
					tabs.setSelectedIndex(index);
					resendPriorityLists();
				}
			}
		}
    }//GEN-LAST:event_filesMousePressed

    private void tabsStateChanged(ChangeEvent evt) {//GEN-FIRST:event_tabsStateChanged
		resendPriorityLists();
    }//GEN-LAST:event_tabsStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JTree files;
    private JScrollPane jScrollPane1;
    private JSplitPane jSplitPane1;
    private JProgressBar progress;
    private JPanel progressPanel;
    private JTabbedPane tabs;
    // End of variables declaration//GEN-END:variables

	private class PathPart implements Comparable<PathPart> {

		private final String total;
		private final String part;
		private final boolean dir;

		@Override
		public int compareTo(PathPart o) {
			int c = Boolean.compare(dir, o.dir);
			if (c == 0) {
				return part.compareToIgnoreCase(o.part);
			} else {
				return c;
			}
		}

		public String getTotal() {
			return total;
		}

		public String getPart() {
			return part;
		}

		public PathPart(String total, String part, boolean dir) {
			this.total = total;
			this.part = part;
			this.dir = dir;
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
