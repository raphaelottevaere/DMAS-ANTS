package comperators;

import java.util.Comparator;

import ants.path.ExplorationAnt;

public class AntReservationComparator implements Comparator<ExplorationAnt> {
	
	@Override
	public int compare(ExplorationAnt o1, ExplorationAnt o2) {
		if(o1.getLastReservation().endTime < o2.getLastReservation().endTime)
			return -1;
		else if (o1.getLastReservation().endTime > o2.getLastReservation().endTime)
			return 1;
		else
			return 0;
	}

}
