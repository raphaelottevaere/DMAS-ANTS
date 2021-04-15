package tasks;

import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;

public class HoldingTask extends BasicTask {

	public boolean finished = false;
	public AGVAgent agv = null;
	public boolean returnAllowed = false;
	public DeliveryTask inOrderFor;

	public HoldingTask(Point point, long time, String carId, long endTime, DeliveryTask st, int HEUR_HOUR_REMAINING_TIME,
			int HEUR_30MIN_REMAINING_TIME, int HEUR_15MIN_REMAINING_TIME, int HEUR_URGENT_TIME) {
		super(point, time, carId, point, endTime, 0, HEUR_HOUR_REMAINING_TIME, HEUR_30MIN_REMAINING_TIME,
				HEUR_15MIN_REMAINING_TIME, HEUR_URGENT_TIME);
		inOrderFor = st;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	public String toString() {
		return "MovementTask: " + endPosition + " for carID: " + carId;
	}

	@Override
	public boolean deliver(String carID2, Point position) {
		if (this.carId.equals(carId) && this.endPosition.equals(position)) {
			this.complete = true;
			return complete;
		}
		return complete;
	}
}
