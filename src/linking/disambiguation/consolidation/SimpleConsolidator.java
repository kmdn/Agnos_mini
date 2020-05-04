package linking.disambiguation.consolidation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.beust.jcommander.internal.Lists;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.linker.Linker;

public class SimpleConsolidator extends AbstractConsolidator {
	public SimpleConsolidator(final Linker... linkers) {
		super(linkers);
	}

	/**
	 * Combines different linkers' results for a particular KG together
	 * 
	 * @param KG knowledge graph for which it is being linked
	 * @return combined mentions
	 */
	public Map<Linker, Collection<Mention>> combine(final EnumModelType KG, final String input) {
		final Map<Linker, Collection<Mention>> mapLinkerResults = new HashMap<>();

		final List<Linker> linkerList = mapLinkers.get(KG.name());
		if (linkerList != null && linkerList.size() > 0) {
			// TODO: Multi-thread this part
			for (Linker linker : linkerList) {
				final Collection<Mention> mentions = linker.annotateMentions(input);
				mapLinkerResults.put(linker, mentions);
			}
		}
		return null;
	}

	@Override
	public Number combine(Linker... linkers) {
		// TODO Auto-generated method stub
		return null;
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
			final String key = makeKey(mention);
			leftMapMentions.put(key, mention);
			keys.add(key);
		}
		for (Mention mention : secondLinkerMentions) {
			final String key = makeKey(mention);
			rightMapMentions.put(makeKey(mention), mention);
			keys.add(key);
		}

		for (String key : keys) {
			final Mention leftMention = leftMapMentions.get(key);
			final Mention rightMention = leftMapMentions.get(key);
			if (leftMention != null && rightMention != null) {
				final Mention mergedMention = merge(leftMention, rightMention);
				mergedMentions.put(key, mergedMention);
			} else if (leftMention != null) {
				final Mention mention = new Mention(leftMention);
				mergedMentions.put(key, mention);
			} else if (rightMention != null) {
				final Mention mention = new Mention(rightMention);
				mergedMentions.put(key, mention);
			}
		}

		Collection<Mention> ret = Lists.newArrayList();
		for (Entry<String, Mention> e : mergedMentions.entrySet()) {
			e.getValue().assignBest();
			ret.add(e.getValue());
		}
		return ret;
	}

	private Mention merge(Mention leftMention, Mention rightMention) {

		final Collection<PossibleAssignment> leftAssignments = leftMention.getPossibleAssignments();
		final Collection<PossibleAssignment> rightAssignments = leftMention.getPossibleAssignments();
		final Map<String, PossibleAssignment> leftMapAssignments = new HashMap<>();
		final Map<String, PossibleAssignment> rightMapAssignments = new HashMap<>();
		final Collection<String> keys = new HashSet<>();
		for (PossibleAssignment assignment : leftAssignments) {
			final String key = makeKey(assignment);
			leftMapAssignments.put(key, assignment);
			keys.add(key);
		}
		for (PossibleAssignment assignment : rightAssignments) {
			final String key = makeKey(assignment);
			rightMapAssignments.put(makeKey(assignment), assignment);
			keys.add(key);
		}

		final Collection<PossibleAssignment> mergedAssignments = new ArrayList<>();

		// Merge assignments
		for (String key : keys) {
			final PossibleAssignment leftVal = leftMapAssignments.get(key);
			final PossibleAssignment rightVal = rightMapAssignments.get(key);
			final PossibleAssignment assignment;
			final String mentionWord, mentionOriginalMention, mentionOriginalWithoutStopwords;
			if (leftVal != null) {
				assignment = new PossibleAssignment(leftVal.getAssignment(), leftVal.getMentionToken(),
						leftVal.getScore().doubleValue() + (rightVal == null ? 0 : rightVal.getScore().doubleValue()));
			} else {
				assignment = new PossibleAssignment(rightVal.getAssignment(), rightVal.getMentionToken(),
						rightVal.getScore().doubleValue() + (leftVal == null ? 0 : leftVal.getScore().doubleValue()));
			}
			mergedAssignments.add(assignment);
		}

		return new Mention(leftMention.getMention(), mergedAssignments, leftMention.getOffset(),
				leftMention.getDetectionConfidence(), leftMention.getOriginalMention(),
				leftMention.getOriginalWithoutStopwords());
	}

	private String makeKey(PossibleAssignment assignment) {
		return assignment.getAssignment() == null ? "" : assignment.getAssignment();
	}

	private String makeKey(Mention linkerMention) {
		return linkerMention.getOffset() + linkerMention.getMention();
	}

}
