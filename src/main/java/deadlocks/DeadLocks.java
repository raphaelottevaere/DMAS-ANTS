package deadlocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import agents.AGVAgent;
import ants.DeadLockDetectionAnt;
import simulator.SimulationSettings;

public abstract class DeadLocks {

	public boolean active = true;
	protected ListenableGraph<LengthData> staticGraph;
	public int ID;
	public static int nextId = 0;

	protected Set<Point> nodes;
	protected Set<DeadLockedEdge> edges;
	public long timeLastTry = 0;
	Set<AGVAgent> toSendGraph = new HashSet<AGVAgent>();
	ListenableGraph<LengthData> dynamicGraph;

	public DeadLocks(DeadLockDetectionAnt ant, ListenableGraph<LengthData> graph, TimeLapse time) {
		try {
			this.ID = nextId;
			nextId++;
			this.edges = ant.getDeadLock();
			this.nodes = ant.getVisitedNodes();
			this.staticGraph = graph;
			this.timeLastTry = time.getTime();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// if the new deadlockant contains a single node that is also present in this
	// node the new deadlock is extension of thsi deadlock and can just be added to
	// this one
	final public boolean containsDeadlock(DeadLockDetectionAnt ant) {
		for (Point n : ant.getVisitedNodes()) {
			if (nodes.contains(n)) {
				return true;
			}
		}
		return false;
	}

	public void expandDeadLock(DeadLockDetectionAnt ant) {
		this.nodes.addAll(ant.getVisitedNodes());
		this.edges.addAll(ant.getDeadLock());
	}

	public void solveDeadlock(TimeLapse time) {

		ArrayList<Point> nodesList = new ArrayList<Point>();

		// TODO this can be moved for more performace
		// calculate in the beginning and recalculate everytime deadlock is expanded
		for (DeadLockedEdge e : edges) {
			if (e.getAgvAgent().getPosition().get().equals(e.from)) {
				nodesList.add(e.from);
				e.getAgvAgent().setWatitTime(SimulationSettings.RETRY_DEADLOCKS_SOLVER, time);
			}
		}

		Graph<LengthData> ng = new TableGraph<>();
		ng.addConnections(staticGraph.getConnections());

		dynamicGraph = new ListenableGraph<LengthData>(ng);

		// nodesList.sort(new connectionComparator(dynamicGraph));

		List<Edge> toRemove = new ArrayList<Edge>();
		for (Point p : nodesList) {
			Collection<Point> connections = dynamicGraph.getIncomingConnections(p);
			for (Point c : connections) {
				toRemove.add(new Edge(c, p));
			}
		}

		for (Edge e : toRemove) {
			if (e.from.equals(e.to)) {
				System.err.println("A edge with the same entrace and exit was added; " + e);
			} else
				dynamicGraph.removeConnection(e.from, e.to);
		}

		this.solve(time, dynamicGraph);
	}

	protected abstract void solve(TimeLapse time, ListenableGraph<LengthData> dynamicGraph);

	protected abstract boolean checkIfDeadlockStillActive();

	protected abstract void freeAgents();

	public void removeAgent(AGVAgent agvAgent) {
		DeadLockedEdge temp = null;
		for (DeadLockedEdge e : this.edges) {
			if (e.getAgvAgent().equals(agvAgent)) {
				temp = e;
			}
		}
		if (temp != null)
			this.edges.remove(temp);
	}

	public boolean containsAGV(AGVAgent agvAgent) {
		for (DeadLockedEdge e : this.edges) {
			if (e.getAgvAgent().equals(agvAgent)) {
				return true;
			}
		}
		return false;
	}

	protected void resetGraph(AGVAgent agvAgent) {
		agvAgent.staticGraph = this.staticGraph;
	}

	public void sendGraph(AGVAgent agvAgent, TimeLapse time) {
		agvAgent.selectShelter(time, dynamicGraph);
		// toSendGraph.add(agvAgent);
	}

	protected void sendGraphs(TimeLapse timeLapse) {
		for (AGVAgent e : toSendGraph) {
			e.selectShelter(timeLapse, dynamicGraph);
		}
		toSendGraph.clear();
	}

}
