/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler;

import java.io.File;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 *
 * @author Fernando
 */
public final class Config {

	private volatile File cfr = new File("cfr_0_119.jar");

	public Config() {
		rescanCfr();
	}

	public File getCfr() {
		return cfr;
	}

	public void rescanCfr() {
		File[] toScan = new File[]{new File("."), new File("lib")};
		File latestVersion = null;
		Predicate<String> cfrJar = Pattern.compile("cfr_0_\\d{1,3}.jar").asPredicate();
		for (File dir : toScan) {
			final File[] listFiles = dir.listFiles((File pathname) -> cfrJar.test(pathname.getName()));
			if (listFiles == null) {
				continue;
			}
			for (File cfr : listFiles) {
				if (latestVersion == null || latestVersion.compareTo(cfr) < 0) {
					latestVersion = cfr;
				}
			}
		}
		cfr = latestVersion;
	}
}
