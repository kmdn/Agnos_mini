package linking.disambiguation.consolidation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;

public class MergeableMention extends Mention implements Mergeable<Mention> {
	private Map<PossibleAssignment, MergeablePossibleAssignment> possibleAssignments = new HashMap<>();

	public MergeableMention(String word, PossibleAssignment assignment, int offset) {
		super(word, assignment, offset);
	}

	public MergeableMention(String word, PossibleAssignment assignment, int offset, double detectionConfidence,
			String originalMention, String originalWithoutStopwords) {
		super(word, assignment, offset, detectionConfidence, originalMention, originalWithoutStopwords);
	}

	/**
	 * Copies a given mention (replacing its equals(Object) method for set storage
	 * adaptation), creating copies of assignments as well
	 * 
	 * @param m mention to copy
	 * @return a copy of passed mention and possible assignments each w/ a modified
	 *         equals(Object) method
	 */
	public MergeableMention(Mention m) {
		this(m.getMention(), m.getAssignment(), m.getOffset(), m.getDetectionConfidence(), m.getOriginalMention(),
				m.getOriginalWithoutStopwords());
		updatePossibleAssignments(m.getPossibleAssignments());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Mention) {
			final Mention mentionObj = (Mention) obj;
			return getMention().equals(mentionObj.getMention()) && getOffset() == mentionObj.getOffset();
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		// If all constituents are null, make it a weird sum, so there is no collision
		// with anything else
		return ((this.getMention() == null) ? 4 : (this.getMention().hashCode())) + getOffset();
	}

	@Override
	public void updatePossibleAssignments(Collection<PossibleAssignment> possibleAssignments) {
		for (PossibleAssignment possAss : possibleAssignments) {
			final MergeablePossibleAssignment copyAssignment = new MergeablePossibleAssignment(possAss.getAssignment(),
					possAss.getMentionToken());
			copyAssignment.setScore(possAss.getScore());
			this.possibleAssignments.put(copyAssignment, copyAssignment);
		}
	}

	@Override
	public Collection<PossibleAssignment> getPossibleAssignments() {
		return this.possibleAssignments.keySet();
	}

	@Override
	public void merge(Mention otherMention) {
		merge(new MergeableMention(otherMention));
	}

	/**
	 * Merge otherMention into this (mergeable) mention
	 * 
	 * @param otherMention
	 */
	public void merge(MergeableMention otherMention) {
		// Mention w/ same word at same offset
		// So: Combine them
		final Map<PossibleAssignment, MergeablePossibleAssignment> toAddAssignments = otherMention.possibleAssignments;

		// Go through every assignment we have to add
		for (Entry<PossibleAssignment, MergeablePossibleAssignment> e : toAddAssignments.entrySet()) {
			final MergeablePossibleAssignment existingAssignment;
			if ((existingAssignment = this.possibleAssignments.get(e.getValue())) != null) {
				// This Assignment already exists in ours, so combine them
				existingAssignment
						.setScore(existingAssignment.getScore().doubleValue() + e.getValue().getScore().doubleValue());
			} else {
				// doesn't exist yet, so add it!
				this.possibleAssignments.put(e.getValue(), e.getValue());
			}
		}

	}

}
