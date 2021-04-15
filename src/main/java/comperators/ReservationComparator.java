package comperators;

import java.util.Comparator;

import reservations.TimeReservations;

public class ReservationComparator implements Comparator<TimeReservations> {
	
	@Override
	public int compare(TimeReservations o1, TimeReservations o2) {
		if(o1.getBeginTime()< o2.getBeginTime())
			return -1;
		else if(o1.getBeginTime() == o2.getBeginTime())
			return 0;
		else 
			return 1;
	}
}
