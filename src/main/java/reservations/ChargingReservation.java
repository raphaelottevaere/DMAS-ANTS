package reservations;

import agents.AGVAgent;

public class ChargingReservation extends TimeReservations {

    public ChargingReservation(AGVAgent agv, long evaporationTimestamp, long beginTime, long endTime) {
		super(beginTime,endTime,agv);
    }

    public ChargingReservation copy(long evaporationTimestamp) {
        return new ChargingReservation(agv, evaporationTimestamp, startTime, endTime);
    }

	@Override
	public String toString() {
		return "ChargingReservation for AGV:" + agv.ID +" with Start " + startTime;
	}
}
