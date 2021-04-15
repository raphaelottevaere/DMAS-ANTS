package graphs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;
import org.javatuples.Pair;

import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import agents.ImportStorage;
import agents.StorageAgent;
import deadlocks.DeadlockSolver;
import models.DARModel;
import simulator.SimulationSettings;

public class ModelPopulator {

	final Map<Point, Pair<Integer, Point>> SA = new HashMap<>();
	final Map<Point, Pair<Integer, Point>> CS = new HashMap<>();
	final Set<Point> RestingPlace = new HashSet<Point>();
	final Map<Point, Integer> ChargingStations = new HashMap<Point, Integer>();
	final Set<Point> agvs = new HashSet<Point>();
	final Set<Point> in = new HashSet<Point>();
	final Set<Point> out = new HashSet<Point>();
	final Set<Point> HoldIAList = new HashSet<Point>();

	public ModelPopulator(String s) throws IOException {
		InputStream initialStream = GraphCreator.class.getResourceAsStream(s);
		read(new InputStreamReader(initialStream));
		initialStream = GraphCreator.class.getResourceAsStream(s);
		readAGVS(new InputStreamReader(initialStream));
	}

	// Need to look for a way to add all the nodes to the model and at the same time
	// differentiate between storage, charging station and infrastructure nodes!
	public void addAllStaticAgents(DARModel darModel, Set<Point> nodes, RandomGenerator rng,
			ListenableGraph<LengthData> staticGraph, DeadlockSolver dl) {

		for (Point node : nodes) {
			Pair<Integer, Point> item = SA.get(node);
			if (item != null) {
				// This point contains a StorageAgent
				if (item.getValue1() == null) {
					// This point is a non connected StorageAgent
					darModel.createStoringAgent(node, SimulationSettings.NODE_DISTANCE, SimulationSettings.AGV_SPEED,
							rng, staticGraph);
				} else {
					// This point is a connected StorageAgent
					darModel.createDUALStoringAgent(node, SimulationSettings.NODE_DISTANCE,
							SimulationSettings.AGV_SPEED, rng, staticGraph, item.getValue0(), item.getValue1());
				}
			} else if (RestingPlace.contains(node)) {
				// This point contains a RestingPlace
				darModel.createRestingPlace(node, SimulationSettings.NODE_DISTANCE, SimulationSettings.AGV_SPEED,
						staticGraph, rng);
			} else if (ChargingStations.get(node) != null) {
				// This point contains a ChargingStation
				darModel.createChargingStation(node, rng, ChargingStations.get(node), SimulationSettings.CHARGE_AMMOUNT,
						SimulationSettings.NODE_DISTANCE, SimulationSettings.AGV_SPEED, staticGraph);
			} else if (in.contains(node)) {
				// This is a node from which only cars can come into the network
				darModel.createImportAgent(node, SimulationSettings.NODE_DISTANCE, SimulationSettings.AGV_SPEED, rng,
						staticGraph);
			} else if (out.contains(node)) {
				// This is a node from which only cars can be removed from the network
				darModel.createExportAgent(node, SimulationSettings.NODE_DISTANCE, SimulationSettings.AGV_SPEED, rng,
						staticGraph);
			} else if (HoldIAList.contains(node)) {
				// This is a HoldIA
				darModel.createHoldInfrastructureAgent(node, SimulationSettings.NODE_DISTANCE,
						SimulationSettings.AGV_SPEED, staticGraph, rng);
			} else {
				// This point is just an InfrastructureAgent
				darModel.createHoldInfrastructureAgent(node, SimulationSettings.NODE_DISTANCE,
						SimulationSettings.AGV_SPEED, staticGraph, rng);
			}
		}
	}

	public void addAllAGV(DARModel darModel, Set<Point> nodes, RandomGenerator rng,
			ListenableGraph<LengthData> staticGraph) {
		HashSet<Point> occupiedNodes = new HashSet<Point>();

		for (Point p : agvs) {
			double percent = (20 + Math.random() * (75));
			double charge = percent * SimulationSettings.AGVMaxCharge / 100;
			darModel.createAGV(p, SimulationSettings.AGV_SPEED, (int) charge, staticGraph,
					SimulationSettings.ALTERNATIVE_PATHS_EXPLORATION, SimulationSettings.EXPLORATION_PATH_REFRESH_TIME,
					SimulationSettings.INTENTION_REFRESH_TIME, darModel.getDeadLockSolver());
			occupiedNodes.add(p);
		}

		while (SimulationSettings.AGV_AMMOUNT > occupiedNodes.size()) {
			double percent = (20 + Math.random() * (80));
			double charge = percent * SimulationSettings.AGVMaxCharge / 100;
			Point node = staticGraph.getRandomNode(rng);

			if (!occupiedNodes.contains(node)) {
				darModel.createAGV(node, SimulationSettings.AGV_SPEED, (int) charge, staticGraph,
						SimulationSettings.ALTERNATIVE_PATHS_EXPLORATION,
						SimulationSettings.EXPLORATION_PATH_REFRESH_TIME, SimulationSettings.INTENTION_REFRESH_TIME,darModel.getDeadLockSolver());
				occupiedNodes.add(node);
			}
		}
	}

	public void populateStorage(DARModel darModel, RandomGenerator rng) {
		System.out.println("Populating Storage");
		Collection<StorageAgent> saCol = darModel.getAllStoringAgents();
		for (StorageAgent sa : saCol) {
			if (sa instanceof StorageAgent) {
				// doesnt need to update non dual storage agents! -> but is easier?
				// doesnt need to update storageAgents twice (for dual storageAgents!)

				double temp = rng.nextDouble();
				long temp2 = Math.round(temp * SimulationSettings.STORAGE_STD);
				int carAmmount = Math.toIntExact(temp2) + SimulationSettings.STORAGE_MEAN;
				if (carAmmount > sa.getMaxCars()) {
					carAmmount = sa.getMaxCars();
				}
				LinkedList<String> ids = new LinkedList<String>();
				for (int i = 0; i < carAmmount; i++) {
					ids.add(randomCarId(sa, rng));
				}
				sa.setCars(ids);
			}
		}

		Collection<ImportStorage> IS = darModel.getAllImport();
		for (StorageAgent sa : IS) {
			// doesnt need to update non dual storage agents! -> but is easier?
			// doesnt need to update storageAgents twice (for dual storageAgents!)

			int carAmmount = 10;
			LinkedList<String> ids = new LinkedList<String>();
			for (int i = 0; i < carAmmount; i++) {
				ids.add(randomCarId(sa, rng));
			}
			sa.setCars(ids);
		}
	}

	private String randomCarId(StorageAgent sa, RandomGenerator rng) {
		return ("SA" + sa.getPosition().get().x + sa.getPosition().get().y + "-" + rng.nextInt(10000));
	}

	public void readAGVS(Reader inputStream) throws IOException {
		final String QUOTE = "\"";
		final BufferedReader reader = new BufferedReader(inputStream);

		String line;
		while ((line = reader.readLine()) != null) {
			if (line.contains("AGV")) {
				final String[] position = line.split(QUOTE);
				String[] sp = position[1].split(",");
				final Point p = new Point(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
				agvs.add(p);
			}
		}
	}

	// This can probably be further optimised
	public void read(Reader inputStream) throws IOException {
		final String QUOTE = "\"";
		final BufferedReader reader = new BufferedReader(inputStream);

		String line;
		while ((line = reader.readLine()) != null) {
			if (line.contains("type")) {
				if (line.contains("SA")) {
					final String[] position = line.split(QUOTE);
					String[] sp = position[1].split(",");
					final Point p = new Point(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
					final Integer ammount = Integer.parseInt(position[3]);
					Point connected = null;
					if (position.length == 9) {
						sp = position[7].split(",");
						connected = new Point(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
					}
					SA.put(p, Pair.with(ammount, connected));
				} else if (line.contains("HoldIA")) {
					final String[] position = line.split(QUOTE);
					String[] sp = position[1].split(",");
					final Point p = new Point(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
					HoldIAList.add(p);
				} else if (line.contains("AGV")) {
					final String[] position = line.split(QUOTE);
					String[] sp = position[1].split(",");
					final Point p = new Point(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
					agvs.add(p);
				} else if (line.contains("CS")) {
					final String[] position = line.split(QUOTE);
					String[] sp = position[1].split(",");
					final Point p = new Point(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
					final Integer ammount = Integer.parseInt(position[3]);
					ChargingStations.put(p, ammount);
				} else if (line.contains("RP")) {
					final String[] position = line.split(QUOTE);
					String[] sp = position[1].split(",");
					final Point p = new Point(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
					RestingPlace.add(p);
				} else if (line.contains("IN")) {
					final String[] position = line.split(QUOTE);
					String[] sp = position[1].split(",");
					final Point p = new Point(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
					in.add(p);
				} else if (line.contains("OUT")) {
					final String[] position = line.split(QUOTE);
					String[] sp = position[1].split(",");
					final Point p = new Point(Double.parseDouble(sp[0]), Double.parseDouble(sp[1]));
					out.add(p);
				}
			}
		}
	}

}
