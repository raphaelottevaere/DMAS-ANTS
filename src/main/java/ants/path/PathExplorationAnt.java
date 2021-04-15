package ants.path;

import java.util.List;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.BasicAgent;
import reservations.RouteReservation;

public class PathExplorationAnt extends ExplorationAnt {
	
	Point goal;

	public PathExplorationAnt(List<Point> path,AGVAgent agent, List<RouteReservation> r, Point goal) {
		super(path, agent,r);
		this.goal = goal;
	}
	
	public PathExplorationAnt(List<Point> path,AGVAgent agent, Point goal) {
		super(path, agent, null);
		this.goal = goal;
	}
	
	public PathExplorationAnt(List<Point> path, AGVAgent agent, List<RouteReservation> r, Point goal, int id) {
		super(path, agent,  r, id);
		this.goal = goal;
	}

	@Override
	public PathExplorationAnt copy() {
		PathExplorationAnt ant =  new PathExplorationAnt(pathCopy(),agent,reservationsCopy(),goal,id);
		ant.isReturning=this.isReturning;
		ant.sendBy=this.sendBy;
		return ant;
	}
	
	public boolean checkGoal(Point p) {
		if (goal.equals(p))
			return true;
		return false;			
	}
	
	public Point getGoal() {
		return goal;		
	}
	
	@Override
	public void handleAnt(BasicAgent cm, TimeLapse timeLapse) {
		cm.handlePathExplorationAnt(this, timeLapse);
	}

	public Point endPoint() {
		return path.get(path.size()-1);
	}
	
	public String toString() {
		if(!this.isReturning)
			return "PathExplo to " +  this.getEndPoint().toString();
		return "PathExplo to " +  this.getEndPoint().toString()  + " arrival: "  + this.getLastReservation().startTime;
	}

	

}
