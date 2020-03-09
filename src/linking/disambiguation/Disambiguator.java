package linking.disambiguation;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;

import linking.disambiguation.scorers.ContinuousHillClimbingPicker;
import linking.disambiguation.scorers.GraphWalkEmbeddingScorer;
import linking.disambiguation.scorers.PageRankScorer;
import linking.disambiguation.scorers.embedhelp.CombineOperation;
import linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import preprocessing.loader.PageRankLoader;
import structure.config.constants.EnumEmbeddingMode;
import structure.config.constants.FilePaths;
import structure.config.constants.Numbers;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.PostScorer;
import structure.interfaces.Scorable;
import structure.interfaces.Scorer;
import structure.utils.Loggable;
import structure.utils.Stopwatch;

public class Disambiguator implements Loggable {
	private final long sleeptime = 100l;
	private final HashSet<Mention> context = new HashSet<>();
	private final EntitySimilarityService similarityService;

	private final boolean IGNORE_DOUBLED_MENTIONS = true;

	@SuppressWarnings("rawtypes")
	private static final Set<Scorer<PossibleAssignment>> scorers = new HashSet<>();
	@SuppressWarnings("rawtypes")
	private static final Set<PostScorer<PossibleAssignment, Mention>> postScorers = new HashSet<>();
	@SuppressWarnings("rawtypes")
	private static ScoreCombiner<PossibleAssignment> combiner = null;

	/**
	 * Default setting constructor for a defined knowledge graph
	 * 
	 * @param KG
	 * @throws IOException
	 */
	public Disambiguator(final EnumModelType KG) throws IOException {
		this(KG, EnumEmbeddingMode.DEFAULT);
	}

	public Disambiguator(final EnumModelType KG, final EnumEmbeddingMode embeddingMode) throws IOException {
		final CombineOperation combineOperation = CombineOperation.MAX_SIM;

		// Determines how everything is scored!
		setScoreCombiner(new ScoreCombiner<PossibleAssignment>());

		// Pre-Scoring
		// Add PR Scoring and get PRLoader for other scoring mechanisms
		final PageRankLoader pagerankLoader = setupPageRankScoring(KG);

		// Post-scoring
		// PossibleAssignment.addPostScorer(new VicinityScorer());

		this.similarityService = setupSimilarityService(KG, embeddingMode);
//		int displayCounter = 0;
//		for (Entry<String, List<Number>> e : entityEmbeddingsMap.entrySet()) {
//			System.out.println(e.getKey());
//			displayCounter++;
//			if (displayCounter > 50) {
//				System.out.println("printed 50");
//				break;
//			}
//		}

		addPostScorer(new GraphWalkEmbeddingScorer(new ContinuousHillClimbingPicker(
				// combineOperation.combineOperation,
				similarityService, pagerankLoader)));

		for (PostScorer postScorer : getPostScorers()) {
			// Links a context object which will be updated when necessary through
			// updateContext(Collection<Mention<N>>)
			postScorer.linkContext(context);
		}
	}

	private EntitySimilarityService setupSimilarityService(EnumModelType KG, final EnumEmbeddingMode embeddingMode)
			throws IOException {
		final Map<String, List<Number>> entityEmbeddingsMap;
		if (embeddingMode == EnumEmbeddingMode.LOCAL) {
			entityEmbeddingsMap = GraphWalkEmbeddingScorer.humanload(
					FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(KG),
					FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG));
			return new EntitySimilarityService(entityEmbeddingsMap);
		} else {
			return new EntitySimilarityService();
		}
	}

	/**
	 * Adds PR scorer and loads the PR scores
	 */
	private PageRankLoader setupPageRankScoring(final EnumModelType KG) throws IOException {
		// How to load pagerank
		final String pagerankWatch = "pagerank";
		Stopwatch.start(pagerankWatch);
		final PageRankLoader pagerankLoader = new PageRankLoader(KG);
		// Loads the pagerank from file
		pagerankLoader.exec();
		Stopwatch.endOutput(pagerankWatch);

		// Pre-scoring
		addScorer(new PageRankScorer(KG, pagerankLoader));
		return pagerankLoader;
	}

	public void disambiguate(final Collection<Mention> mentions) throws InterruptedException {
		disambiguate(mentions, IGNORE_DOUBLED_MENTIONS);
	}

	/**
	 * Disambiguate mentions and find the best possible assignment for each
	 * 
	 * @param mentions              list of mentions
	 * @param removeDoubledMentions
	 * @throws InterruptedException
	 */
	public void disambiguate(final Collection<Mention> mentions, final boolean removeDoubledMentions)
			throws InterruptedException {
		// Update contexts
		updatePostContext(mentions);

		// Start sending mentions to scorers
		if (removeDoubledMentions) {
			// In order to avoid disambiguating multiple times for the same mention word, we
			// split our mentions up and then just copy results from the ones that were
			// computed
			final Map<String, Collection<Mention>> mentionMap = new HashMap<>();
			// Split up the mentions by their keys
			for (final Mention mention : mentions) {
				Collection<Mention> val;
				if ((val = mentionMap.get(mention.getMention())) == null) {
					val = Lists.newArrayList();
					mentionMap.put(mention.getMention(), val);
				}
				val.add(mention);
			}
			for (final Map.Entry<String, Collection<Mention>> e : mentionMap.entrySet()) {
				// Just score the first one within the lists of doubled mentions
				final Collection<Mention> sameWordMentions = e.getValue();
				final Mention mention = sameWordMentions.iterator().next();// .get(0);
				score(mention);
				// Assign the top-scored possible assignment to the mention
				mention.assignBest();
				// Copy into the other mentions
				// for (int i = 1; i < e.getValue().size(); ++i) {
				final Iterator<Mention> itSameWordMentions = sameWordMentions.iterator();
				while (itSameWordMentions.hasNext()) {
					// Skip the first one as it's just time lost...
					final Mention sameWordMention = itSameWordMentions.next();
					sameWordMention.copyResults(mention);
				}
			}
		} else {
			for (Mention mention : mentions) {
				// Compute the score for this mention
				score(mention);
				// Assign the best possible assignment to this mention
				// Best assignment can be retrieved from mention object directly
				mention.assignBest();
			}
		}
		clearContext();
	}

	/**
	 * Calls the scorer appropriately and returns a list of the assignments
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	private Collection<PossibleAssignment> score(final Mention mention) throws InterruptedException {
		// Now score all of the assignments based on their own characteristics
		// and on the contextual ones
		Collection<PossibleAssignment> possAssignments = mention.getPossibleAssignments();
		final int assSize = possAssignments.size();
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(Numbers.SCORER_THREAD_AMT.val.intValue());
		final AtomicInteger doneCounter = new AtomicInteger(0);
		for (Scorable assgnmt : possAssignments) {
			// Multi thread here
			final Future<Integer> future = executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					assgnmt.computeScore();
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
				getLogger().debug(
						"Score Computation - In progress [" + doneCounter.get() + " / " + assSize + "] documents.");
			}
		} while (!executor.isTerminated());
		final boolean terminated = executor.awaitTermination(10L, TimeUnit.MINUTES);
		if (!terminated) {
			throw new RuntimeException("Could not compute score in time.");
		}
		return mention.getPossibleAssignments();
	}

	/**
	 * Updates the context for post-scorers (required for proper functioning)
	 * 
	 * @param mentions
	 */
	public void updatePostContext(Collection<Mention> mentions) {
		clearContext();
		this.context.addAll(mentions);
		for (PostScorer postScorer : getPostScorers()) {
			postScorer.updateContext();
		}
	}

	public EntitySimilarityService getSimilarityService() {
		return this.similarityService;
	}

	/**
	 * Adds a scorer for disambiguation
	 * 
	 * @param scorer
	 */
	public static void addScorer(@SuppressWarnings("rawtypes") final Scorer<PossibleAssignment> scorer) {
		scorers.add(scorer);
	}

	public static void addPostScorer(
			@SuppressWarnings("rawtypes") final PostScorer<PossibleAssignment, Mention> scorer) {
		postScorers.add(scorer);
	}

	public static Set<Scorer<PossibleAssignment>> getScorers() {
		return scorers;
	}

	public static Set<PostScorer<PossibleAssignment, Mention>> getPostScorers() {
		return postScorers;
	}

	public static void setScoreCombiner(
			@SuppressWarnings("rawtypes") final ScoreCombiner<PossibleAssignment> combiner) {
		Disambiguator.combiner = combiner;
	}

	public static ScoreCombiner<PossibleAssignment> getScoreCombiner() {
		return combiner;
	}

	private void clearContext() {
		this.context.clear();
	}

}