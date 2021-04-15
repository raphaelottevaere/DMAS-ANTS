package agents;

import java.util.LinkedList;
import java.util.List;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.geom.Point;

public class DARDepot extends Depot{

	private List<AGVAgent> waitLine = new LinkedList<AGVAgent>();
	
	public DARDepot(Point position) {
		super(position);
	}
	
	public void addAGVToLine(AGVAgent r) {
		this.waitLine.add(r);
	}
	
	public boolean removeAGVFromLine(AGVAgent a) {
		return this.waitLine.remove(a);
	}
	
	public boolean removeAGVFromLine(int ID) {
		return this.waitLine.removeIf(a -> a.ID == ID);
	}

	public boolean hasAGV() {
		return !waitLine.isEmpty();
	}

	public AGVAgent getFirst() {
		if(hasAGV()) {
			AGVAgent temp = waitLine.get(0);
			waitLine.remove(0);
			return temp;
		}
		return null;
	}

	public List<AGVAgent> getAllAgv() {
		return waitLine;
	}

	public boolean contains(AGVAgent agv) {
		return waitLine.contains(agv);
	}
}
