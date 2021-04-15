package comperators;

import java.util.Comparator;

import reservations.RouteReservation;

public class RouteComperator implements Comparator<RouteReservation>{

	@Override
	public int compare(RouteReservation o1, RouteReservation o2) {
		if(o1.getBeginTime()< o2.getBeginTime())
			return -1;
		else if(o1.getBeginTime() == o2.getBeginTime())
			return 0;
		else 
			return 1;
	}

}
