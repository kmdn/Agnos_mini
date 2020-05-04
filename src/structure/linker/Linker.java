package structure.linker;

import java.util.Collection;

import structure.datatypes.Mention;
import structure.interfaces.Weighable;
import structure.utils.Loggable;

public interface Linker extends Weighable<Number>, Loggable {
	
	public boolean init();

	public String annotate(final String input);

	public Collection<Mention> annotateMentions(final String input);
	
	public String getKG();
	
	@Override
	public int hashCode();
	
	@Override
	boolean equals(Object obj);
}
