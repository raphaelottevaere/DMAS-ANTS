package graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import simulator.SimulationSettings;
import simulator.SimulationException;

public class AStar {
	
	private static final AStar INSTANCE = new AStar();
	private Set<Point> points;

	public static AStar getInstance() {
		return INSTANCE;
	}
	
	public void setGraph(Graph<? extends ConnectionData> graph) {
		this.points = new HashSet<Point>(graph.getNodes());
	}
	
	public AStar() {}
	
	

	/**
	 * Making a shortcut to use the given SimulationSettings Alternative Paths
	 * exploration options, can be circumvented by using the other function
	 */
	public List<List<Point>> getAlternativePaths(Point start, Point dest, RandomGenerator rng,
			Graph<? extends ConnectionData> staticGraph) {
		return getAlternativePaths(SimulationSettings.ALTERNATIVE_PATHS_EXPLORATION, start, dest, rng, staticGraph);
	}

	/**
	 * Creates a List of potential paths using A* and a probabilistic penalty
	 * approach "Multi-agent route planning using delegate MAS." (2016) Dinh, van
	 * Lon and Holvoet
	 */
	public List<List<Point>> getAlternativePaths(int numberOfPaths, Point start, Point dest, RandomGenerator rng,
			Graph<? extends ConnectionData> staticGraph) {
		List<List<Point>> alternativePaths = new LinkedList<List<Point>>();

		int fails = 0;
		int maxFails = SimulationSettings.MAX_PATH_FAILS;
		double penalty = SimulationSettings.PENALTY_PATH_DISCOVERY;
		double alpha=0;
		try {
			alpha = rng.nextDouble();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		Table<Point, Point, Double> weights = HashBasedTable.create();
		start = new Point(roundToClosestEven(start.x),roundToClosestEven(start.y));

		while (alternativePaths.size() < numberOfPaths && fails < maxFails) {
			List<Point> path = getShortestPathWithAStar(staticGraph, weights, start, dest);
			if (path == null) {
				fails++;
				//We got a null path here, this is because no path exists
				if(SimulationSettings.INFRASTRUCTURE_VERBOSE) {
				new SimulationException(
						"Got a null path while searching for paths usign A*, this should not happen").printStackTrace();
				System.err.println("exits for this node: " + staticGraph.getOutgoingConnections(start));
				}
				return null;
			} else {
				if (alternativePaths.contains(path))
					fails++;
				else {
					alternativePaths.add(path);
					fails = 0;
				}
				for (Point usedPoints : path) {
					double beta = rng.nextDouble();
					if (beta < alpha) {
						for (Point weightedPoints : staticGraph.getIncomingConnections(usedPoints)) {
							if (weights.contains(usedPoints, weightedPoints))
								weights.put(usedPoints, weightedPoints,
										weights.get(usedPoints, weightedPoints) + penalty);
							else
								weights.put(usedPoints, weightedPoints, penalty);

							if (weights.contains(weightedPoints, usedPoints))
								weights.put(weightedPoints, usedPoints,
										weights.get(weightedPoints, usedPoints) + penalty);
							else
								weights.put(weightedPoints, usedPoints, penalty);
						}
					}
				}
			}
		}

		return alternativePaths;
	}

	/**
	 * Implementation of A* Should work as A* is generally described
	 */
	public List<Point> getShortestPathWithAStar(Graph<? extends ConnectionData> graph,
			Table<Point, Point, Double> weights, Point start, Point dest) {
		if (start.equals(dest)) {
			LinkedList<Point> list = new LinkedList<Point>();
			list.add(start);
			return list;
		}

		// Empty set
		Set<Point> closedSet = new HashSet<Point>();

		// Set of visited nodes
		Set<Point> openSet = new HashSet<Point>();

		// Make sure we work with even numbers, not all points are even numbers
		// We initialized the entire Graph working on even nodes (for communication with
		// commUsers)
		// But this might not be the case in every setup
		// Maybe this might need to be removed/changed -> not quite sure
		// start = new Point(roundToClosestEven(start.x), roundToClosestEven(start.y));

		openSet.add(start);

		HashMap<Point, Point> cameFrom = new HashMap<Point, Point>();

		HashMap<Point, Double> gScore = getGScore(start);

		HashMap<Point, Double> fScore = getFScore(start, dest, weights);

		while (!openSet.isEmpty()) {
			Point current = getLowestFScore(openSet, fScore);
			if (current.equals(dest))
				return reconstructPath(cameFrom, current);
			openSet.remove(current);
			closedSet.add(current);

			for (Point neighbour : graph.getOutgoingConnections(current)) {
				if (closedSet.contains(neighbour))
					continue;
				if (!openSet.contains(neighbour))
					openSet.add(neighbour);

				// The estimatedTime from current to a neighbour
				double totalgScore = gScore.get(current) + estimatedHeuristicCost(current, neighbour, weights);
				if (totalgScore >= gScore.get(neighbour))
					continue; // This is not a better path

				// This path is the best atm
				cameFrom.put(neighbour, current);
				gScore.put(neighbour, totalgScore);
				fScore.put(neighbour, gScore.get(neighbour) + estimatedHeuristicCost(neighbour, dest, weights));
			}
		}
		return null;
	}

	/*******************************
	 * Methods for facilitating A* *
	 *******************************/
	public double roundToClosestEven(double d) {
		return Math.round(d / 2) * 2;
	}

	private List<Point> reconstructPath(HashMap<Point, Point> cameFrom, Point current) {
		List<Point> path = new LinkedList<Point>();
		path.add(current);
		while (cameFrom.keySet().contains(current)) {
			current = cameFrom.get(current);
			path.add(current);
		}

		return new LinkedList<>(Lists.reverse(path));
	}

	private Point getLowestFScore(Set<Point> points, HashMap<Point, Double> fScore) {
		try {
		Point minPoint = null;
		double minVal = Double.MAX_VALUE;
		for (Point p : points) {
			double fVal = fScore.get(p);
			if (fVal < minVal) {
				minVal = fVal;
				minPoint = p;
			}
		}
		return minPoint;
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private HashMap<Point, Double> getGScore(Point start) {
		HashMap<Point, Double> gScore = new HashMap<Point, Double>();
		for (Point p :points) {
			if (p.equals(start))
				gScore.put(start, 0.0);
			else
				gScore.put(p, Double.MAX_VALUE);
		}
		return gScore;
	}

	private HashMap<Point, Double> getFScore(Point start, Point dest,
			Table<Point, Point, Double> weights) {
		HashMap<Point, Double> gScore = new HashMap<Point, Double>();
		for (Point p : points) {
			if (p.equals(start))
				gScore.put(start, estimatedHeuristicCost(start, dest, weights));
			else
				gScore.put(p, Double.MAX_VALUE);
		}
		return gScore;
	}

	private Double estimatedHeuristicCost(Point start, Point dest, Table<Point, Point, Double> weights) {
		if (weights.contains(start, dest))
			return distanceBetween(start, dest) + weights.get(start, dest);
		return distanceBetween(start, dest);
	}

	private static Double distanceBetween(Point current, Point neighbour) {
		return Math.abs(Point.distance(current, neighbour));
	}

	public List<Point> getAlternativePathsConcat(ArrayList<Point> dests, RandomGenerator rng,
			ListenableGraph<LengthData> staticGraph) {
		
		ArrayList<Point> concatPath = new ArrayList<Point>();
		for(int i = 0; i< dests.size()-1; i++) {
			List<List<Point>> path = getAlternativePaths(1, dests.get(i), dests.get(i+1), rng, staticGraph);
			if(path.isEmpty()) {
				//We didnt find a path so we just return an empty list
				return new ArrayList<Point>();
			}
	
			List<Point> newPath = path.get(0);
					
			if(!concatPath.isEmpty()) {
				//when concatPath is not empty we remove the last point if it is duplicated in both paths
				if(!newPath.isEmpty() && newPath.get(0).equals(concatPath.get(concatPath.size()-1))){
					newPath.remove(0);
				}
			}
			
			concatPath.addAll(newPath);

		}
		
		return concatPath;
	}
	
	public static Long calculateDistanceFromPath(List<Point> list) {
		long distance = 0;
		for(int i =0; i<list.size()-1; i++) {
			distance += Point.distance(list.get(i), list.get(i+1));
		}
		return distance;
	}
}