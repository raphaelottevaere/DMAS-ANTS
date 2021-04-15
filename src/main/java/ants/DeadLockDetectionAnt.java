package ants;

import java.util.HashSet;
import java.util.Set;

import org.javatuples.Pair;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.BasicAgent;
import ants.path.PathAnt;
import deadlocks.DeadLockedEdge;
import deadlocks.DeadlockSolver;

public class DeadLockDetectionAnt extends BasicAnt {
	
	private final Set<DeadLockedEdge> LockedEdges = new HashSet<DeadLockedEdge>();
	private final Set<Point> visitedNodes = new HashSet<Point>();
	private Point lastNode = null;
	private Pair<AGVAgent, Point> nonMover = null;
	private DeadlockSolver dl;	

	public DeadLockDetectionAnt(AGVAgent agent, DeadlockSolver dl1) {
		super(agent);
		this.dl = dl1;
	}

	@Override
	public PathAnt copy() {
		return null;
	}

	@Override
	public void handleAnt(BasicAgent cm, TimeLapse timeLapse) {
		cm.handleDeadLockDetectionAnt(this, timeLapse);
	}

	@Override
	public boolean canContinue() {
		return nonMover==null || lastNode == null;
	}
	
	public boolean addEdge(DeadLockedEdge e) {
		return this.LockedEdges.add(e);
	}
	
	public boolean addNode(Point p) {
		return this.visitedNodes.add(p);
	}
	
	public boolean hasVisited(Point p) {
		return this.visitedNodes.contains(p);
	}

	public void setLastNode(Point position, TimeLapse time) {
		this.lastNode  = position;
		dl.addDeadlock(this, time);
	}
	
	public Point getLastNode() {
		return this.lastNode;
	}

	public void setNonMover(AGVAgent agvAgent, TimeLapse time) {
		this.nonMover  = new Pair<AGVAgent, Point>(agvAgent, agvAgent.getPosition().get());
		if(!this.LockedEdges.isEmpty())
			dl.addDeadlock(this, time);
	}
	
	public Pair<AGVAgent, Point> getNonMover() {
		return nonMover;
	}

	public Set<DeadLockedEdge> getDeadLock() {
		return LockedEdges;
	}
	
	public Set<Point> getVisitedNodes() {
		return visitedNodes;
	}
	
}
