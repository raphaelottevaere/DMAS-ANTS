package deadlocks;

import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;

public class DeadLockedEdge extends Edge {

	private final AGVAgent AgvAgent;
	
	public DeadLockedEdge(Point from, Point to, AGVAgent waitingAgent) {
		super(from, to);
		this.AgvAgent = waitingAgent;
	}

	public AGVAgent getAgvAgent() {
		return AgvAgent;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof DeadLockedEdge))
			return false;
		DeadLockedEdge temp = (DeadLockedEdge) o;
		if (temp.from().equals(this.from()) && this.to().equals(temp.to()) && this.AgvAgent.equals(temp.AgvAgent))
			return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		  return from.hashCode() + to.hashCode() + AgvAgent.ID%1000;
		}

}
