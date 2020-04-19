package linking.disambiguation;

import java.util.Collection;

import structure.datatypes.Mention;

public interface Disambiguator {
	public void disambiguate(final Collection<Mention> mentions) throws InterruptedException;
}