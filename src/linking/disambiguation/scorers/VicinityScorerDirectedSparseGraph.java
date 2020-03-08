package linking.disambiguation.scorers;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;

import structure.config.constants.Numbers;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.DirectedHoppableSparseGraph;
import structure.interfaces.HoppableGraph;
import structure.interfaces.PostScorer;

/**
 * Context-based scorer making use of an in-memory graph and node connectivities
 * 
 * @author Kristian Noullet
 *
 */
public class VicinityScorerDirectedSparseGraph implements PostScorer<PossibleAssignment, Mention> {
	private Logger logger = Logger.getLogger(getClass());
	private double sigma_ratio = Numbers.VICINITY_SCORING_WEIGHT_SIGMA.val.doubleValue();
	private Collection<Mention> context;
	private final HoppableGraph<String, String> graph;
	private final Set<String> goalNodesSet = new HashSet<>();

	public VicinityScorerDirectedSparseGraph() {
		// Initialize graph for path building
		this.graph = new DirectedHoppableSparseGraph<String, String>();
	}

	@Override
	public Number computeScore(final PossibleAssignment assignment) {
		double retScore = 0d;
		// We have an assignment and we check the neighborhood through the graph
		// Take the assignment's source as a source node
		final Collection<String> allNodes = graph.getVertices();

		// Get the ID for the source of this mention as a source node for the graph
		// traversal
		// Not completely sure whether nodeURL should be the assignment's URL or the
		// mention's, but since we are doing disambiguation for entity linking here, it
		// seems more likely that it should be from the assignment
		final String nodeURL = assignment.getAssignment().toString();
		final boolean vertexExists = this.graph.containsVertex(nodeURL);

		Collection<String> neighbors = this.graph.getNeighbors(nodeURL);

		if (!vertexExists) {
			logger.error("No node found for " + nodeURL);
			logger.debug("\t->Skipping path building for: Assignment(" + assignment + ") - " + nodeURL);
			return 0;
		}

		// Add mention's source as a 'starting'/'from' node
		if (sigma_ratio > 1) {
			logger.warn("Hop-Scoring decline weight(" + sigma_ratio + ") greater than 1 (negative score possible).");
		}

		final Deque<LinkedList<Integer>> paths;
		// logger.debug(nodeURL + "(" + startNodeID + ") - Found paths(" + paths.size()
		// + "):\n" + paths);
		final HashSet<String> from = new HashSet<>();
		from.add(nodeURL);
		dive(new HashSet<>(), from, 3);
		//Now do dives for all the possible goals
		
		//Then merge the results to see how well it performs
		
		
		return null;//pathworth(paths);
	}

//	private double pathworth(List<String> paths) {
//		double retScore = 0d;
//		for (String path : paths) {
//			final String[] pathNodes = path.trim().split(" ");
//			retScore += pathworth(pathNodes);
//		}
//		return retScore;
//	}

	/**
	 * Adds elements from the second passed list (in reverse indexed order) to the
	 * first one. Generally meant for path concatenation.
	 * 
	 * @param dest destination list
	 * @param src  source list (will be appended in reverse indexed order)
	 */
	private <E> void appendReverse(List<E> dest, List<E> src) {
		for (int i = src.size() - 1; i > 0; --i) {
			dest.add(src.get(i));
		}
	}

	/**
	 * TODO: Based on the chosen logic for the maps
	 * @param <E>
	 * @param map1
	 * @param map2
	 * @return
	 */
	private <E> Map<Integer, Collection<E>> mergeMaps(Map<Integer, Collection<E>> map1, Map<Integer, Collection<E>> map2)
	{
		final Set<Integer> depths = map1.keySet();
		for (Map.Entry<Integer, Collection<E>> e : map1.entrySet())
		{
			final Set<E> coll = new HashSet<>(e.getValue());
			for (Integer depth : depths)
			{
				final Collection<E> coll2 = map2.get(depth);
			}
		}
		return map2;
	}
	
	private <E> double pathworth(Collection<Collection<E>> paths) {
		// Initially a path is worth 100% of the usual worth
		double retScore = 0d;
		for (Collection path : paths) {

			//retScore += pathWorth;
		}
		return retScore;
	}

	private <E> double pathworth(Map<Integer, Collection<E>> map, final Set<E> from) {
		double retScore = 0d;
		for (Map.Entry<Integer, Collection<E>> e : map.entrySet()) {
			final double worth = pathworth(e.getKey());
			new HashSet<>(e.getValue());
			// For each path, the worth is added up...
			retScore += worth * e.getValue().size();
		}
		return retScore;
	}

	private double pathworth(int dist) {
		return pathworth(dist, 1d);
	}

	private double pathworth(int dist, double pathInit) {
		double pathWorth = pathInit;
		// ((1.0d) / ((double) (path.split(" ").length - 1)));
		for (double i = 0; i < dist - 2; ++i) {
			// Starts at index 1 due to the first node being the source node
			pathWorth -= (sigma_ratio * pathWorth);
		}
		return pathWorth;
	}

	/**
	 * Dives deeper into the graph to get the neighbours
	 * TODO
	 * @param visited
	 * @param from
	 * @param depthToDo
	 * @return
	 */
	private HashMap<Integer, Collection<String>> dive(final Set<String> visited, final Set<String> from,
			final int depthToDo) {
		if (depthToDo <= 0) {
			return new HashMap<>();
		}
		final HashMap<Integer, Collection<String>> retMap = new HashMap<Integer, Collection<String>>();
		Collection<String> lengthNeighbours = retMap.get(depthToDo);
		// None yet, so fill it up!
		if (lengthNeighbours == null) {
			lengthNeighbours = new HashSet<>();
			retMap.put(depthToDo, lengthNeighbours);
		}

		// One thread per "from" node
		for (String s : from) {
			lengthNeighbours.add(s);
			// Create a thread
			Set<String> neighbours = new HashSet<>(this.graph.getNeighbors(s));
			neighbours.removeAll(visited);
			visited.addAll(neighbours);
			final HashMap<Integer, Collection<String>> map = dive(visited, neighbours, depthToDo - 1);
			for (Map.Entry<Integer, Collection<String>> e : map.entrySet()) {
				// newColl cannot be null as we iterate over it
				final Collection<String> newColl = e.getValue();// map.get(e.getKey());
				Collection<String> retColl = retMap.get(e.getKey());
				if (retColl == null) {
					retColl = new HashSet<>();
					retMap.put(e.getKey(), retColl);
				}

				// Add the newly-found neighbours to the return map's appropriate collection
				retColl.addAll(newColl);
			}
		}
		return retMap;
	}

	/**
	 * Updates context based on linked context
	 */
	@Override
	public void updateContext() {
		goalNodesSet.clear();
		// Update what nodes can be used as goal nodes
		for (Mention contextMention : context) {
			for (PossibleAssignment possibleAssignment : contextMention.getPossibleAssignments()) {
				final String nodeURL = possibleAssignment.getAssignment().toString();
				if (nodeURL != null) {
					goalNodesSet.add(nodeURL);
				}
			}
		}
	}

	@Override
	public void linkContext(Collection<Mention> context) {
		this.context = context;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		return hashCode() == obj.hashCode();
	}

	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}

	@Override
	public Number getWeight() {
		return Numbers.VICINITY_WEIGHT.val;
	}

	@Override
	public BiFunction<Number, PossibleAssignment, Number> getScoreModulationFunction() {
		return null;
	}

}
