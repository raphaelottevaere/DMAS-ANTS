package reservations;

import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.InfrastructureAgent;

public class RouteReservation extends TimeReservations {
	public Point exit;
	public Point node;
	public InfrastructureAgent responsibleNode ;
	public long maximumEndTime=0;

    public RouteReservation(AGVAgent agent, Point node,  long beginTime, long endTime, Point exit, InfrastructureAgent responsibleNode) {
    	super(beginTime,endTime, agent);
        this.exit =exit;
        this.node = node;
        this.responsibleNode = responsibleNode;
        this.maximumEndTime=Long.MAX_VALUE;
    }
    
    public RouteReservation(AGVAgent agent, Point node,  long beginTime, long endTime, Point exit, InfrastructureAgent responsibleNode, long maximumEndTime) {
    	super(beginTime,endTime, agent);
        this.exit =exit;
        this.node = node;
        this.responsibleNode = responsibleNode;
        this.maximumEndTime=maximumEndTime;
    }
	
	public String toString() {
		return "RouteReservation for AGV:" + agv.ID%1000 +"," + startTime + " - " + endTime  + "; on node "+ node;
	}
	
	public RouteReservation copy() {
        return new RouteReservation(agv, node,startTime, endTime, this.exit, responsibleNode,maximumEndTime);
	}
	
	@Override
	public boolean equals(Object o) {
		try {
			if(o == null)
				return false;
			if(!(o instanceof RouteReservation))
				return false;
			RouteReservation o1 = (RouteReservation) o;
			return o1.agv.equals(this.agv) && o1.startTime == this.startTime && o1.endTime == this.endTime && o1.responsibleNode.equals(this.responsibleNode);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
