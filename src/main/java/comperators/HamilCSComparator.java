package comperators;

import java.util.Comparator;

import com.github.rinde.rinsim.geom.Point;

import agents.ChargingStation;

public class HamilCSComparator implements Comparator<ChargingStation> {

	private Point position;

	public HamilCSComparator(Point position) {
		this.position = position;
	}

	@Override
	public int compare(ChargingStation o1, ChargingStation o2) {
		if (Point.distance(o1.position, position) == Point.distance(o2.position, position))
			return 0;
		if (Point.distance(o1.position, position) < Point.distance(o2.position, position))
			return -1;
		else
			return +1;
	}
}
