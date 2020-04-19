package structure.linker;

import java.util.Collection;

import structure.datatypes.Mention;
import structure.interfaces.Weighable;

public interface Linker extends Weighable<Number> {
	
	public boolean init();

	public String annotate(final String input);

	public Collection<Mention> annotateMentions(final String input);
	
	public String getKG();
	
	@Override
	public int hashCode();
	
	@Override
	boolean equals(Object obj);
}
