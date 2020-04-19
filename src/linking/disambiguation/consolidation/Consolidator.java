package linking.disambiguation.consolidation;

import structure.linker.Linker;

public interface Consolidator {
	
	public Number combine(Linker... linkers);
}
