package deadlocks;
import org.javatuples.Pair;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import agents.AGVAgent;
import ants.DeadLockDetectionAnt;
import simulator.SimulationSettings;

public class DeadLock_NonMover extends DeadLocks {

	protected Pair<AGVAgent, Point> nonMover;

	@Override
	protected boolean checkIfDeadlockStillActive() {
		if (nonMover == null)
			return false;
		return nonMover.getValue0().getPosition().get().equals(nonMover.getValue1());
	}

	public DeadLock_NonMover(DeadLockDetectionAnt ant, ListenableGraph<LengthData> graph, TimeLapse time) {
		super(ant, graph, time);
		this.nonMover = ant.getNonMover();

		// See if it is only 2 nodes
		// if it is only 2 nodes immediatly push for solving

		System.out.println("---------------DEADLOCK----------------");
		nonMover.getValue0().setWatitTime(SimulationSettings.RETRY_DEADLOCKS_SOLVER, time);
		System.out.println(nonMover.getValue0());
		for (DeadLockedEdge e : edges) {
			if (e.getAgvAgent().getPosition().get().equals(e.from)) {
				// e.getAgvAgent().setWatitTime(SimulationSettings.RETRY_DEADLOCKS_SOLVER,
				// time);
				System.out.println(e.getAgvAgent());
			}
		}
		System.out.println("---------------END----------------");

		this.solveDeadlock(time);
	}

	protected void solve(TimeLapse time, ListenableGraph<LengthData> graph) {

		// Sometimes we get AGV with deadlocks that only contain 1agv, is not possible
		Point p = this.nonMover.getValue1();

		if (!nonMover.getValue0().getPosition().get().equals(p)) {
			for (DeadLockedEdge e : edges) {
				e.getAgvAgent().waiting = false;
			}
			this.active = false;
			return;
		}

		long endTime;
		if (nodes.contains(p))
			endTime = nonMover.getValue0().getEndReservation(time);
		else
			endTime = 0;

		System.out.println(nonMover.getValue0());
		nonMover.getValue0().forcedMove = true;
		nonMover.getValue0().searchForRestingPlace(this.nodes, endTime, time, graph);

		/*
		 * Collection<Point> outgoing = this.staticGraph.getOutgoingConnections(p);
		 * Collection<Point> incoming = this.staticGraph.getIncomingConnections(p);
		 */
		// See if it is only 2 nodes
		// if it is only 2 nodes immediatly push for solving

		System.out.println("-----------SOLVING NON-MOVER DEADLOCK -----------");

		if (this.edges.isEmpty()) {
			System.out.println(nonMover.getValue0());
			nonMover.getValue0().forcedMove = true;
			nonMover.getValue0().searchForRestingPlace(this.nodes, endTime, time, graph);
			return;
		}

		System.out.println(nonMover.getValue0());
		nonMover.getValue0().forcedMove = true;
		nonMover.getValue0().searchForRestingPlace(this.nodes, endTime, time, graph);
		nonMover.getValue0().waiting = false;
		for (DeadLockedEdge e : edges) {
			if (!e.getAgvAgent().equals(nonMover.getValue0())) {
				System.out.println(e.getAgvAgent());
				e.getAgvAgent().forcedMove = true;
				e.getAgvAgent().searchForRestingPlace(this.nodes, endTime, time, graph);
				e.getAgvAgent().waiting = false;
			}
		}

		/*
		 * if (outgoing.containsAll(incoming) && outgoing.size() == incoming.size()) {
		 * System.out.println("----------- PROBLEM NODE -----------"); // All routes
		 * going out equals to all routes coming in // There is only 1 way to move in
		 * and out // THis means that the current agv can not move out without the other
		 * agv moving // out DeadLockedEdge problemNode = null; for (DeadLockedEdge e :
		 * edges) { System.out.println(e.getAgvAgent());
		 * 
		 * if (!e.getAgvAgent().equals(nonMover.getValue0())) { if (e.to.equals(p) &&
		 * e.getAgvAgent().getPosition().get().equals(e.from)) { problemNode = e; if
		 * (graph.getOutgoingConnections(e.from).size() != 0) {
		 * e.getAgvAgent().forcedMove = true;
		 * e.getAgvAgent().searchForRestingPlace(this.nodes, endTime, time, graph); } }
		 * else if (!e.getAgvAgent().finalPoint().isPresent() ||
		 * e.getAgvAgent().finalPoint().get().equals(this.nonMover.getValue1())) { if
		 * (graph.getOutgoingConnections(e.from).size() != 0) {
		 * e.getAgvAgent().forcedMove = true;
		 * e.getAgvAgent().searchForRestingPlace(this.nodes, endTime, time, graph); } }
		 * } }
		 * 
		 * // If the nonMover has no parcel, is not doingPickUp or going charging it is
		 * // doing nothing and should move out of the way
		 * //System.out.println(graph.getOutgoingConnections(nonMover.getValue1())); if
		 * (!nonMover.getValue0().hasParcel() && !nonMover.getValue0().doingPickUp &&
		 * !nonMover.getValue0().doingCharge &&
		 * graph.getOutgoingConnections(nonMover.getValue1()).size() != 0) { Set<Point>
		 * cN = new HashSet<Point>(); cN.addAll(this.nodes); if (problemNode != null)
		 * cN.remove(problemNode.from); nonMover.getValue0().forcedMove = true;
		 * nonMover.getValue0().searchForRestingPlace(cN, endTime, time, graph);
		 * 
		 * } } else { System.out.println(nonMover.getValue0());
		 * nonMover.getValue0().forcedMove=true;
		 * nonMover.getValue0().searchForRestingPlace(this.nodes, endTime, time, graph);
		 * for (DeadLockedEdge e : edges) { if
		 * (!e.getAgvAgent().equals(nonMover.getValue0())) { if
		 * ((!e.getAgvAgent().finalPoint().isPresent() &&
		 * (e.getAgvAgent().finalPoint().isPresent() &&
		 * !e.getAgvAgent().finalPoint().get().equals(this.nonMover.getValue1())))) { if
		 * (graph.getOutgoingConnections(e.from).size() != 0) {
		 * System.out.println(e.getAgvAgent()); e.getAgvAgent().forcedMove = true;
		 * e.getAgvAgent().searchForRestingPlace(this.nodes, endTime, time, graph); } }
		 * else { if (graph.getOutgoingConnections(e.from).size() != 0) {
		 * System.out.println(e.getAgvAgent()); e.getAgvAgent().forcedMove = true;
		 * e.getAgvAgent().intention = Optional.absent();
		 * e.getAgvAgent().lookForPath(time, true, dynamicGraph, false); } } } } }
		 */
		this.timeLastTry = time.getTime();

		System.out.println("-----------END SOLVING DEADLOCK -----------");
	}

	@Override
	protected void freeAgents() {
		for (DeadLockedEdge e : edges) {
			e.getAgvAgent().waiting = false;
			resetGraph(e.getAgvAgent());
		}
	}

	public void removeAgent(AGVAgent agvAgent) {
		if (nonMover == null || nonMover.getValue0() == null)
			return;
		if (nonMover.getValue0().equals(agvAgent)) {
			nonMover = null;
		} else
			super.removeAgent(agvAgent);
	}

	public boolean containsAGV(AGVAgent agvAgent) {
		if (nonMover != null && nonMover.getValue0().equals(agvAgent)) {
			return true;
		} else
			return super.containsAGV(agvAgent);
	}
}
