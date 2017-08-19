/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.decompiler;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import me.ferrybig.javacoding.graphical.decompiler.support.CFRTalker;

/**
 *
 * @author Fernando
 */
public class SmartDecompiler implements DecompileTask {

	private static final String CFR_TALKER_TASKPOOL_TASKDONE = "[CFRTalker] Taskpool: task-done: ";
	private static final int CFR_TALKER_TASKPOOL_TASKDONE_LENGTH = CFR_TALKER_TASKPOOL_TASKDONE.length();
	private static final String CFR_TALKER_TASKPOOL_TASKSTART = "[CFRTalker] Taskpool: task-start: ";
	private static final int CFR_TALKER_TASKPOOL_TASKSTART_LENGTH = CFR_TALKER_TASKPOOL_TASKSTART.length();
	private static final Logger LOG = Logger.getLogger(SmartDecompiler.class.getName());
	private static final Pattern PATTERN_SPACE = Pattern.compile(" ");
	private final List<String> baseOptions;
	private final List<String> classes;

	@GuardedBy(value = "this")
	private boolean exiting = false;
	@GuardedBy(value = "this")
	private List<String> extraOptions = new ArrayList<>();
	private final Path jarFile;
	@GuardedBy(value = "this")
	private boolean newOptions = false;
	@GuardedBy(value = "this")
	@Nullable
	private OutputStream out;
	private final Path tmpDir;
	@GuardedBy(value = "this")
	private boolean waitingForOptionsAck = false;

	public SmartDecompiler(List<String> decompileClassPath, Path tmpDir, Path jarFile, List<String> classes) {
		this.tmpDir = tmpDir;
		this.jarFile = jarFile;
		this.classes = classes;
		this.baseOptions = Collections.unmodifiableList(Arrays.asList(
				"--outputdir",
				tmpDir.toAbsolutePath().toString(),
				jarFile.toAbsolutePath().toString()));
	}

	@Override
	public boolean decompile(DecompileData data) throws Exception {
		final Path toAbsolutePath = jarFile.toAbsolutePath();

		List<String> runClassPath = new ArrayList<>(2);
		final Class<CFRTalker> cfrClass = CFRTalker.class;

		final URL url = cfrClass.getResource(cfrClass.getSimpleName().replace(".", "/") + ".class");
		if (url.getProtocol().equals("jar") && url.toString().startsWith("jar:file")) {
			final JarURLConnection connection
					= (JarURLConnection) url.openConnection();
			final URL fileurl = connection.getJarFileURL();
			assert fileurl.getProtocol().equals("file");
			runClassPath.add(new File(fileurl.toURI()).getAbsolutePath());
		} else if (url.getProtocol().equals("file")) {
			String file = new File(url.toURI()).getAbsolutePath();
			int parentDirs = cfrClass.getPackage().getName().split("\\.").length + (cfrClass.getPackage().getName().isEmpty() ? 0 : 1);
			for (int i = 0; i < parentDirs; i++) {
				file = new File(file).getParent();
			}
			runClassPath.add(file);
		} else {
			return false;
		}
		runClassPath.add(data.getConfig().getCfr().getAbsolutePath());

		List<String> cmd = new ArrayList<>();
		cmd.add(System.getProperty("java.home") + "/bin/java");
		cmd.add("-cp");
		cmd.add(runClassPath.stream().collect(Collectors.joining(File.pathSeparator)));
		cmd.add(cfrClass.getCanonicalName());
		LOG.log(Level.INFO, "Executing: \"''{0}''\"", cmd.stream().collect(Collectors.joining("' '")));
		ProcessBuilder builder = new ProcessBuilder(cmd);
		builder.redirectErrorStream(true);
		Process p = builder.start();

		try (Closeable ignored = data.registerCancelListener(p::destroy); Closeable ignored2 = data.registerPriorityListener(this::setPriority)) {
			childStarted(p.getOutputStream());
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				final String ignoredProcessing = "Processing " + toAbsolutePath + " (use silent to silence)";
				final int ignoredProcessingLength = ignoredProcessing.length();
				Map<String, PerThreadStatus> statuses = new HashMap<>();
				PerThreadStatus[] taskById = new PerThreadStatus[8];
				String line;
				mainLoop:
				while ((line = r.readLine()) != null) {
					LOG.log(Level.FINE, "Read: {0}", line);
					while (line.startsWith(ignoredProcessing)) {
						if (line.length() == ignoredProcessingLength) {
							continue mainLoop;
						}
						line = line.substring(ignoredProcessingLength);
					}
					if (line.equals("[CFRTalker] Taskpool: options-done")) {
						if (waitingForOptionsAck) {
							if (newOptions) {
								sendOptions();
								newOptions = false;
							} else {
								waitingForOptionsAck = false;
							}
						}
					} else if (line.equals("[CFRTalker] Taskpool: done")) {
						synchronized (this) {
							if (waitingForOptionsAck) {
								continue;
							}
							exiting = true;
						}
						p.getOutputStream().write("exit\n".getBytes(StandardCharsets.UTF_8));
						p.getOutputStream().flush();
						LOG.info("Sending exit notification");
					} else if (line.startsWith(CFR_TALKER_TASKPOOL_TASKSTART)) {
						String[] split = PATTERN_SPACE.split(
								line.substring(CFR_TALKER_TASKPOOL_TASKSTART_LENGTH));
						int taskID = Integer.parseInt(split[0]);
						if (taskById.length < taskID) {
							PerThreadStatus[] old = taskById;
							taskById = new PerThreadStatus[Math.max(taskID, old.length * 2)];
							System.arraycopy(old, 0, taskById, 0, old.length);
						}
						final PerThreadStatus perThreadStatus = new PerThreadStatus(taskID);
						if (taskById[taskID] != null) {
							throw new IllegalStateException("Overrode task " + taskID);
						}
						taskById[taskID] = perThreadStatus;
						for (int i = 1; i < split.length; i++) {
							statuses.put(split[i], perThreadStatus);
						}
					} else if (line.startsWith(CFR_TALKER_TASKPOOL_TASKDONE)) {
						int taskID = Integer.parseInt(
								line.substring(CFR_TALKER_TASKPOOL_TASKDONE_LENGTH));
						String part = taskById[taskID].getLastLine();
						taskById[taskID] = null;
						if (part != null) {
							markFileAsDone(data, part);
						}
					} else if (line.startsWith("Processing ")) {
						String decompiled = line.substring("Processing ".length());
						PerThreadStatus task = statuses.remove(decompiled);
						if (task == null) {
							throw new IllegalStateException("CFR bridge returned " + line + " without announcing it");
						}
						if (task.getLastLine() != null) {
							markFileAsDone(data, task.getLastLine());
						}
						task.setLastLine(decompiled);
					}
					if (data.checkCancelation()) {
						p.destroy();
					}
				}
			}
			p.getOutputStream().close();
		} finally {
			p.destroy();
		}
		if (p.waitFor() != 0) {
			throw new IllegalStateException("Invalid exit state: " + p.exitValue());
		}
		return true;
	}

	public synchronized void setOptions(List<String> options) {
		extraOptions = options;
		if (!waitingForOptionsAck) {
			sendOptions();
		} else {
			newOptions = true;
		}
	}

	public synchronized void setPriority(Map<String, Integer> prio) {
		if (exiting || out == null) {
			return;
		}
		try {
			out.write(("setPrio\0"
					+ prio.entrySet().stream().map(e -> e.getValue() + ":" + e.getKey()).collect(Collectors.joining("\0"))
					+ "\n").getBytes(StandardCharsets.UTF_8)
			);
			out.flush();
		} catch (IOException ex) {
			Logger.getLogger(AdvancedDecompiler.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private synchronized void childStarted(OutputStream out) throws IOException {
		if (out == null) {
			throw new NullPointerException("out");
		}
		this.out = out;
		this.waitingForOptionsAck = true;
		sendOptions();
		out.write(("classes\0"
				+ classes.stream().map(s -> s.replace(".class", "").replace('/', '.')).collect(Collectors.joining("\0"))
				+ "\nstart\n").getBytes(StandardCharsets.UTF_8)
		);
		out.flush();
	}

	private void markFileAsDone(DecompileData data, String file) throws MalformedURLException {
		file = file.replace('.', '/');
		data.fileDecompiled(file + ".class", new File(tmpDir.toFile() + "/" + file + ".java").toURI().toURL());
	}

	private void sendOptions() {
		try {
			out.write(("options\0"
					+ Stream.concat(baseOptions.stream(), extraOptions.stream()).collect(Collectors.joining("\0"))
					+ "\n").getBytes(StandardCharsets.UTF_8)
			);
			out.flush();
			waitingForOptionsAck = true;
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, null, ex);
		}
	}

	private static class PerThreadStatus {

		@Nullable
		private String lastLine;
		private final int taskId;

		public PerThreadStatus(int taskId) {
			this.taskId = taskId;
		}

		public int getTaskId() {
			return taskId;
		}

		public void setLastLine(String lastLine) {
			this.lastLine = lastLine;
		}

		public String getLastLine() {
			return lastLine;
		}

	}

}
