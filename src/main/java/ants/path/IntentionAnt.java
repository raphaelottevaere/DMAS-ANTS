package ants.path;

import java.util.List;

import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import reservations.RouteReservation;

public abstract class IntentionAnt extends PathAnt {
	
	//Point to which the intention is to move towards to do something there
	public Point endPoint;

	//An intention ant describes a basic intention of the agent
	//Here in this case the general intention that needs to be defined later on
	public IntentionAnt(List<Point> path, AGVAgent agent, Point endPoint, List<RouteReservation> r) {
		super(path, agent, r);
		this.endPoint=endPoint;
	}
	public IntentionAnt(List<Point> pathCopy,AGVAgent agent, Point endPoint,
			List<RouteReservation> reservationsCopy, int id) {
		super(pathCopy, agent, reservationsCopy, id);
		this.endPoint=endPoint;
	}
	
	public Point getEndPoint() {
		return endPoint;		
	}
	
	public boolean checkEndPoint(Point position) {
		return endPoint.equals(position);
	}
	
}
