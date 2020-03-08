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

	/**
	 * Meant for more complex score modulation, e.g. via squaring, applying the sqrt
	 * or such.</br>
	 * 
	 * <b>Number1</b>: Score for this passed parameter / possible assignment</br>
	 * <b>T</b>: possibleAssignment (or whatever is used for that particular scorer)</br>
	 * <b>Number2</b>: Return value is a number...
	 * 
	 * @return a function that modulates the score dynamically, e.g. via squaring a
	 *         score
	 */
	public BiFunction<Number, T, Number> getScoreModulationFunction();
}
