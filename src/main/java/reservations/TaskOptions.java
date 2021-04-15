package reservations;

import com.github.rinde.rinsim.geom.Point;

import agents.StorageAgent;
import tasks.BasicTask;

public class TaskOptions {

	public final StorageAgent taskAgent;
	private Point point;
	public final BasicTask bt;
	private final Point beginPoint;
	private int priority;

	public TaskOptions(BasicTask bt, StorageAgent taskAgent, int priority) {
		this.bt=bt;
		this.beginPoint = taskAgent.getPosition().get();
		this.setPriority(priority);
		this.taskAgent = taskAgent;
	}

	public Point getPoint() {
		return this.point;
	}
	
	public void setPoint(Point point) {
		this.point = point;
	}

	public Point getBeginPoint() {
		return beginPoint;
	}
	
	public String toString() {
			return "Taskoptions for " + bt;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}


}
