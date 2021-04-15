package ants.path;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import ants.BasicAnt;
import reservations.RouteReservation;
import simulator.SimulationSettings;
import simulator.SimulationException;

/**
 * Represent a Basic Ant
 * 
 * @author rapha
 *
 */
public abstract class PathAnt extends BasicAnt {
	public List<Point> path;
	public List<RouteReservation> reservations;
	protected Set<PathAnt> dependantAnt = new HashSet<PathAnt>();
	public boolean accepted;

	public PathAnt(List<Point> path, AGVAgent agent, List<RouteReservation> r) {
		super(agent);
		this.isReturning = false;
		if (path != null)
			this.path = path;
		/*else
			System.err.println(new SimulationException("Can't make an ant without a path"));
		 */

		if (r != null)
			this.reservations = new ArrayList<>(r);
		else
			reservations = new ArrayList<RouteReservation>();
	}

	public PathAnt(List<Point> pathCopy, AGVAgent agent, List<RouteReservation> reservationsCopy, int pid) {
		super(pid,agent);
		this.isReturning = false;
		if (pathCopy != null)
			this.path = pathCopy;
		else
			System.err.println(new SimulationException("Can't make an ant without a path"));

		if (reservationsCopy != null)
			this.reservations = new ArrayList<>(reservationsCopy);
		else
			reservations = new ArrayList<RouteReservation>();
	}
	
	@Override
	public abstract PathAnt copy();

	public List<Point> getPath() {
		return path;
	}

	public void AddPointToPath(Point p) {
		this.path.add(p);
	}

	public Point getNextPoint(Point p) throws SimulationException {
		if(path==null)
			return null;
		
		if (!isReturning) {
			int index = path.indexOf(p);
			if (index == path.size() - 1)
				return null;
			if (index < path.size())
				return path.get(index + 1);
			throw new SimulationException("Couldnt get non returning ant next point in path  "  + this.agent);
		} else {
			if (path.get(0).equals(p))
				return null;
			int index = path.indexOf(p);
			if(index==-1) {
				throw new SimulationException("Couldnt get returning ant next point in path "  + this.agent);
			}
			if (index < path.size())
				return path.get(index - 1);
			throw new SimulationException("Couldnt get returning ant next point in path "  + this.agent);
		}
	}

	public List<Point> pathCopy() {
		return new ArrayList<Point>(path);
	}

	public List<RouteReservation> reservationsCopy() {
		return new ArrayList<RouteReservation>(reservations);
	}

	public RouteReservation getLastReservation() {
		if (reservations.size() - 1 < 0)
			return null;
		return reservations.get(reservations.size() - 1);
	}

	public void addRouteReservation(RouteReservation r) {
		if(r==null) {
			System.err.println(" A NULL routeResevation was added");
		}
		reservations.add(r);
	}

	public boolean hasDependentAnt() {
		return !this.dependantAnt.isEmpty();
	}

	public void setDependentAnt(PathAnt dependentAnt) {
		this.dependantAnt.add(dependentAnt);
	}

	public boolean checkEndPoint(Point p) {
		if (!isReturning) {
			return path.get(path.size() - 1).equals(p);
		} else {
			return path.get(0).equals(p);
		}
	}
	
	public boolean canContinue() {
		return true;
	}
	
	public Point getEndPoint() {
		return path.get(path.size() - 1);
	}

	public long getTotalPathLength() {
		if (path.size() == 1) {
			return 0;
		}
		long distance = 0;
		for (int i = 0; i < path.size() - 1; i++) {
			distance += Point.distance(path.get(i), path.get(i + 1));
		}
		if (dependantAnt.isEmpty())
			return distance;
		
		long max = 0;
		for(PathAnt pa: dependantAnt) {
			max = (long) Math.max(distance + pa.getTotalPathLength() + SimulationSettings.BATTERY_LOAD_IMPACT, max);
		}
		
		return max;
	}
}
