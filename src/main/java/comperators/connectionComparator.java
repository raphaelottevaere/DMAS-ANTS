package comperators;

import java.util.Comparator;

import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

public class connectionComparator implements Comparator<Point> {

	private ListenableGraph<LengthData> graph;

	public connectionComparator(ListenableGraph<LengthData> graph) {
		this.graph = new ListenableGraph<LengthData>(graph);
	}

	@Override
	public int compare(Point o1, Point o2) {
		if (graph.getIncomingConnections(o1).size() + graph.getOutgoingConnections(o1).size() == graph
				.getIncomingConnections(o2).size() + graph.getOutgoingConnections(o2).size())
			return 0;
		if (graph.getIncomingConnections(o1).size() + graph.getOutgoingConnections(o1).size() < graph
				.getIncomingConnections(o2).size() + graph.getOutgoingConnections(o2).size())
			return -1;
		else
			return +1;
	}

}
