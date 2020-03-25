package structure.datatypes;

import org.apache.log4j.Logger;

import linking.disambiguation.Disambiguator;
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
	public static PossibleAssignment createNew(final String assignment, final String mentionToken) {
		// return new PossibleAssignment(new Resource(assignment, false).toN3(),
		// mentionToken);
		return new PossibleAssignment(assignment, mentionToken);
	}

	public PossibleAssignment(final String assignment, final String mentionToken) {
		this.assignment = assignment;
		this.mentionToken = mentionToken;
	}

	@Override
	public int compareTo(final PossibleAssignment o) {
		return Double.compare(this.score.doubleValue(), o.score.doubleValue());
	}

	public String getAssignment() {
		return assignment;
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
	
	public void setScore(final Number score)
	{
		this.score = score;
		computedScore = true;
	}
}
