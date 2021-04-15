package comperators;

import java.util.Comparator;

import ants.path.ChargingExplorationAnt;

public class ChargingExplorationComparator implements Comparator<ChargingExplorationAnt> {
	
	
	@Override
	public int compare(ChargingExplorationAnt o1, ChargingExplorationAnt o2) {
		
		if(o1.chargingOptions().getCr().getBeginTime() < o2.chargingOptions().getCr().getBeginTime())
			return -1;
		else if(o1.chargingOptions().getCr().getBeginTime() > o2.chargingOptions().getCr().getBeginTime())
			return 1;
		else {
			if(o1.chargingOptions().getCr().getEndTime() > o2.chargingOptions().getCr().getEndTime())
				return -1;
			else if(o1.chargingOptions().getCr().getEndTime() < o2.chargingOptions().getCr().getEndTime())
				return 1;
			else
				return 0;
		}
	}

}
