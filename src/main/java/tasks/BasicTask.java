package tasks;

import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;


public abstract class BasicTask implements RoadUser {
	private final int HEUR_HOUR_REMAINING_TIME;
	private final int HEUR_30MIN_REMAINING_TIME;
	private final int HEUR_15MIN_REMAINING_TIME;
	private final int HEUR_URGENT_TIME;
	public static int IDCounter = 0;
	public final long startTime;
	public Point beginPosition;
	public final int ID;
	public Point currentPosition;
	private long endTime;
	private int priority;
	public boolean pickedup = false;
	public String carId;
	public Point endPosition;
	public boolean complete = false;
	public long completionTime;
	public long pickupTime;

	public int agvID = -1;

	public BasicTask(Point point, long time, String carId, Point endPoint, long endTime, int priority,
			int HEUR_HOUR_REMAINING_TIME, int HEUR_30MIN_REMAINING_TIME, int HEUR_15MIN_REMAINING_TIME,
			int HEUR_URGENT_TIME) {
		ID = getNextID();
		this.beginPosition = point;
		this.currentPosition = beginPosition;
		this.startTime = time;
		this.carId = carId;
		this.endPosition = endPoint;
		this.endTime = endTime;
		this.priority = priority;
		this.HEUR_HOUR_REMAINING_TIME = HEUR_HOUR_REMAINING_TIME;
		this.HEUR_30MIN_REMAINING_TIME = HEUR_30MIN_REMAINING_TIME;
		this.HEUR_15MIN_REMAINING_TIME = HEUR_15MIN_REMAINING_TIME;
		this.HEUR_URGENT_TIME = HEUR_URGENT_TIME;
	}

	private int getNextID() {
		return IDCounter++;
	}

	public void initRoadUser(@NotNull RoadModel roadModel) {
		roadModel.addObjectAt(this, beginPosition);
	}

	public abstract boolean isFinished();

	public int getTaskID() {
		return ID;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public abstract boolean deliver(String carID2, Point point);

	public int getTimePriority(long time) {
		if (endTime == 0) {
			return 0;
		}
		long timeTillEnd = endTime - time;
		if (timeTillEnd - 60 * 60 * 1000 > 0) {
			// More then an hour left, no priority
			return HEUR_HOUR_REMAINING_TIME;
		} else if (timeTillEnd - 30 * 60 * 1000 > 0) {
			// More then 30 minutes left, slight priority
			return HEUR_30MIN_REMAINING_TIME;
		} else if (timeTillEnd - 15 * 60 * 1000 > 0) {
			// More then 15 minutes left, high priority
			return HEUR_15MIN_REMAINING_TIME;
		} else {
			// less then 15 minutes left, high priority
			return HEUR_URGENT_TIME;
		}
	}

}
