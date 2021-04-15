package ants.path;

import java.util.List;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.BasicAgent;
import reservations.RouteReservation;
import reservations.TaskOptions;
import simulator.SimulationException;

public class TaskIntentionAnt extends IntentionAnt {
	
	private List<TaskOptions> taskOptions;

	//An intention ant describes a basic intention of the agent
	//Here the intention if to find a chargingStation
	public TaskIntentionAnt(List<Point> path, AGVAgent agent,  Point endpoint, List<RouteReservation> r, List<TaskOptions> list) {
		super(path, agent, endpoint, r);
		if(list==null) {
			new SimulationException("A Task intention ant should have taskoptions").printStackTrace();
		}
		this.taskOptions= list;
	}

	public TaskIntentionAnt(List<Point> pathCopy, AGVAgent agent,  Point endPoint,
			List<RouteReservation> reservationsCopy, List<TaskOptions> taskOptions2, int id) {
		super(pathCopy, agent,  endPoint, reservationsCopy,id);
		this.taskOptions= taskOptions2;
	}

	@Override
	public PathAnt copy() {
		TaskIntentionAnt ant = new TaskIntentionAnt(pathCopy(),agent,endPoint,reservationsCopy(), taskOptions, id);
		ant.isReturning=this.isReturning;
		ant.sendBy=this.sendBy;
		return ant;
	}
	
	public boolean getAccepted() {
		return accepted;
	}
	
	public void setAccepted(boolean b) {
		this.accepted=b;
	}
	 
	public List<TaskOptions> getTaskOptions() {
		return this.taskOptions;
	}

	@Override
	public void handleAnt(BasicAgent cm, TimeLapse timeLapse) {
		cm.handleTaskIntentionAnt(this, timeLapse);
	}
	
	@Override
	public String toString() {
		return "TaskIntentionAnt, " + this.endPoint + "; " + this.taskOptions.toString();
	}
	
}