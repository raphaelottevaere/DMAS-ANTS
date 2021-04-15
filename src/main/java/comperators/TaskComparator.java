package comperators;

import java.util.Comparator;

import ants.path.TaskExplorationAnt;

public class TaskComparator implements Comparator<TaskExplorationAnt> {

	@Override
	public int compare(TaskExplorationAnt o1, TaskExplorationAnt o2) {
		if(o1.getHighestPriotity() > o2.getHighestPriotity())
			return -1;
		else if (o2.getHighestPriotity() > o1.getHighestPriotity())
			return +1;
		else {
			if(o1.getLastReservation().startTime < o2.getLastReservation().startTime )
				return -1;
			else if(o2.getLastReservation().startTime < o1.getLastReservation().startTime )
				return +1;
			else 
				return 0;
		}
	}

}