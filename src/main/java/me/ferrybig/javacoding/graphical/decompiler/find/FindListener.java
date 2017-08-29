/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.graphical.decompiler.find;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.IntConsumer;


public interface FindListener {

	public void onResult(List<SearchResult> results);

	public void done(Future<Void> future);

	public void totalCalculated(int total);

	public void madeProgress(int progress, String lastFile);

	public static FindListener of(
			Consumer<List<SearchResult>> results,
			Consumer<Future<Void>> finish,
			IntConsumer total,
			IntConsumer progress) {
		return new FindListener() {
			@Override
			public void done(Future<Void> future) {
				finish.accept(future);
			}

			@Override
			public void madeProgress(int progress0, String lastFile) {
				progress.accept(progress0);
			}

			@Override
			public void onResult(List<SearchResult> results0) {
				results.accept(results0);
			}

			@Override
			public void totalCalculated(int total0) {
				total.accept(total0);
			}
		};
	}

}
