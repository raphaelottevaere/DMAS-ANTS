package reservations;

import agents.AGVAgent;
import simulator.SimulationSettings;

public abstract class Reservation {
	public final AGVAgent agv;
	public int evaporates = SimulationSettings.INTENTION_RESERVATION_LIFETIME;
	
	public Reservation(AGVAgent AGV) {
		this.agv=AGV;
	}

	public void resetEvaporation() {
		evaporates=		SimulationSettings.INTENTION_RESERVATION_LIFETIME;
		
	}
}
