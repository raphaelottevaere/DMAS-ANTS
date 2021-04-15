package ants;

import java.util.HashSet;
import java.util.Set;

import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;

public abstract class BroadCastAnt extends BasicAnt {
	protected int TTL;
	protected Set<Point> constrainedNodes;
	protected Set<Point> visitedNodes= new HashSet<Point>();

	public BroadCastAnt(AGVAgent agent, Set<Point> disallowedNodes, int TTL) {
		super(agent);
		this.TTL=TTL;
		this.constrainedNodes= disallowedNodes;
	}
	
	protected BroadCastAnt(int pid, AGVAgent agent, Set<Point> ConstrainedNodes, int TTL) {
		super(pid, agent);
		this.TTL=TTL;
		this.constrainedNodes= ConstrainedNodes;
	}

	@Override
	public boolean canContinue() {
		return TTL>=0;
	}
	
	public boolean hasVisitedNode(Point p) {
		return this.visitedNodes.contains(p);
	}
	
	public boolean canContinue(Point p) {
		return !constrainedNodes.contains(p);
	}
	
	public void wasAccepted() {
		TTL-=1;
	}
	
	public void addVisitedNode(Point position) {
		this.visitedNodes.add(position);
	}

}
