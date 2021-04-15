package deadlocks;
import com.github.rinde.rinsim.geom.Point;

public class Edge {
	protected final Point from;
	protected final Point to;
	
	Edge(Point from, Point to) {
		this.from = from;
		this.to = to;
	}

	public Point from() {
		return this.from;
	}

	public Point to() {
		return this.to;
	}

}
