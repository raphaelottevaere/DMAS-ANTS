package tasks;

import com.github.rinde.rinsim.geom.Point;

public class DeliveryTask extends BasicTask {

	public DeliveryTask(Point startPosition, Point endPosition, String carId, long time, long endTime, int priority,
			int HEUR_HOUR_REMAINING_TIME, int HEUR_30MIN_REMAINING_TIME, int HEUR_15MIN_REMAINING_TIME,
			int HEUR_URGENT_TIME) {
		super(startPosition, time, carId, endPosition, endTime, priority, HEUR_HOUR_REMAINING_TIME,
				HEUR_30MIN_REMAINING_TIME, HEUR_15MIN_REMAINING_TIME, HEUR_URGENT_TIME);
	}

	@Override
	public boolean isFinished() {
		return complete;
	}

	public boolean deliver(String carID, Point position) {
		if (this.carId.equals(carId) && this.endPosition.equals(position)) {
			this.complete = true;
			return complete;
		}
		return complete;
	}

	public String toString() {
		return "SingleTask: " + endPosition + " for carID: " + carId;
	}
}
