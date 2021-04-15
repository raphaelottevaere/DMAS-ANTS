package comperators;

import java.util.Comparator;

import com.github.rinde.rinsim.geom.Point;

import ants.findRestingPlaceAnt;

public class distanceComparator implements Comparator<findRestingPlaceAnt> {
	
	private Point position;

	public distanceComparator(Point position) {
		this.position = position;
	}

	@Override
	public int compare(findRestingPlaceAnt o1, findRestingPlaceAnt o2) {
		if(Point.distance(o1.acceptedPoint.position, position) == Point.distance(o2.acceptedPoint.position, position ))
			return 0;
		if(Point.distance(o1.acceptedPoint.position, position) < Point.distance(o2.acceptedPoint.position, position ))
				return -1;
		else 
				return +1;
	}
}
