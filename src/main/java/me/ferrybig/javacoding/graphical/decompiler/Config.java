/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 *
 * @author Fernando
 */
public final class Config {

	private static final Logger LOG = Logger.getLogger(Config.class.getName());

	private volatile File cfr = new File("cfr_0_119.jar");

	public Config() {
		rescanCfr();
	}

	public File getCfr() {
		return cfr;
	}

	@Nonnull
	public Optional<File> getRunningLocation() {
		try {
			URL url = Config.class.getResource(Config.class.getSimpleName().replace(".", "/") + ".class");
			LOG.log(Level.INFO, "Url of myself: {0}", url);
			if (url.getProtocol().equals("jar") && url.toString().startsWith("jar:file")) {

				final JarURLConnection connection
						= (JarURLConnection) url.openConnection();
				final URL fileurl = connection.getJarFileURL();
				assert fileurl.getProtocol().equals("file");
				return Optional.of(new File(fileurl.toURI()).getParentFile());
			} else if (url.getProtocol().equals("file")) {
				String file = new File(url.toURI()).getAbsolutePath();
				int parentDirs = Config.class.getPackage().getName().split("\\.").length + (Config.class.getPackage().getName().isEmpty() ? 0 : 1);
				for (int i = 0; i < parentDirs; i++) {
					file = new File(file).getParent();
				}
				return Optional.of(new File(file));
			} else {
				return Optional.empty();
			}
		} catch (IOException | URISyntaxException ex) {
			LOG.log(Level.SEVERE, null, ex);
		}
		return Optional.empty();
	}

	public void rescanCfr() {
		final Optional<File> runningLocationOptional = getRunningLocation();
		final File runningLocation = runningLocationOptional.orElse(new File("."));
		LOG.log(Level.INFO, "Our application is location in: {0}, {1}", new Object[]{ runningLocation, runningLocationOptional.isPresent() ? "" : "<working-directory>"});
		File[] toScan = new File[]{runningLocation, new File(runningLocation, "lib")};
		File latestVersion = null;
		Predicate<String> cfrJar = Pattern.compile("^cfr[-_]0[._]\\d{1,3}.jar$").asPredicate();
		for (File dir : toScan) {
			final File[] listFiles = dir.listFiles((File pathname) -> cfrJar.test(pathname.getName()));
			if (listFiles == null) {
				continue;
			}
			for (File cfr : listFiles) {
				if (latestVersion == null || latestVersion.compareTo(cfr) < 0) {
					LOG.log(Level.INFO, "Found cfr: {0}", cfr);
					latestVersion = cfr;
				}
			}
		}
		cfr = latestVersion;
	}
}
