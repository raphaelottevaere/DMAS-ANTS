package reservations;

import agents.AGVAgent;

public abstract class TimeReservations extends Reservation{
	public long endTime;
	public long startTime;

	public TimeReservations(long startTime, long endTime, AGVAgent AGV) {
		super(AGV);
		this.startTime=startTime;
		this.endTime=endTime;
		
	}

	
	public long getEndTime() {
		return this.endTime;
	}
	
	public long getBeginTime() {
		return this.startTime;
	}
	
	public abstract String toString();

	public boolean isIn(long time) {
		return (startTime<=time && endTime>=time);
	}
}
