package linking.disambiguation.consolidation;

public interface Mergeable<M> {
	public void merge(M toMerge);
}
