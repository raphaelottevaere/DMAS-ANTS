package ants.path;

import java.util.List;

import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import reservations.RouteReservation;

public abstract class ExplorationAnt extends PathAnt {

	public ExplorationAnt(List<Point> path, AGVAgent agent, List<RouteReservation> r) {
		super(path, agent, r);
	}
	
	public ExplorationAnt(List<Point> path, AGVAgent agent, List<RouteReservation> r, int id) {
		super(path, agent, r,id);
	}

}
