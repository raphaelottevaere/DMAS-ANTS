package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import java.util.Set;

import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.DecisionAGV;
import agents.DualStorageAgent;
import agents.ExportStorage;
import agents.HoldIA;
import agents.ImportStorage;
import agents.ChargingStation;
import agents.InfrastructureAgent;
import agents.RestingAgent;
import agents.StorageAgent;
import agents.StorageAgentConnector;
import deadlocks.DeadLocks;
import deadlocks.DeadlockSolver;
import experiments.ExperimentParameters;
import graphs.AStar;
import simulator.SimulationSettings;
import simulator.SimulationException;
import tasks.BasicTask;
import tasks.CarPackage;
import tasks.DeliveryTask;

public class DARModel extends AbstractModel<DARUser> {

	private RoadModel roadModel;
	private Simulator sim;
	private DynamicGraphRoadModelImpl dynamicGraphRoadModel;
	private EventDispatcher eventDispatcher;
	private long tickLength;
	private boolean verbose;
	private Map<Point, ChargingStation> chargingStations = new HashMap<Point, ChargingStation>();
	private int robotID = 0;
	private Map<Point, StorageAgent> storageAgents = new HashMap<Point, StorageAgent>();
	private Map<Point, ImportStorage> ImportAgents = new HashMap<Point, ImportStorage>();
	private Map<Point, ExportStorage> ExportAgents = new HashMap<Point, ExportStorage>();
	private Map<Point, InfrastructureAgent> infrastructureAgents = new HashMap<Point, InfrastructureAgent>();
	// change for interdependent tasks
	private Map<Integer, DeliveryTask> tasks = new HashMap<Integer, DeliveryTask>();
	private Set<Point> taskPoints = new HashSet<Point>();
	private Set<Point> restingPlaces = new HashSet<Point>();
	private final ExperimentParameters p;

	int currentTasks = 0;
	private List<AGVAgent> AGV = new ArrayList<AGVAgent>();
	private DeadlockSolver dl;

	public DARModel(RoadModel roadModel, SimulatorAPI simulatorAPI, long tickLength, boolean verbose,
			ExperimentParameters p) {
		this.p = p;
		this.roadModel = roadModel;
		this.sim = (Simulator) simulatorAPI;
		this.dynamicGraphRoadModel = (DynamicGraphRoadModelImpl) this.roadModel;
		this.eventDispatcher = new EventDispatcher(DAREventType.values());
		this.tickLength = tickLength;
		this.verbose = verbose;
		AStar.getInstance().setGraph(this.dynamicGraphRoadModel.getGraph());
	}

	@Nonnull
	@Override
	public <U> U get(Class<U> type) {
		if (type == DARModel.class) {
			return type.cast(this);
		}
		throw new IllegalArgumentException();
	}

	public static DARModelBuilder builder(long tickLength, boolean verbose, ExperimentParameters p) {
		return new DARModelBuilder(tickLength, verbose, p);
	}

	public boolean register(@NotNull DARUser user) {
		user.initDARUser(this);
		return true;
	}

	public boolean unregister(@NotNull DARUser user) {
		user.removeDARUser(this);
		return true;
	}

	public void createChargingStation(Point position, RandomGenerator rng, int chargingstationAgv,
			double chargingstationRecharge, int nodeDistance, double agvSpeed,
			ListenableGraph<LengthData> staticGraph) {
		ChargingStation chargingStation = new ChargingStation(position, nodeDistance, agvSpeed, tickLength, verbose,
				staticGraph, this, chargingstationAgv, chargingstationRecharge, rng);

		this.chargingStations.put(position, chargingStation);
		this.sim.register(chargingStation);
	}

	public void createInfrastructureAgent(Point position, int nodeDistance, double agvSpeed,
			ListenableGraph<LengthData> staticGraph, RandomGenerator rng) {
		InfrastructureAgent agent = new InfrastructureAgent(position, nodeDistance, agvSpeed, tickLength, verbose,
				staticGraph, this, rng);
		this.infrastructureAgents.put(position, agent);
		this.sim.register(agent);
	}

	public void createHoldInfrastructureAgent(Point position, int nodeDistance, double agvSpeed,
			ListenableGraph<LengthData> staticGraph, RandomGenerator rng) {
		InfrastructureAgent agent = new HoldIA(position, nodeDistance, agvSpeed, tickLength, verbose, staticGraph, this,
				rng, p.active_holdIA);
		this.infrastructureAgents.put(position, agent);
		this.sim.register(agent);
	}

	public void createRestingPlace(Point position, int nodeDistance, double agvSpeed,
			ListenableGraph<LengthData> staticGraph, RandomGenerator rng) {
		RestingAgent agent = new RestingAgent(position, nodeDistance, agvSpeed, tickLength, verbose, staticGraph, this,
				rng);
		this.restingPlaces.add(agent.position);
		this.infrastructureAgents.put(agent.position, agent);
		this.sim.register(agent);
	}

	public void createDUALStoringAgent(Point position, int nodeDistance, double agvSpeed, RandomGenerator rng,
			ListenableGraph<LengthData> staticGraph, Integer maxCars, Point connectedPoint) {
		StorageAgent connected = storageAgents.get(connectedPoint);
		if (connected == null) {
			StorageAgentConnector connection = new StorageAgentConnector(this);
			DualStorageAgent agent = new DualStorageAgent(position, nodeDistance, agvSpeed, tickLength, verbose, rng,
					staticGraph, this, maxCars, connection, p.ttl, p.cons1, p.maxFast, p.taskCons, p.pickupsizefactor,
					p.tasks1ammount, p.h1, p.m30, p.m15, p.ur);
			this.storageAgents.put(position, agent);
			this.restingPlaces.add(agent.position);
			this.sim.register(agent);
		} else {
			DualStorageAgent das = ((DualStorageAgent) connected);
			StorageAgentConnector connection = das.getConnector();
			DualStorageAgent agent = new DualStorageAgent(position, nodeDistance, agvSpeed, tickLength, verbose, rng,
					staticGraph, this, maxCars, connection, p.ttl, p.cons1, p.maxFast, p.taskCons, p.pickupsizefactor,
					p.tasks1ammount, p.h1, p.m30, p.m15, p.ur);
			this.storageAgents.put(position, agent);
			this.restingPlaces.add(agent.position);
			this.sim.register(agent);
			connection.setDualAgent(das, agent);
		}
	}

	public void createStoringAgent(Point position, int nodeDistance, double agvSpeed, RandomGenerator rng,
			ListenableGraph<LengthData> staticGraph) {
		StorageAgent agent = new StorageAgent(position, nodeDistance, agvSpeed, tickLength, verbose, rng, staticGraph,
				this, 6, p.ttl, p.cons1, p.maxFast, p.taskCons, p.pickupsizefactor, p.tasks1ammount, p.h1, p.m30, p.m15,
				p.ur);
		this.storageAgents.put(position, agent);
		this.restingPlaces.add(agent.position);
		this.sim.register(agent);
	}

	public void createImportAgent(Point position, int nodeDistance, double agvSpeed, RandomGenerator rng,
			ListenableGraph<LengthData> staticGraph) {
		ImportStorage agent = new ImportStorage(position, nodeDistance, agvSpeed, tickLength, verbose, rng, staticGraph,
				this, p.imp1, p.ttl, p.cons1, p.maxFast, p.taskCons, p.pickupsizefactor, p.tasks1ammount, p.h1, p.m30,
				p.m15, p.ur);
		this.ImportAgents.put(position, agent);
		this.sim.register(agent);
	}

	public void createExportAgent(Point position, int nodeDistance, double agvSpeed, RandomGenerator rng,
			ListenableGraph<LengthData> staticGraph) {
		ExportStorage agent = new ExportStorage(position, nodeDistance, agvSpeed, tickLength, verbose, rng, staticGraph,
				this, p.ttl, p.cons1, p.maxFast, p.taskCons, p.pickupsizefactor, p.tasks1ammount, p.h1, p.m30, p.m15,
				p.ur);
		this.ExportAgents.put(position, agent);
		this.sim.register(agent);
	}

	public void createAGV(Point randomNode, double agvSpeed, int agvcharge, ListenableGraph<LengthData> staticGraph,
			int alternativePathsExploration, int explorationRefreshTime, int intentionRefreshTime, DeadlockSolver dl2) {

		try {
			AGVAgent agv = new DecisionAGV(robotID++, randomNode, agvSpeed, alternativePathsExploration,
					explorationRefreshTime, intentionRefreshTime, SimulationSettings.AGVVERBOSE, getAgentAt(randomNode),
					agvcharge, dl2);
			agv.initDARUser(this);
			this.AGV.add(agv);
			sim.register(agv);
		} catch (SimulationException e) {
			e.printStackTrace();
		}
	}

	public EventAPI getEventAPI() {
		return this.eventDispatcher.getPublicEventAPI();
	}

	public void robotArrivedAtChargingStation(AGVAgent r, ChargingStation cs, TimeLapse time, double capacityUsed) {
		this.chargingStations.get(cs.position).AGVArrivesAtChargingStation(r, time);
		this.eventDispatcher.dispatchEvent(new DAREvent(DAREventType.AGV_AT_CHARGING_STATION, 0, null, null, r));
	}

	public boolean robotWantsToLeaveCharging(long time, AGVAgent agvAgent) {
		agvAgent.getChargeTime();
		return true;
	}

	public void robotLeftChargingStation(AGVAgent r, Point point, TimeLapse timeLapse) throws SimulationException {
		ChargingStation cs = chargingStations.get(point);
		if (cs == null) {
			throw new SimulationException("Couldn't find chargingstation for point " + point);
		}
		this.eventDispatcher.dispatchEvent(
				new DAREvent(DAREventType.AGV_LEAVING_CHARGING_STATION, r.getAndResetChargeTime(), null, null, r));
		int ID = r.co.getChargingStationUtil().ID;
		cs.AGVLeavingChargingStation(r, ID, timeLapse);
	}

	public void dropCarParcel(AGVAgent robot, CarPackage parcel, long time) {
		this.eventDispatcher.dispatchEvent(new DAREvent(DAREventType.DROP_CAR, time, null, parcel, robot));
		this.recountTasks();
		// Unregister the parcel from the simulator
		this.sim.unregister(parcel);
	}

	public boolean canDeliverCar(AGVAgent agv, CarPackage car, long time) {
		Point position = agv.getPosition().get();
		BasicTask task = car.task;

		// If robot is not on the location of the task, cannot deliver.
		if (!position.equals(task.endPosition)) {
			return false;
		}

		StorageAgent resourceAgent = this.storageAgents.get(position);
		// check if on resourceAgent and not on random infrastructure agent
		if (resourceAgent == null)
			resourceAgent = this.ExportAgents.get(position);

		if (resourceAgent == null)
			return false;

		if (resourceAgent.canDeliver(agv.ID, task.ID, time))
			return true;

		return false;
	}

	public CarPackage createCarParcel(BasicTask ts, Point startPosition, Point endPosition, long time, AGVAgent agv) {

		if (verbose) {
			System.out.println("DARModel.createSingleCarParcel for task " + ts + " at time " + time);
		}
		ParcelDTO pdto = Parcel.builder(startPosition, endPosition).buildDTO();
		DeliveryTask task = tasks.get(ts.ID);
		CarPackage parcel = new CarPackage(pdto, ts, time, ts.carId);
		if (task instanceof DeliveryTask) {
			this.eventDispatcher.dispatchEvent(new DAREvent(DAREventType.PICKUP_TASK, time, ts, parcel, agv));
			System.out.println("Picked up singleTask " + ts);
		} else {
			this.eventDispatcher.dispatchEvent(new DAREvent(DAREventType.PICKUP_TEMP_TASK, time, ts, parcel, agv));
			System.out.println("Picked up MovementTask " + ts);
		}

		this.sim.register(parcel);

		return parcel;
	}

	public Set<Point> getAllTaskPoints() {
		return taskPoints;
	}

	public Collection<DeliveryTask> getAllTasks() {
		return tasks.values();
	}

	public Collection<StorageAgent> getAllStoringAgents() {
		return this.storageAgents.values();
	}

	public Collection<ChargingStation> getChargingStations() {
		return this.chargingStations.values();
	}

	public InfrastructureAgent getAgentAt(Point startNode) throws SimulationException {
		InfrastructureAgent potential = infrastructureAgents.get(startNode);
		if (potential != null) {
			if (!potential.position.equals(startNode)) {
				System.out.println(" not equals for point " + startNode);
			}
			return potential;
		}
		potential = chargingStations.get(startNode);
		if (potential != null) {
			if (!potential.position.equals(startNode)) {
				System.out.println(" not equals for point " + startNode);
			}
			return potential;
		}
		potential = storageAgents.get(startNode);
		if (potential != null) {
			if (!potential.position.equals(startNode)) {
				System.out.println(" not equals for point " + startNode);
			}
			return potential;
		}
		potential = ImportAgents.get(startNode);
		if (potential != null) {
			if (!potential.position.equals(startNode)) {
				System.out.println(" not equals for point " + startNode);
			}
			return potential;
		}
		potential = ExportAgents.get(startNode);
		if (potential != null) {
			if (!potential.position.equals(startNode)) {
				System.out.println(" not equals for point " + startNode);
			}
			return potential;
		}
		throw new SimulationException("Couldnt find a Node to start on");
	}

	public void unregisterForCharging(AGVAgent r) {
		try {
			// roadModel.removeObject(r);
			if (this.roadModel.unregister(r)) {
				System.out.println("Removed from roadmodel " + r.ID);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	public AGVAgent registerForCharging(AGVAgent a, InfrastructureAgent ia, TimeLapse time) {
		// A ticklistener can't be removed in rinSim and another be added that has
		// o.equals(o2) = true
		// This is because the tickListeners are removed only at the end of the tick
		// cycle
		// ATM we avoid this by making a new AGV with a new ID, this allows us to remove
		// the previous agv
		// This is not optimal but it works
		sim.removeTickListener(a);
		sim.unregister(a);
		AGVAgent agv = a.createFalseCopy(ia, SimulationSettings.AGV_SPEED, time);
		agv.initDARUser(this);
		sim.register(agv);
		return agv;
	}

	// Maybe just remove the AGV when this occurs
	// Faulty agv -> no longer present in Simulation
	public void AGVNeedsRescue(AGVAgent agv, TimeLapse time) {
		new SimulationException("AGV is stuck: " + agv).printStackTrace();
		//AGVAgent falseCopy = agv.createFalseCopy(Agent, agv.getSpeed(), time);
		this.eventDispatcher.dispatchEvent(new DAREvent(DAREventType.AGV_STUCK, time.getTime(), null, null, agv));
		agv.active=false;
		sim.unregister(agv);
		System.err.println("REMOVED AGV AS IT WAS STUCK " + agv);
		//sim.register(falseCopy);

	}

	public void registerTickListener(TickListener csu) {
		sim.register(csu);
	}

	public boolean inRestingPlaces(Point position) {
		return this.restingPlaces.contains(position);
	}

	public void AGVhasNoBattery(AGVAgent agvAgent, TimeLapse time) {
		this.eventDispatcher.dispatchEvent(new DAREvent(DAREventType.AGV_STUCK, time.getTime(), null, null, agvAgent));

	}

	public boolean noTaskActive() {
		return this.tasks.isEmpty();
	}

	public void removePointFromTaskList(StorageAgent storageAgent) {
		this.taskPoints.remove(storageAgent.getPosition().get());
		restingPlaces.add(storageAgent.position);
	}

	public void removeSingleTaskFromTaskList(DeliveryTask ts, StorageAgent storageAgent, boolean hasNoReturningAgents) {
		if (tasks.get(ts.ID) != null) {
			// we found a tasks with the given ID
			// This means this was a tasks made by the DarModel and not by the StorageAgent
			// So we should remove this task from the tasks map because it was pickedup

			tasks.remove(ts.ID);

			// For interdependent tasks
			// Now we look trough all other entries and if there is no other tasks with the
			// same location we can remove the location from the taksLocations

			if (hasNoReturningAgents) {
				this.checkTaskBeginPositions(ts);
				this.checkBeginPositions(ts);
				this.checkEndPostitions(ts);
			}
		}
	}

	private void checkTaskBeginPositions(DeliveryTask ts) {
		for (Entry<Integer, DeliveryTask> entry : tasks.entrySet()) {
			if (entry.getValue().beginPosition.equals(ts.beginPosition)) {
				// we found another tasks that starts on the starlocation
				// So we dont remove the taskLocation from the set
				return;
			}
		}

		// we did NOT found another tasks that starts on the starlocation
		// So we remove the taskLocation from the set
		taskPoints.remove(ts.beginPosition);
	}

	// Input/output nodes can not be restnodes
	private void checkBeginPositions(DeliveryTask ts) {
		for (Entry<Integer, DeliveryTask> entry : tasks.entrySet()) {
			if (entry.getValue().beginPosition.equals(ts.beginPosition)
					&& entry.getValue().beginPosition.equals(ts.endPosition)) {
				// we found another tasks that starts or end on the starlocation
				// So we dont add the restingplace to the list
				return;
			}
		}

		// we did NOT found another tasks that starts on the starlocation
		// So we remove the taskLocation from the set
		restingPlaces.add(ts.beginPosition);
	}

	// Input/output nodes can not be restnodes
	private void checkEndPostitions(DeliveryTask ts) {
		for (Entry<Integer, DeliveryTask> entry : tasks.entrySet()) {
			if (entry.getValue().endPosition.equals(ts.endPosition)
					&& entry.getValue().endPosition.equals(ts.beginPosition)) {
				// we found another tasks that end or start on the endLocation
				// So we dont add the restingplace to the list
				return;
			}
		}
		restingPlaces.add(ts.endPosition);
	}

	// This needs to be changed for the simulations
	// A single node may receive multiple singletasks in 1 go
	// But can no longer receive additional tasks when it already received these
	// tasks
	// And a node can only receive tasks once a simulation
	// We wont send multiple agvs to a single node multiple times a day
	// A task can also only go from an input node to a storage unit or from a
	// storage node to an output node
	// some input/output nodes resemble workshops
	// some resemble batchJobs
	// NEXT
	public int createSingleTasks(RandomGenerator rng, long time, int maxToMakeTasks) {
		if (rng.nextBoolean()) {
			return createTask(rng, time, this.ImportAgents.values().toArray(), this.storageAgents.values().toArray(),
					true, 1, maxToMakeTasks, 6);
		} else {
			return createTask(rng, time, this.storageAgents.values().toArray(), this.ExportAgents.values().toArray(),
					false, 2, maxToMakeTasks, 0);
		}
	}

	private int createTask(RandomGenerator rng, long time, Object[] from, Object[] to, boolean followOrder, int divider,
			int maxToMakeTasks, int minAmmount) {
		StorageAgent startPoint;
		StorageAgent endPoint;
		int tries = 0;
		int nextInt;
		do {
			try {
				nextInt = rng.nextInt(from.length);
			} catch (NotStrictlyPositiveException e) {
				return 0;
			}
			startPoint = (StorageAgent) from[nextInt];
			if (tries > 5)
				return 0;
			tries++;
		} while (this.taskPoints.contains(startPoint.position) || !startPoint.hasCars()
				|| (startPoint.carIDs.size() <= minAmmount));

		Object[] carIds = startPoint.getCarIds().toArray();
		int length = carIds.length;
		int selectedCars = length / divider;

		int ammountOfTasks = 0;
		try {
			ammountOfTasks = 1 + rng.nextInt(selectedCars);
		} catch (NotStrictlyPositiveException e) {
			return 0;
		}
		List<DeliveryTask> tempTasks = new ArrayList<DeliveryTask>();
		Set<String> tempCars = new HashSet<String>();

		int priorityMultiply = 1;
		if (followOrder)
			priorityMultiply = SimulationSettings.HEUR_PREFER_BATCH_JOBS;

		for (int i = 0; i < ammountOfTasks; i++) {
			do {
				try {
					nextInt = rng.nextInt(to.length);
				} catch (NotStrictlyPositiveException e) {
					return 0;
				}
				endPoint = (StorageAgent) to[nextInt];
				if (tries > 5)
					return 0;
				tries++;
			} while (!endPoint.canStoreCars());

			String carId = null;
			if (followOrder) {
				carId = (String) carIds[i];
			} else {
				do {
					carId = (String) carIds[rng.nextInt(length)];
				} while (tempCars.contains(carId));
			}
			tempCars.add(carId);

			int MinTenTillEnd = (rng.nextInt(4) + 3);
			long endTime = time + MinTenTillEnd * 10 * 60 * 1000;
			// add priority factor

			int priority = 1 + rng.nextInt(10);
			priority = (int) priorityMultiply * priorityMultiply;

			DeliveryTask st = new DeliveryTask(startPoint.getPosition().get(), endPoint.getPosition().get(), carId, time,
					endTime, priority, p.h1, p.m30, p.m15, p.ur);
			tasks.put(st.ID, st);
			tempTasks.add(st);
			sim.register(st);
			System.out.println("[TASK_INFO] DARModel.createSingleTask: " + st + " at time " + time);
			this.eventDispatcher.dispatchEvent(new DAREvent(DAREventType.NEW_TASK, time, st, null, null));
			if (i >= maxToMakeTasks)
				break;
		}
		startPoint.registerTask(tempTasks);
		this.recountTasks();

		return tempTasks.size();
	}

	private void recountTasks() {
		int tasks = 0;
		for (Point p : this.taskPoints) {
			try {
				InfrastructureAgent ia = this.getAgentAt(p);
				tasks += ((StorageAgent) ia).getTaskAmmount();
			} catch (Exception e) {
			}
		}
		this.currentTasks = tasks;
	}

	public int taskAmmount() {
		return currentTasks;
	}

	public void addPointToTaskList(StorageAgent sa, BasicTask bt) {
		this.taskPoints.add(sa.position);
		this.restingPlaces.remove(sa.position);
		this.restingPlaces.remove(bt.endPosition);
	}

	public void tempMoveFinished(AGVAgent agv, CarPackage parcel, TimeLapse time) {
		BasicTask task = parcel.task;
		this.eventDispatcher.dispatchEvent(new DAREvent(DAREventType.END_TEMP_MOVE, time.getTime(), task, parcel, agv));
		currentTasks -= 1;
	}

	// removing taskPoints doenst work like wanted, takspoints are removed
	// while there are still tasks on said points
	public void deliverCar(AGVAgent agv, CarPackage parcel, TimeLapse time) {
		BasicTask task = parcel.task;
		task.deliver(parcel.carID, agv.getPosition().get());
		task.completionTime = time.getTime();
		this.eventDispatcher.dispatchEvent(new DAREvent(DAREventType.END_TASK, time.getTime(), task, parcel, agv));
		currentTasks -= 1;
		this.tasks.remove(task.ID);
		try {
			this.roadModel.removeObject(task);
		} catch (Exception e) {
		}
		for (DeliveryTask st : tasks.values()) {
			if (st.beginPosition == task.beginPosition && st.ID != task.ID)
				return;
		}
		taskPoints.remove(task.beginPosition);
	}

	public void addPointToRestPlace(Point position) {
		if (ImportAgents.containsKey(position) || ExportAgents.containsKey(position))
			return;

		for (BasicTask st : this.tasks.values()) {
			if (st.endPosition.equals(position))
				return;
		}

		this.restingPlaces.add(position);
	}

	public void addCarToImport(RandomGenerator rng) {
		Object[] iS = this.ImportAgents.values().toArray();
		ImportStorage chosen = null;
		do {
			chosen = (ImportStorage) iS[rng.nextInt(iS.length)];
		} while (!chosen.canStoreCars());

		chosen.addCarToImport(chosen.getNewCarId());
	}

	public Collection<ImportStorage> getAllImport() {
		return this.ImportAgents.values();
	}

	public void removeDeadLock(TimeLapse time, DeadLocks d) {
		this.eventDispatcher.dispatchEvent(new DLEvent(DAREventType.DEADLOCK_FINISHED, d, time.getTime()));

	}

	public void addDeadLock(TimeLapse time, DeadLocks d) {
		this.eventDispatcher.dispatchEvent(new DLEvent(DAREventType.DEADLOCK_ADDED, d, time.getTime()));

	}

	public List<AGVAgent> getAllAGV() {
		return this.AGV;
	}

	public void betterPathFor(AGVAgent AGV, TimeLapse time) {
		this.eventDispatcher
				.dispatchEvent(new DAREvent(DAREventType.BETTER_PATH_SELECTED, time.getTime(), null, null, AGV));
	}

	public void betterTaskFor(AGVAgent AGV, TimeLapse time) {
		this.eventDispatcher
				.dispatchEvent(new DAREvent(DAREventType.BETTER_TASK_SELECTED, time.getTime(), null, null, AGV));
	}

	public void setDeadLockSolver(DeadlockSolver dl) {
		this.dl = dl;
	}

	public DeadlockSolver getDeadLockSolver() {
		return this.dl;
	}
}
