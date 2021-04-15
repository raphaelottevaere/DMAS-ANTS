package ants.path;

import java.util.List;

import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import tasks.BasicTask;

public class TaskPathExplorationAnt extends PathExplorationAnt {

	public BasicTask st;
	
	public TaskPathExplorationAnt(List<Point> path, AGVAgent agent, Point goal, BasicTask bt) {
		super(path, agent, goal);
		this.st=bt;
	}

	long getSoloPathLength() {
		long distance = 0;
		for (int i = 0; i < path.size() - 1; i++) {
			distance += Point.distance(path.get(i), path.get(i + 1));
		}
		return distance;
	}

	public String toString() {
		if(!this.isReturning)
			return "tasksPathExplo " + this.id +" to " +  this.getEndPoint().toString();	
		return "tasksPathExplo " + this.id +" to " +  this.getEndPoint().toString() + " arrival: "  + this.getLastReservation().startTime ;
	}
}
