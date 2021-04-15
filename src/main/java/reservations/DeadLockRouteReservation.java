package reservations;

import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.InfrastructureAgent;

public class DeadLockRouteReservation extends RouteReservation{

	public DeadLockRouteReservation(AGVAgent agent, Point node, long beginTime, long endTime, Point exit,
			InfrastructureAgent responsibleNode, long maximumEndTime) {
		super(agent, node, beginTime, endTime, exit, responsibleNode, maximumEndTime);
	}

	public DeadLockRouteReservation(RouteReservation newRR) {
		super(newRR.agv, newRR.node, newRR.getBeginTime(), newRR.endTime, newRR.exit, newRR.responsibleNode, newRR.maximumEndTime);
	}

	
	public String toString() {
		return "DEADLOCK for AGV:" + agv.ID%1000 +"," + startTime + " - " + endTime  + "; on node "+ node;
	}
	
}
