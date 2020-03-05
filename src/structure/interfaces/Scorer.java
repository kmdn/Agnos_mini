package structure.interfaces;

import java.util.function.BiFunction;

/**
 * Interface for the type of class which can attribute a score based on an
 * assignment. Additionally has its own specific weight that can (and likely is)
 * used for combining its resulting scores with other scorers'
 * 
 * @author Kristian Noullet
 *
 * @param <T> type of assignment to be scored
 */
public interface Scorer<T> {
	public Number computeScore(T assignment);

	public Number getWeight();
	
	public BiFunction<Number, T, Number> getFunction();
}
