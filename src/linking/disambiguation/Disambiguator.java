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

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import linking.disambiguation.scorers.ContinuousHillClimbingPicker;
import linking.disambiguation.scorers.GraphWalkEmbeddingScorer;
import linking.disambiguation.scorers.PageRankScorer;
import linking.disambiguation.scorers.VicinityScorerDirectedSparseGraph;
import linking.disambiguation.scorers.embedhelp.CombineOperation;
import linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import linking.disambiguation.scorers.pagerank.PageRankLoader;
import structure.config.constants.Comparators;
import structure.config.constants.EnumEmbeddingMode;
import structure.config.constants.FilePaths;
import structure.config.constants.Numbers;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.PostScorer;
import structure.interfaces.Scorer;
import structure.utils.Loggable;
import structure.utils.Stopwatch;

public class Disambiguator implements Loggable {
	private final long sleeptime = 100l;
	private final HashSet<Mention> context = new HashSet<>();

	private final boolean IGNORE_DOUBLED_MENTIONS = true;
	private final EntitySimilarityService similarityService;
	private final Set<Scorer<PossibleAssignment>> scorers = new HashSet<>();
	private final Set<PostScorer<PossibleAssignment, Mention>> postScorers = new HashSet<>();
	// Determines how everything is scored!
	private final ScoreCombiner<PossibleAssignment> combiner = new ScoreCombiner<PossibleAssignment>();

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

		// Pre-Scoring
		// Add PR Scoring and get PRLoader for other scoring mechanisms
		final PageRankLoader pagerankLoader = setupPageRankScoring(KG);

		// Post-scoring
		// PossibleAssignment.addPostScorer(new VicinityScorer());

//		int displayCounter = 0;
//		for (Entry<String, List<Number>> e : entityEmbeddingsMap.entrySet()) {
//			System.out.println(e.getKey());
//			displayCounter++;
//			if (displayCounter > 50) {
//				System.out.println("printed 50");
//				break;
//			}
//		}

		addPostScorer(new VicinityScorerDirectedSparseGraph(KG));

		
		final boolean doEmbeddings = false;
		if (doEmbeddings) {
			this.similarityService = setupSimilarityService(KG, embeddingMode);
			addPostScorer(new GraphWalkEmbeddingScorer(new ContinuousHillClimbingPicker(
					combineOperation.combineOperation, similarityService, pagerankLoader)));
		} else {
			this.similarityService = null;
		}

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
		for (PossibleAssignment assgnmt : possAssignments) {
			// Multi thread here
			final Future<Integer> future = executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					final Number score = computeScore(assgnmt);
					assgnmt.setScore(score);
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

	/**
	 * Adds a scorer for disambiguation
	 * 
	 * @param scorer
	 */
	public void addScorer(@SuppressWarnings("rawtypes") final Scorer<PossibleAssignment> scorer) {
		scorers.add(scorer);
	}

	public void addPostScorer(@SuppressWarnings("rawtypes") final PostScorer<PossibleAssignment, Mention> scorer) {
		postScorers.add(scorer);
	}

	public Set<Scorer<PossibleAssignment>> getScorers() {
		return scorers;
	}

	public Set<PostScorer<PossibleAssignment, Mention>> getPostScorers() {
		return postScorers;
	}

	public ScoreCombiner<PossibleAssignment> getScoreCombiner() {
		return combiner;
	}

	private void clearContext() {
		this.context.clear();
	}

	/**
	 * Computes the score for this possible assignment
	 */
	public Number computeScore(final PossibleAssignment assgnmt) {
		Number currScore = null;
		// Goes through all the scorers that have been defined and combines them in the
		// wanted manner
		// Pre-scoring step
		for (@SuppressWarnings("rawtypes")
		Scorer<PossibleAssignment> scorer : getScorers()) {
			currScore = getScoreCombiner().combine(currScore, scorer, assgnmt);
		}
		// Post-scoring step
		for (@SuppressWarnings("rawtypes")
		PostScorer<PossibleAssignment, Mention> scorer : getPostScorers()) {
			currScore = getScoreCombiner().combine(currScore, scorer, assgnmt);
		}
		return currScore;
	}

	private void displaySimilarities(final EnumModelType KG, List<Mention> mentions) {
		if (this.similarityService == null) {
			System.err.println("No similarity service defined.");
			return;
		}
		// Get all similarities
		for (int i = 0; i < mentions.size(); ++i) {
			for (int j = i + 1; j < mentions.size(); ++j) {
				if (mentions.get(i).getMention().equals(mentions.get(j).getMention())) {
					continue;
				}
				List<String> targets = Lists.newArrayList();
				for (PossibleAssignment ass : mentions.get(j).getPossibleAssignments()) {
					targets.add(ass.getAssignment());
				}

				System.out.println("Mention:" + mentions.get(i) + "->" + mentions.get(j));
				for (PossibleAssignment ass : mentions.get(i).getPossibleAssignments()) {
					// Sorted similarities
					final List<Pair<String, Double>> similarities = this.similarityService.computeSortedSimilarities(
							ass.getAssignment(), targets, Comparators.pairRightComparator.reversed());
					System.out.println("Source:" + ass);
					System.out.println(similarities.subList(0, Math.min(5, similarities.size())));
				}
			}
		}
	}

}