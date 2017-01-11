/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.media;

import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;
import me.ferrybig.javacoding.graphical.decompiler.Config;

/**
 *
 * @author Fernando
 */
public class CodePaneConfig {

	private final String path;
	private final URL url;
	private final Config config;

	public CodePaneConfig(String path, URL url, Config config) {
		this.path = path;
		this.url = url;
		this.config = config;
	}

	public String getPath() {
		return path;
	}

	public URL getUrl() {
		return url;
	}

	public Config getConfig() {
		return config;
	}

	@Override
	public String toString() {
		return "CodePaneConfig{" + "path=" + path + ", url=" + url + ", config=" + config + '}';
	}

	public CodePaneConfig setUrl(URL url) {
		return new CodePaneConfig(path, url, config);
	}

	public CodePane createPane() {
		if (this.getUrl() == null) {
			return new LoadingPane(this);
		}
		for (Map.Entry<String, String> next : config.getFileTypes().entrySet()) {
			if (this.getPath().endsWith(next.getKey())) {
				LOG.info("Created pane for " + next.getValue() + " content");
				return config.getMediaTypes().get(next.getValue()).apply(this);
			}
		}
		throw new IllegalArgumentException("Not defined!");
	}
	private static final Logger LOG = Logger.getLogger(CodePaneConfig.class.getName());

}
