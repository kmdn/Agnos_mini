package linking.disambiguation.consolidation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import com.beust.jcommander.internal.Lists;

import structure.config.constants.Numbers;
import structure.datatypes.Mention;
import structure.interfaces.Weighable;
import structure.linker.Linker;
import structure.utils.Loggable;

public abstract class AbstractConsolidator implements Consolidator, Loggable {

	protected final Linker[] linkers;

	protected Map<String, List<Linker>> mapLinkers = new HashMap<>();

	public abstract Collection<MergeableMention> mergeMentions(Collection<MergeableMention> linkerMentions,
			Collection<Mention> value);

	private final long sleeptime = 10l;

	public AbstractConsolidator(final Linker... linkers) {
		this.linkers = linkers;
		if (linkers != null && linkers.length >= 0) {

			for (int i = 0; i < linkers.length; ++i) {
				final Linker linker = linkers[i];
				List<Linker> linkerList;
				if ((linkerList = mapLinkers.get(linker.getKG())) == null) {
					linkerList = Lists.newArrayList();
					mapLinkers.put(linker.getKG(), linkerList);
				}
				linkerList.add(linker);
			}

		}
	}

	public <N> Number combine(final Number currScore, final Number nextScore, final Weighable<N> linker,
			final N scorerParam) {
		// Add all types of scorers here with the appropriate weights
		// final Number score = scorer.computeScore(scorerParam);
		final Number weight = linker.getWeight();
		final BiFunction<Number, N, Number> func = linker.getScoreModulationFunction();
		final Number modulatedVal = func == null ? nextScore : func.apply(nextScore, scorerParam).doubleValue();
		return add(currScore, weight.doubleValue() * modulatedVal.doubleValue());
	}

	/**
	 * Transforms both numbers to double and adds them together.<br>
	 * <b>If currScore is NULL, it is treated as 0.</b>
	 * 
	 * @param currScore
	 * @param score
	 * @return
	 */
	private Number add(Number currScore, Number score) {
		return currScore == null ? score.doubleValue() : currScore.doubleValue() + score.doubleValue();
	}

	/**
	 * Execute all linkers and return map with the mentions separated by linker
	 * 
	 * @param input input text to annotate
	 * @return map with the mentions separated by linker
	 * @throws InterruptedException
	 */
	public Map<Linker, Collection<Mention>> executeLinkers(final String input) throws InterruptedException {
		final Map<Linker, Collection<Mention>> mapLinkerResults = new HashMap<>();
		//
		final AtomicInteger doneCounter = new AtomicInteger(0);
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(Numbers.SCORER_THREAD_AMT.val.intValue());
		//
		for (Linker linker : linkers) {

			// Multi thread here
			final Future<Integer> future = executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					final Collection<Mention> mentions = linker.annotateMentions(input);
					mapLinkerResults.put(linker, mentions);
					return doneCounter.incrementAndGet();
				}
			});

		}

		executor.shutdown();
		long sleepCounter = 0l;
		do {
			// No need for await termination as this is pretty much it already...
			Thread.sleep(sleeptime);
			sleepCounter += sleeptime;
			if ((sleepCounter > 5_000) && ((sleepCounter % 5000) <= sleeptime)) {
				getLogger().debug("Finished Linkers[" + mapLinkerResults.keySet() + "]");
			}
		} while (!executor.isTerminated());
		final boolean terminated = executor.awaitTermination(200L, TimeUnit.MINUTES);
		if (!terminated) {
			throw new RuntimeException("Could not compute score in time.");
		}
		return mapLinkerResults;
	}

	public Collection<Mention> mergeAll(Map<Linker, Collection<Mention>> mapLinkerMention) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Merges mentions by each knowledge graph
	 * 
	 * @param mapLinkerMention map with linkers and their disambiguated mentions
	 * @return map with (merged) mentions keyed by KG.name()
	 */
	public Map<String, Collection<Mention>> mergeByKG(Map<Linker, Collection<Mention>> mapLinkerMention) {
		final Map<String, Collection<MergeableMention>> mapMergedResults = new HashMap<>();
		// Go through the map with the linkers and their results
		for (Entry<Linker, Collection<Mention>> e : mapLinkerMention.entrySet()) {
			final Collection<MergeableMention> linkerMentions;
			final String keyKG = e.getKey().getKG();
			if ((linkerMentions = mapMergedResults.get(keyKG)) != null) {
				// We already have mentions for this KG, so merge them
				final Collection<MergeableMention> mergedMentions = mergeMentions(linkerMentions, e.getValue());
				mapMergedResults.put(keyKG, mergedMentions);
			} else {
				// We don't have it yet, so just put it into the results map
				mapMergedResults.put(keyKG, new HashSet<>());
			}
		}
		// Then check which

		return null;
	}
}
