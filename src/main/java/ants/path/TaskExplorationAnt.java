package ants.path;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.BasicAgent;
import reservations.RouteReservation;
import reservations.TaskOptions;
import simulator.SimulationSettings;

public class TaskExplorationAnt extends ExplorationAnt {

	private List<TaskOptions> taskOptions = new ArrayList<TaskOptions>();
	private int highestPriotity = -1;
	public boolean holdingTask = false;

	public TaskExplorationAnt(List<Point> path, AGVAgent agent, List<RouteReservation> r) {
		super(path, agent, r);
	}
	
	public TaskExplorationAnt(List<Point> path, AGVAgent agent, List<RouteReservation> r, int id) {
		super(path, agent, r, id);
	}

	public TaskExplorationAnt(List<Point> path,AGVAgent agent) {
		super(path, agent,null);
	}
	
	@Override
	public TaskExplorationAnt copy() {
		TaskExplorationAnt ant =  new TaskExplorationAnt(pathCopy(), agent, reservationsCopy(), id);
		if(!taskOptions.isEmpty())
			ant.addTaskOptions(taskOptions);
		ant.isReturning=this.isReturning;
		ant.sendBy=this.sendBy;
		return ant;
	}

	public void addTaskOptions(List<TaskOptions> taskOptions2) {
		this.taskOptions.addAll(taskOptions2);
		this.calculatePriority();
	}

	public void addTaskOptions(TaskOptions taskOptions) {
		this.taskOptions.add(taskOptions);
	}
	
	public List<TaskOptions> getTaskOptions() {
		return this.taskOptions;
	}
	
	@Override
	public void handleAnt(BasicAgent cm, TimeLapse timeLapse) {
		cm.handleTaskExplorationAnt(this, timeLapse);
	}

	public Point getEndPoint() {
		return this.path.get(path.size()-1);
	}
	
	public long getTaskPathLength() {
		if (path.size() == 1) {
			return 0;
		}
		long distance = 0;
		for (int i = 0; i < path.size() - 1; i++) {
			distance += Point.distance(path.get(i), path.get(i + 1));
		}
		if (dependantAnt.isEmpty())
			return distance;
		
		long max = distance;
		for(PathAnt pa: dependantAnt) {
			if(pa instanceof TaskPathExplorationAnt)
				max = (long) Math.max(distance + ((TaskPathExplorationAnt) pa).getSoloPathLength() + SimulationSettings.BATTERY_LOAD_IMPACT, max);
		}
		
		return max;
	}

	public int getHighestPriotity() {
		return highestPriotity;
	}

	public void setHighestPriotity(int highestPriotity) {
		this.highestPriotity = highestPriotity;
	}
	
	public String toString() {
		if(!this.isReturning)
			return "tasksExplo " + this.id +" to " +  this.getEndPoint().toString()  + " : " + this.highestPriotity;	
		return "tasksExplo " + this.id +" to " +  this.getEndPoint().toString()  + " : " + this.highestPriotity + ", arrival: "  + this.getLastReservation().startTime ;
	}

	public void calculatePriority() {
		for(TaskOptions to: this.taskOptions) {
			this.highestPriotity = Math.max(this.highestPriotity, to.getPriority());
		}
	}
}
