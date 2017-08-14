/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;

/**
 *
 * @author Fernando
 */
public class CFRTalker {

	private static final int CLASSES_PER_INVOCATION = 8;
	private static final int MAX_TASKS = 1;//Runtime.getRuntime().availableProcessors();
	private volatile ExecutorService pool;
	private final AtomicReference<List<String>> decompiling;
	private boolean stopping = false;
	private final Method cfrMain;
	private volatile String[] baseArgs;

	public CFRTalker() {
		decompiling = new AtomicReference<>(Collections.emptyList());
		try {
			cfrMain = Class.forName("org.benf.cfr.reader.Main").getMethod("main", String[].class);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException ex) {
			throw new RuntimeException(ex);
		}
	}

	public synchronized void start() {
		pool = Executors.newFixedThreadPool(MAX_TASKS, (Runnable r) -> {
			Thread t = new Thread(r);
			t.setPriority(Thread.NORM_PRIORITY - 2);
			t.setDaemon(true);
			return t;
		});
		stopping = false;
		for (int i = 0; i < MAX_TASKS; i++) {
			newTask();
		}
	}

	@GuardedBy(value = "this")
	private boolean newTask() {
		if (stopping || decompiling.get().isEmpty()) {
			return false;
		}
		List<String> toDecompile = new ArrayList<>(CLASSES_PER_INVOCATION);
		decompiling.getAndUpdate(s -> {
			ArrayList<String> copy = new ArrayList<>(s);
			ListIterator<String> iterator = copy.listIterator(copy.size());
			toDecompile.clear();
			for (int i = 0; iterator.hasPrevious() && i < CLASSES_PER_INVOCATION; i++) {
				toDecompile.add(iterator.previous());
				iterator.remove();
			}
			return copy;
		});
		pool.submit(() -> {
			try {
				if (!toDecompile.isEmpty()) {
					executeCFR(toDecompile);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			} finally {
				synchronized (CFRTalker.this) {
					if (toDecompile.size() < CLASSES_PER_INVOCATION) {
						sendFinishMessage();
					} else {
						newTask();
					}
				}
			}
		});
		return true;
	}

	public void executeCFR(List<String> classes) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String[] args = baseArgs.clone();
		args[1] = classes.stream().map(Pattern::quote).collect(Collectors.joining("|", "^(", ")$"));
		cfrMain.invoke(null, (Object) args);
	}

	public void sendFinishMessage() {
		System.out.println("[CFRTalker] Taskpool: done");
	}

	public void stop() throws InterruptedException {
		synchronized (this) {
			stopping = true;
		}
		pool.shutdown();
		pool.awaitTermination(1, TimeUnit.MINUTES);
	}

	public static void main(String[] args) throws InterruptedException {
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
		List<String> classes = Arrays.asList(args);
		CFRTalker main = new CFRTalker();
		boolean started = false;
		System.out.println("[CFRTalker] Taskpool: initized");
		try (Scanner scan = new Scanner(System.in)) {
			mainLoop:
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String[] split = line.split("\0");
				switch (split[0]) {
					case "start":
						System.out.println("[CFRTalker] Taskpool: started");
						if (!started) {
							started = true;
							main.decompiling.set(classes);
							main.start();
						}
						break;
					case "pause":
						System.out.println("[CFRTalker] Taskpool: paused");
						if (started) {
							started = false;
							main.stop();
						}
						break;
					case "exit":
						System.out.println("[CFRTalker] Taskpool: exited");
						if (started) {
							main.stop();
						}
						break mainLoop;
					case "classes":
						System.out.println("[CFRTalker] Taskpool: classes: " + Arrays.toString(split));
						classes = Arrays.stream(split, 1, split.length).collect(Collectors.toList());
						break;
					case "options":
						System.out.println("[CFRTalker] Taskpool: options: " + split.length);
						if (started) {
							main.stop();
						}
						String[] newArgs = new String[split.length + 1];
						newArgs[0] = "--jarfilter";
						System.arraycopy(split, 1, newArgs, 2, split.length - 1);
						main.baseArgs = newArgs;
						main.decompiling.set(new ArrayList<>(classes));
						if (started) {
							main.start();
						}
						System.out.println("[CFRTalker] Taskpool: options-done");
						break;
					case "setPrio":
						System.out.println("[CFRTalker] Taskpool: setprio: " + Arrays.toString(split));
						Map<String, Integer> prio = new HashMap<>(split.length - 1);
						for (int i = 1; i < split.length; i++) {
							String[] argsplit = split[i].split(":", 2);
							prio.put(argsplit[1], Integer.parseInt(argsplit[0]));
						}
						Integer d = 0;
						main.decompiling.getAndUpdate(s -> {
							String[] list = s.toArray(new String[0]);
							Arrays.sort(list, Comparator.comparing(c -> prio.getOrDefault(c, d)));
							return Arrays.asList(list);
						});
						break;
					default:
						System.err.println("Unknown cmd: " + line);
						System.exit(2);
				}
			}
		}
	}
}
