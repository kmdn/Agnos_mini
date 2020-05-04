package linking.disambiguation.consolidation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
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
	public Collection<MergeableMention> mergeMentions(Collection<MergeableMention> firstLinkerMentions,
			Collection<Mention> secondLinkerMentions) {

		final Map<MergeableMention, MergeableMention> mergedMentions = new HashMap<>();

		if (firstLinkerMentions != null && firstLinkerMentions.size() > 0) {
			for (Mention linkerMention : firstLinkerMentions) {
				final MergeableMention storedMention;
				if ((storedMention = mergedMentions.get(linkerMention)) != null) {
					storedMention.merge(linkerMention);
				} else {
					final MergeableMention copyMention = new MergeableMention(linkerMention);
					mergedMentions.put(copyMention, copyMention);
				}
			}
		}

		if (secondLinkerMentions != null && secondLinkerMentions.size() > 0) {
			for (Mention linkerMention : secondLinkerMentions) {
				final MergeableMention storedMention;
				if ((storedMention = mergedMentions.get(linkerMention)) != null) {
					storedMention.merge(linkerMention);
				} else {
					final MergeableMention copyMention = new MergeableMention(linkerMention);
					mergedMentions.put(copyMention, copyMention);
				}
			}
		}

		// Assigns the best to each...
		for (MergeableMention mention : mergedMentions.keySet()) {
			mention.assignBest();
		}
		return mergedMentions.keySet();
	}

}
