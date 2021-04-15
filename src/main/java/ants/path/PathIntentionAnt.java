package ants.path;

import java.util.List;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.BasicAgent;
import reservations.RouteReservation;
import reservations.TaskOptions;
import simulator.SimulationException;

public class PathIntentionAnt extends IntentionAnt {

	TaskOptions taskOptions=null;
	public PathIntentionAnt(List<Point> path, AGVAgent agent, Point endPoint,List<RouteReservation> r) {
		super(path, agent,endPoint, r);
		this.accepted=true;
	}
	
	public PathIntentionAnt(AGVAgent agent,  Point endPoint) {
		super(null, agent, endPoint,null);
		this.accepted=true;
	}
	
	public PathIntentionAnt(List<Point> path, AGVAgent agent, Point endPoint,List<RouteReservation> r, int id) {
		super(path, agent, endPoint, r, id);
		this.accepted=true;
	}

	@Override
	public PathAnt copy() {
		PathIntentionAnt ant = new PathIntentionAnt(pathCopy(),agent, endPoint,reservationsCopy(), id);
		ant.sendBy=this.sendBy;
		ant.isReturning=this.isReturning;
		return ant;
	}
	
	TaskOptions getTaskOptions() {
		return this.taskOptions;
	}
	
	@Override
	public void handleAnt(BasicAgent cm, TimeLapse timeLapse) {
		cm.handlePathIntentionAnt(this, timeLapse);
	}

	public boolean checkGoal(Point position) {
		return path.get(path.size()-1).equals(position);
	}

	public void AddPointToPath(Point p) {
		System.err.println(new SimulationException("Cant add a point to the path of PathIntentionAnt"));
	}
	
}
