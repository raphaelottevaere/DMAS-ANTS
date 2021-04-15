package ants;

import java.util.Set;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.BasicAgent;
import agents.InfrastructureAgent;

public class findRestingPlaceAnt extends BroadCastAnt {
	
	private boolean accepted;
	public InfrastructureAgent acceptedPoint;

	public findRestingPlaceAnt(AGVAgent agent, Set<Point> disallowedNodes, int TTL) {
		super(agent, disallowedNodes, TTL);
	}

	private findRestingPlaceAnt(int pid, AGVAgent agent, Set<Point> constrainedNodes, int TTL) {
		super(pid, agent, constrainedNodes, TTL);
	}

	@Override
	public findRestingPlaceAnt copy() {
		findRestingPlaceAnt tempAnt = new findRestingPlaceAnt(id,agent,constrainedNodes, this.TTL);
		tempAnt.visitedNodes=this.visitedNodes;
		return tempAnt;
	}

	@Override
	public void handleAnt(BasicAgent cm, TimeLapse timeLapse) {
		cm.handleRestingPlaceAnt(this, timeLapse);
	}

	public void setAccepted(boolean b) {
		this.accepted= b;
	}
	
	public boolean getAccepted() {
		return this.accepted;
	}

	public Set<Point> getDisAllowedNodes() {
		return this.constrainedNodes;
	}
	
	public String toString() {
		return "RPANT for point : " + this.acceptedPoint;
	}

}
