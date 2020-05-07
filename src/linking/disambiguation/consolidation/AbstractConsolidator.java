package linking.disambiguation.consolidation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import structure.datatypes.PossibleAssignment;
import structure.linker.Linker;
import structure.utils.FunctionUtils;
import structure.utils.Loggable;
import structure.utils.MentionUtils;

public abstract class AbstractConsolidator implements Consolidator, Loggable {

	private int keyCounter = 0;

	protected final Linker[] linkers;

	protected Map<String, List<Linker>> mapLinkers = new HashMap<>();

	public abstract Number combineScore(final PossibleAssignment leftAssignment,
			final PossibleAssignment rightAssignment);

	private final long sleeptime = 10l;

	/**
	 * Constructor splitting given linkers by their defined KGs
	 * 
	 * @param linkers linkers to execute for annotation
	 */
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

	/**
	 * Execute all linkers and return map with the mentions (separated) by linker.
	 * 
	 * @param input input text to annotate
	 * @return map with the mentions separated by linker
	 * @throws InterruptedException
	 */
	public Map<Linker, Collection<? extends Mention>> executeLinkers(final String input) throws InterruptedException {
		final Map<Linker, Collection<? extends Mention>> mapLinkerResults = new HashMap<>();
		//
		final AtomicInteger doneCounter = new AtomicInteger(0);
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(Numbers.SCORER_THREAD_AMT.val.intValue());
		// Foreach linker, we start the process of annotation
		for (Linker linker : linkers) {

			// Multi thread here
			final Future<Integer> future = executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					// System.out.println("Linker Class Executed: "+linker.getClass());
					final Collection<Mention> mentions = linker.annotateMentions(input);
					synchronized (mapLinkerResults) {
						mapLinkerResults.put(linker, mentions);
					}
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
			if ((sleepCounter > 5_000) && ((sleepCounter % 5_000) <= sleeptime)) {
				getLogger().debug("Finished Linkers[" + mapLinkerResults.keySet() + "]");
			}
		} while (!executor.isTerminated());
		final boolean terminated = executor.awaitTermination(200L, TimeUnit.MINUTES);
		if (!terminated) {
			throw new RuntimeException("Could not compute score in time.");
		}

		return mapLinkerResults;
	}

	/**
	 * Merges all mentions together with linker-specific scoring, regardless of
	 * which KG they are computed for
	 * 
	 * @param mapLinkerMention
	 * @return
	 */
	public Collection<? extends Mention> mergeAll(Map<Linker, Collection<Mention>> mapLinkerMention) {
		return mergeAll(mapLinkerMention, true);
	}

	public Collection<? extends Mention> mergeAll(Map<Linker, Collection<Mention>> mapLinkerMention,
			final boolean linkerSpecificModulation) {
		Collection<? extends Mention> linkerMentions = Lists.newArrayList();
		for (Map.Entry<Linker, Collection<Mention>> e : mapLinkerMention.entrySet()) {

			final Linker linker = e.getKey();
			final Collection<? extends Mention> otherLinkedMentions = MentionUtils.copyMentions(e.getValue());
			// Applies linker-specific modulations to the scores for consolidation
			if (linkerSpecificModulation) {
				// Do linker-specific modulation
				final Collection<? extends Mention> modulatedMentions = modulate(linker, otherLinkedMentions);
				linkerMentions = mergeMentions(linkerMentions, modulatedMentions);
			} else {
				// Do not apply linker-specific modulation
				linkerMentions = mergeMentions(linkerMentions, otherLinkedMentions);
			}
		}

		return linkerMentions;
	}

	/**
	 * Merges mentions by each knowledge graph
	 * 
	 * @param mapLinkerMention map with linkers and their disambiguated mentions
	 * @return map with (merged) mentions keyed by KG.name()
	 */
	public Map<String, Collection<? extends Mention>> mergeByKG(
			Map<Linker, Collection<? extends Mention>> mapLinkerMention) {
		final Map<String, Collection<? extends Mention>> mapMergedResults = new HashMap<>();
		// Go through the map with the linkers and their results
		for (Entry<Linker, Collection<? extends Mention>> e : mapLinkerMention.entrySet()) {
			final Collection<? extends Mention> linkerMentions;
			final Linker linker = e.getKey();
			final String keyKG = linker.getKG();
			final Collection<? extends Mention> otherLinkedMentions = MentionUtils.copyMentions(e.getValue());
			final Collection<? extends Mention> modulatedMentions = modulate(linker, otherLinkedMentions);
			if ((linkerMentions = mapMergedResults.get(keyKG)) != null) {
				// We already have mentions for this KG, so merge them
				final Collection<? extends Mention> mergedMentions = mergeMentions(linkerMentions, modulatedMentions);
				mapMergedResults.put(keyKG, mergedMentions);
			} else {
				// We don't have it yet, so just put the modulated version into the results map
				mapMergedResults.put(keyKG, modulatedMentions);
				// new HashSet<>()
				// e.getValue()
			}
		}
		// Then check which

		return mapMergedResults;
	}

	/**
	 * Applies passed linker's modulation to the passed mention's possible
	 * assignments' scores
	 * 
	 * @param linker   which modulation function and weight to apply
	 * @param mentions mentions to apply the logic on
	 * @return
	 */
	protected static Collection<? extends Mention> modulate(Linker linker, Collection<? extends Mention> mentions) {
		BiFunction<Number, Mention, Number> func = linker.getScoreModulationFunction();
		final Number weight = linker.getWeight();
//		final Number weight = linker.getWeight();
//		final BiFunction<Number, N, Number> func = linker.getScoreModulationFunction();
//		final Number modulatedVal = func == null ? nextScore : func.apply(nextScore, scorerParam).doubleValue();
//		return add(currScore, weight.doubleValue() * modulatedVal.doubleValue());

		if (func == null) {
			func = FunctionUtils::returnScore;
		}

		for (final Mention m : mentions) {
			for (final PossibleAssignment assignment : m.getPossibleAssignments()) {
				final Number oldScore = assignment.getScore();
				final double newScore = func.apply(oldScore, null).doubleValue();
				assignment.setScore(weight.doubleValue() * newScore);
			}
			m.assignBest();
		}
		return mentions;
	}

	/**
	 * Possible improvement: assume one is already aggregated to and take from the
	 * other, add to the first, but that makes the data structures more difficult to
	 * handle...
	 */
	public Collection<? extends Mention> mergeMentions(Collection<? extends Mention> firstLinkerMentions,
			Collection<? extends Mention> secondLinkerMentions) {
		final Map<String, Mention> mergedMentions = new HashMap<>();

		final Map<String, Mention> leftMapMentions = new HashMap<>();
		final Map<String, Mention> rightMapMentions = new HashMap<>();
		final Set<String> keys = new HashSet<>();
		for (Mention mention : firstLinkerMentions) {
			// Makes unique key based on which they will be grouped
			final String key = makeKey(mention);
			leftMapMentions.put(key, mention);
			keys.add(key);
		}
		for (Mention mention : secondLinkerMentions) {
			// Makes unique key based on which they will be grouped
			final String key = makeKey(mention);
			rightMapMentions.put(makeKey(mention), mention);
			keys.add(key);
		}

		// Now that we've collected all the relevant keys and sorted them to be accessed
		// with them, we iterate and merge accordingly
		for (String key : keys) {
			final Mention leftMention = leftMapMentions.get(key);
			final Mention rightMention = rightMapMentions.get(key);
			if (leftMention != null && rightMention != null) {
				// Neither is null, so merge them
				final Mention mergedMention = mergeMention(leftMention, rightMention);
				mergedMentions.put(key, mergedMention);
			} else if (leftMention != null) {
				// One of them is null, if it isn't leftMention, get data from it
				final Mention mention = new Mention(leftMention);
				mergedMentions.put(key, mention);
			} else if (rightMention != null) {
				// One of them is null, if it isn't rightMention, get data from it
				final Mention mention = new Mention(rightMention);
				mergedMentions.put(key, mention);
			}
		}

		Collection<Mention> ret = Lists.newArrayList();
		for (Entry<String, Mention> e : mergedMentions.entrySet()) {
			e.getValue().assignBest();

			System.out.println("[" + e.getKey() + "] Adding to ret[" + e.getValue() + "]");
			ret.add(e.getValue());
		}
		return ret;
	}

	/**
	 * Merges the assignments of passed two mentions. If you just want mentions with
	 * the same mention word to be merged, you will have to check it prior to
	 * calling this function
	 * 
	 * @param leftMention  one mention
	 * @param rightMention another mention
	 * @return a new merged mention containing merged possible assignment copies
	 */
	private Mention mergeMention(Mention leftMention, Mention rightMention) {

		final Collection<PossibleAssignment> leftAssignments = leftMention.getPossibleAssignments();
		final Collection<PossibleAssignment> rightAssignments = rightMention.getPossibleAssignments();
		final Map<String, PossibleAssignment> leftMapAssignments = new HashMap<>();
		final Map<String, PossibleAssignment> rightMapAssignments = new HashMap<>();
		final Collection<String> urlKeys = new HashSet<>();

		for (PossibleAssignment assignment : leftAssignments) {
			final String key = makeKey(assignment);
			leftMapAssignments.put(key, assignment);
			urlKeys.add(key);
		}

		for (PossibleAssignment assignment : rightAssignments) {
			final String key = makeKey(assignment);
			rightMapAssignments.put(key, assignment);
			urlKeys.add(key);
		}

		final Collection<PossibleAssignment> mergedAssignments = new ArrayList<>();

		// Merge assignments
		for (String urlKey : urlKeys) {
			final PossibleAssignment leftVal = leftMapAssignments.get(urlKey);
			final PossibleAssignment rightVal = rightMapAssignments.get(urlKey);
			final PossibleAssignment assignment;
			final String mentionWord, mentionOriginalMention, mentionOriginalWithoutStopwords;

			// TODO: Set here how they are to be combined - Need information on specified
			// linkers
			final Number scoreSum = combineScore(leftVal, rightVal);

			if (leftVal != null && leftVal.getAssignment() != null && leftVal.getAssignment().length() > 0) {
				assignment = new PossibleAssignment(leftVal.getAssignment(), // leftVal.getMentionToken(),
						scoreSum);
			} else if (rightVal != null && rightVal.getAssignment() != null && rightVal.getAssignment().length() > 0) {
				assignment = new PossibleAssignment(rightVal.getAssignment(), // rightVal.getMentionToken(),
						scoreSum);
			} else {
				// ERROR - Continue to next...
				continue;
			}
			mergedAssignments.add(assignment);
		}

		final Mention retMention = new Mention(leftMention.getMention(), mergedAssignments, leftMention.getOffset(),
				leftMention.getDetectionConfidence(), leftMention.getOriginalMention(),
				leftMention.getOriginalWithoutStopwords());
		return retMention;
	}

	/**
	 * Makes a key for an assignment
	 * 
	 * @param assignment assignment to transform into a String key
	 * @return key
	 */
	private String makeKey(PossibleAssignment assignment) {
		return assignment.getAssignment() == null ? ("key" + keyCounter++) : assignment.getAssignment();
	}

	/**
	 * Makes a key for a mention
	 * 
	 * @param linkerMention mention to transform into a String key
	 * @return key
	 */
	private String makeKey(Mention linkerMention) {
		return linkerMention.getOffset() + linkerMention.getMention();
	}

}
