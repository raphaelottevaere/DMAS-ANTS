package agents;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import ants.findRestingPlaceAnt;
import models.DARModel;

public class RestingAgent extends InfrastructureAgent {

	public RestingAgent(Point position, int nodeDistance, double agvSpeed, long tickLength, boolean verbose,
			ListenableGraph<LengthData> staticGraph, DARModel darModel, RandomGenerator rng) {
		super(position, nodeDistance, agvSpeed, tickLength, verbose, staticGraph, darModel, rng);
	}

	@Override
	public void handleRestingPlaceAnt(findRestingPlaceAnt rp, TimeLapse timeLapse) {
		if (!rp.getAccepted() && !rp.hasVisitedNode(this.position)) {
			if (this.routeReservations.isEmpty()) {
				rp.wasAccepted();
				rp.addVisitedNode(this.position);
				findRestingPlaceAnt tempAnt = rp.copy();
				tempAnt.acceptedPoint=this;
				tempAnt.setAccepted(true);
				tempAnt.isReturning = true;
				tempAnt.handleAnt((BasicAgent) tempAnt.getAgent(), timeLapse);
			} else {
				super.handleRestingPlaceAnt(rp, timeLapse);
			}
		}
	}

	@Override
	public boolean canRestHere(AGVAgent agv) {
		return this.routeReservations.isEmpty() || this.routeReservations.get(0).agv.equals(agv);
	}

	public boolean canSearchHere(AGVAgent agv) {
		return this.canRestHere(agv);
	}
	
}
