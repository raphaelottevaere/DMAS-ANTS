package experiments;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import simulator.SimulationSettings;

public class ExperimentParameters {
	public long tickLength = SimulationSettings.TICK_LENGTH;
	public long simulationLength = SimulationSettings.SIMULATION_LENGTH;
	public int numRobots = SimulationSettings.AGV_AMMOUNT;
	public int batteryCapacity = SimulationSettings.AGVMaxCharge;
	public int nodeDistance = SimulationSettings.NODE_DISTANCE;
	public Unit<Length> distanceUnit = SimulationSettings.DISTANCE_UNIT;
	public Unit<Velocity> speedUnit = SimulationSettings.SPEED_UNIT;

	public double robotSpeed = SimulationSettings.AGV_SPEED;
	public int alternativePathsToExplore = SimulationSettings.ALTERNATIVE_PATHS_EXPLORATION;
	public long intentionReservationLifetime = SimulationSettings.INTENTION_RESERVATION_LIFETIME;
	public long reexploration = SimulationSettings.EXPLORATION_PATH_REFRESH_TIME;
	public long intentionRefreshTime = SimulationSettings.INTENTION_REFRESH_TIME;
	public double probNewDeliveryTask = SimulationSettings.NEW_TASK_PROB;
	public double storageAmountMean = SimulationSettings.STORAGE_MEAN;
	public double storageAmountStd = SimulationSettings.STORAGE_STD;

	public boolean imp1 = SimulationSettings.IMPORT_1_COMSUMER;
	public long ttl = SimulationSettings.TIME_TO_LOAD;
	public boolean cons1 = SimulationSettings.ONLY_1_TASKCONSUMER;
	public int minTask = SimulationSettings.MinSingleTasks;
	public int maxTasks = SimulationSettings.MaxTasks;
	public int maxFast = SimulationSettings.MAX_FAST_COALITION;

	public int ur = SimulationSettings.HEUR_URGENT_TIME;
	public int m15 = SimulationSettings.HEUR_15MIN_REMAINING_TIME;
	public int m30 = SimulationSettings.HEUR_30MIN_REMAINING_TIME;
	public int h1 = SimulationSettings.HEUR_HOUR_REMAINING_TIME;
	public int pickupsizefactor = SimulationSettings.HEUR_PICKUPTASK_SIZE_FACTOR;
	public int preferbatch = SimulationSettings.HEUR_PREFER_BATCH_JOBS;
	public int taskCons = SimulationSettings.HEUR_TASKCONS_SIZE_FACTOR;
	public int tasks1ammount = SimulationSettings.HEUR_1_TASK_AMMOUNT;

	public String graphName;
	public String id;
	public int repeat = SimulationSettings.REPEAT;
	public boolean verbose = false;
	public boolean showGUI = SimulationSettings.SHOWGUIExperiments;
	public int threads = SimulationSettings.THREADS;
	public boolean active_holdIA = SimulationSettings.HOLD_IA_ACTIVE;

	public ExperimentParameters(String graphName, String Id, int repeat) {
		this.graphName = graphName;
		this.id = Id;
		this.repeat = repeat;
	}
	
	public ExperimentParameters(String graphName, String Id) {
		this.graphName = graphName;
		this.id = Id;
	}

	public String toJson() {
		return "{" + "\"numRobots\": " + numRobots + ", " + "\"robotSpeed\": " + robotSpeed + ", "
				+ "\"batteryCapacity\": " + batteryCapacity + ", " + "\"speedUnit\": " + speedUnit + ", "
				+ "\"alternativePathsToExplore\": " + alternativePathsToExplore + ", "
				+ "\"intentionReservationLifetime\": " + intentionReservationLifetime + ", " + "\"intentionRefresh\": "
				+ intentionRefreshTime + ", " + "\"explorationRefreshTime\": " + reexploration + ", "
				+ "\"probNewDeliveryTask\": " + probNewDeliveryTask + ", " + "\"StorageAmountMean\": "
				+ storageAmountMean + ", " + "\"StorageAmountStd\": " + storageAmountStd + ", \"Import_1_cons\": " + imp1
				+ ", " + "\"timeToLoad\": " + ttl + ", \"only_1_taskConsumer\": " + cons1 + ", \"minTasks\": " + minTask
				+ ", \"maxTasks\": " + maxTasks + ", \"maxFastCoalitionForming\": " + maxFast + ", \"HEUR_URGENT\": " + ur
				+ ", \"HEUR_15MIN_REMAINING\": " + m15 + ", \"HEUR_30MIN_REMAINING\": " + m30 + ", \"HEUR_HOUR_REMAINING\": "
				+ h1 + ",\"HEUR_PICKUPTASK_SIZE_FACTOR\": " + pickupsizefactor + ", \"HEUR_PREFER_BATCH_JOBS\": "
				+ preferbatch + ", \"HEUR_TASKCONS_SIZE_FACTOR\": " + taskCons + ", \"active_holdIA\": " + active_holdIA
				+ "}";
	}

	public String getGraphName() {
		return this.graphName;
	}
}
