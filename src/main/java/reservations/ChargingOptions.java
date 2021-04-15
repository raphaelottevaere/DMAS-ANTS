package reservations;

import agents.ChargingStation;
import agents.ChargingStationUtil;

public class ChargingOptions {
	
	private ChargingStation cs;
	private ChargingStationUtil cu;
	private ChargingReservation cr;
	
	public ChargingReservation getCr() {
		return cr;
	}

	public void setCr(ChargingReservation cr) {
		this.cr = cr;
	}
	
	public ChargingOptions(ChargingStation cs, ChargingStationUtil cu) {
		this.cs=cs;
		this.cu=cu;
	}

	public ChargingStation getChargingStation() {
		return cs;
	}

	public ChargingStationUtil getChargingStationUtil() {
		return this.cu;
	}

}
