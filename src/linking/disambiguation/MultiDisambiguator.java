package linking.disambiguation;

import structure.datatypes.PossibleAssignment;

public interface MultiDisambiguator extends Disambiguator {
	public ScoreCombiner<PossibleAssignment> getScoreCombiner();

}
