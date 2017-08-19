/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.decompiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.ferrybig.javacoding.graphical.decompiler.support.CFRTalker;

/**
 *
 * @author Fernando
 */
public class DumbDecompiler implements DecompileTask {

	public DumbDecompiler(List<String> decompileClassPath, Path tmpDir, Path jarFile) {
		this.decompileClassPath = decompileClassPath;
		this.tmpDir = tmpDir;
		this.jarFile = jarFile;
	}

	private final List<String> decompileClassPath;
	private final Path tmpDir;
	private final Path jarFile;

	private static final Logger LOG = Logger.getLogger(DumbDecompiler.class.getName());

	private void markFileAsDone(DecompileData data, String file) throws MalformedURLException {
		file = file.replace('.', '/');
		data.fileDecompiled(file + ".class", new File(tmpDir.toFile() + "/" + file + ".java").toURI().toURL());
	}

	@Override
	public boolean decompile(DecompileData data) throws Exception {
		List<String> cmd = new ArrayList<>();
		cmd.add(System.getProperty("java.home") + "/bin/java");
		cmd.add("-cp");
		cmd.add(data.getConfig().getCfr().getAbsolutePath());
		cmd.add("org.benf.cfr.reader.Main");
		cmd.add("--outputdir");
		cmd.add(tmpDir.toAbsolutePath().toString());
		cmd.add(jarFile.toAbsolutePath().toString());
		LOG.log(Level.INFO, "Executing: \"''{0}''\"", cmd.stream().collect(Collectors.joining("' '")));
		ProcessBuilder builder = new ProcessBuilder(cmd);
		builder.redirectErrorStream(true);
		Process p = builder.start();
		data.registerCancelListener(p::destroy);
		List<String> lastLines = new ArrayList<>(10);
		try {
			p.getOutputStream().close();
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String lastLine = null;
				String currentLine;
				while ((currentLine = r.readLine()) != null) {
					if (lastLines.size() < 10) {
						lastLines.add(currentLine);
					}
					LOG.log(Level.INFO, "Read: {0}", currentLine);
					if (currentLine.startsWith("Processing ")
							&& !currentLine.equals("Processing " + jarFile.toAbsolutePath() + " (use silent to silence)")) {
						if (lastLine != null) {
							markFileAsDone(data, lastLine);
						}
						lastLine = currentLine.substring("Processing ".length());
					}
					if (data.checkCancelation()) {
						break;
					}
				}
				if (lastLine != null) {
					markFileAsDone(data, lastLine);
				}
			}
		} finally {
			p.destroy();
		}
		if (p.waitFor() != 0) {
			if (lastLines.size() < 10) {
				throw new IllegalStateException("Invalid exit state: " + p.exitValue() + "\n" + lastLines.stream().collect(Collectors.joining("\n")));
			} else {
				throw new IllegalStateException("Invalid exit state: " + p.exitValue());
			}
		}
		return true;
	}

}
