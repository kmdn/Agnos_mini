package structure.datatypes;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import linking.disambiguation.ScoreCombiner;
import structure.interfaces.PostScorer;
import structure.interfaces.Scorable;
import structure.interfaces.Scorer;
import structure.utils.Loggable;

/**
 * A possible output candidate for entity linking - it can be scored, based on
 * the score it can be compared to another possible assignment with (assumingly)
 * the same mention
 * 
 * @author Kristian Noullet
 *
 */
public class PossibleAssignment implements Scorable, Comparable<PossibleAssignment>, Loggable {
	private static Logger logger = Logger.getLogger(PossibleAssignment.class);
	private Number score = Float.valueOf(0f);
	private final String assignment;
	private final String mentionToken;
	private boolean computedScore = false;
	private boolean warned = false;
	@SuppressWarnings("rawtypes")
	private static Set<Scorer<PossibleAssignment>> scorers = new HashSet<>();
	@SuppressWarnings("rawtypes")
	private static Set<PostScorer<PossibleAssignment, Mention>> postScorers = new HashSet<>();
	@SuppressWarnings("rawtypes")
	private static ScoreCombiner<PossibleAssignment> combiner = null;

	@SuppressWarnings("rawtypes")
	public static PossibleAssignment createNew(final String assignment, final String mentionToken) {
		// return new PossibleAssignment(new Resource(assignment, false).toN3(),
		// mentionToken);
		return new PossibleAssignment(assignment, mentionToken);
	}

	public PossibleAssignment(final String assignment, final String mentionToken) {
		this.assignment = assignment;
		this.mentionToken = mentionToken;
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
		PossibleAssignment.combiner = combiner;
	}

	@Override
	public int compareTo(final PossibleAssignment o) {
		return Double.compare(this.score.doubleValue(), o.score.doubleValue());
	}

	public String getAssignment() {
		return assignment;
	}

	/**
	 * Computes the score for this possible assignment
	 */
	@Override
	public Number computeScore() {
		Number currScore = null;
		// Goes through all the scorers that have been defined and combines them in the
		// wanted manner
		// Pre-scoring step
		for (@SuppressWarnings("rawtypes")
		Scorer<PossibleAssignment> scorer : scorers) {
			currScore = combiner.combine(currScore, scorer, this);
		}
		// Post-scoring step
		for (@SuppressWarnings("rawtypes")
		PostScorer<PossibleAssignment, Mention> scorer : postScorers) {
			currScore = combiner.combine(currScore, scorer, this);
		}
		this.score = currScore;
		computedScore = true;
		return currScore;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PossibleAssignment) {
			@SuppressWarnings("rawtypes")
			final PossibleAssignment ass = ((PossibleAssignment) obj);
			return this.score.equals(ass.score) && this.assignment.equals(ass.assignment);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return 23 * (this.assignment.hashCode() + this.score.hashCode() + this.mentionToken.hashCode());
	}

	@Override
	public String toString() {
		return getAssignment();
	}

	public Number getScore() {
		if (!computedScore && !warned) {
			// Warns only once per possible assignment
			logger.warn("Score has not yet been computed.");
			warned = true;
		}
		return this.score;
	}

	public String getMentionToken() {
		return mentionToken;
	}
}
