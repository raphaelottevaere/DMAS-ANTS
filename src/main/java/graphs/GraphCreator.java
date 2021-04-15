package graphs;

import static com.google.common.collect.Maps.newHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.github.rinde.rinsim.geom.*;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.geom.io.Filters;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import simulator.SimulationSettings;

// Changes this to load from an xml file - .dot file like the RinSim examples
public class GraphCreator {

	public static ListenableGraph<LengthData> getGraph() {
		final Graph<LengthData> g = new TableGraph<LengthData>();

		final Table<Integer, Integer, Point> matrix = newMatrix(5, 5, new Point(0, 0), SimulationSettings.AGV_Length);

		for (final Map<Integer, Point> column : matrix.columnMap().values()) {
			Graphs.addBiPath(g, column.values());
		}

		for (final Map<Integer, Point> row : matrix.rowMap().values()) {
			Graphs.addBiPath(g, row.values());
		}

		return new ListenableGraph<LengthData>(g);
	}

	private static Table<Integer, Integer, Point> newMatrix(int col, int row, Point offset, int agvLength) {
		final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable.builder();
		for (int c = 0; c < col; c++) {
			for (int r = 0; r < row; r++) {
				Point point = new Point(offset.x + c * agvLength * 2, offset.y + r * agvLength * 2);
				builder.put(r, c, point);
			}
		}
		return builder.build();
	}

	private static final Map<String, Graph<LengthData>> GRAPH_CACHE = newHashMap();

	public static Graph<LengthData> loadGraph(String name, List<String> graphs) {
		if (name.equals(""))
			return getGraph();

		try {
			if (GRAPH_CACHE.containsKey(name)) {
				return GRAPH_CACHE.get(name);
			}
			Graph<LengthData> g = null;
			for (String s : graphs) {
				Graph<LengthData> temp = DotGraphIO.getLengthGraphIO(Filters.selfCycleFilter())
						.read(GraphCreator.class.getResourceAsStream(s));				
				if (g == null) {
					g = temp;
				} else {
					g.merge(temp);
				}
			}
			GRAPH_CACHE.put(name, g);
			return g;
		} catch (final FileNotFoundException e) {
			throw new IllegalStateException(e);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
