package agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import AGV.Battery;
import ants.BasicAnt;
import ants.BroadCastAnt;
import ants.DeadLockDetectionAnt;
import ants.findRestingPlaceAnt;
import ants.path.ChargingExplorationAnt;
import ants.path.ChargingIntentionAnt;
import ants.path.ExplorationAnt;
import ants.path.PathExplorationAnt;
import ants.path.PathIntentionAnt;
import ants.path.TaskExplorationAnt;
import ants.path.TaskIntentionAnt;
import comperators.ChargingExplorationComparator;
import comperators.HamilCSComparator;
import comperators.TaskComparator;
import comperators.distanceComparator;
import deadlocks.DeadLockedEdge;
import deadlocks.DeadlockSolver;
import graphs.AStar;
import reservations.DeadLockRouteReservation;
import reservations.RouteReservation;
import reservations.TaskOptions;
import simulator.SimulationSettings;
import simulator.SimulationException;
import tasks.HoldingTask;
import tasks.DeliveryTask;

public class DecisionAGV extends AGVAgent {
	List<Point> previousSelectedTasks = new ArrayList<Point>();;

	private boolean hasShelter = false;

	private boolean noTasksActive = false;

	// just a value to check for ammount of reservationsExtensions for stats
	@SuppressWarnings("unused")
	private int extendReservation = 0;

	public DecisionAGV(int id, Point startNode, double agvSpeed, int alternativePathsExploration,
			int explorationRefreshTime, int intentionRefreshTime, boolean verbose,
			InfrastructureAgent infrastructureAgent, int currentBattery, DeadlockSolver dl1) {
		super(id, startNode, agvSpeed, alternativePathsExploration, explorationRefreshTime, intentionRefreshTime,
				verbose, infrastructureAgent, currentBattery, dl1);
	}

	public DecisionAGV(int i, double aGVSpeed, boolean verbose, InfrastructureAgent agent, Battery battery, DeadlockSolver dl1) {
		super(i, aGVSpeed, verbose, agent, battery, dl1);
	}

	public DecisionAGV createFalseCopy(InfrastructureAgent agent, double AGVSpeed, TimeLapse time) {
		DecisionAGV agv = new DecisionAGV(ID + 1000, AGVSpeed, verbose, agent, battery, dl);
		if (this.hasParcel()) {
			agv.addParcel(this.parcel, time);
		}
		return agv;
	}

	protected void refreshChargingIntention(TimeLapse time) {
		if (doingCharge && co != null) {
			List<Point> path = getPathFromReservations(reservations);
			if (endPoint.isPresent()) {
				ChargingIntentionAnt ca = new ChargingIntentionAnt(path, this, endPoint.get(), reservations, co);
				ca.handleAnt(co.getChargingStation(), time);
			}
		}
	}

	private void lookForBetterChargingPoint(TimeLapse time) throws SimulationException {
		if (this.co == null) {
			lookForBestChargingStation(time);
			return;
		} else {
			if (this.co.getChargingStation().position.equals(this.getPosition().get())) {
				// we are already on a charging station, we just look for a better ChargingUtil
				ArrayList<Point> path = new ArrayList<Point>();
				path.add(this.getPosition().get());
				ChargingExplorationAnt ant = new ChargingExplorationAnt(path, this);
				ant.handleAnt(this.co.getChargingStation(), time);
			} else {
				lookForBestChargingStation(time);
			}
		}
	}

	private void lookForBestChargingStation(TimeLapse timeLapse) throws SimulationException {
		Point position = this.getPosition().get();
		int waitTime = SimulationSettings.Minimum_Wait_Time;
		// look at this, it should be able to directly access the ChargingUtils if
		// it is already on the node
		// Do i want to send an explorationAgent to all the chargingstations?
		if (position.equals(new Point(-5, -5))) {
			if (this.co == null)
				throw new SimulationException("ON A CHARGINGSTATION WITH NO CHARGINGRESERVARTION");
			List<Point> path = new ArrayList<>();
			path.add(this.co.getChargingStation().position);
			ChargingExplorationAnt ant = new ChargingExplorationAnt(path, this);
			sendAnt(ant, timeLapse);

		} else {
			List<ChargingStation> chargingStations = new ArrayList<ChargingStation>(darModel.getChargingStations());
			List<List<Point>> paths = null;
			chargingStations.sort(new HamilCSComparator(position));

			int accepted = 0;
			for (ChargingStation cs : chargingStations) {
				if (accepted == 3)
					break;
				if (cs.CUhasPlace(this)) {
					accepted += 1;
					paths = AStar.getInstance().getAlternativePaths(position, cs.position, rng, staticGraph);
					for (List<Point> path : paths) {
						// AStar.calculateDistanceFromPath(path);
						ChargingExplorationAnt ant = new ChargingExplorationAnt(path, this);
						sendExplorationAnts++;
						waitTime = Math.max(waitTime, path.size() * 2);
						sendAnt(ant, timeLapse);
					}
				}
			}
		}

		if (sendExplorationAnts == 0) {
			if (isOnNode && nodeOn.canRestHere(this)) {
				this.waiting = true;
				this.timeToWait = SimulationSettings.RestWait / timeLapse.getTickLength();
				this.nodeOn.extendReservationTime(timeLapse, SimulationSettings.RestWait, this);
			} else {
				this.seekShelter(timeLapse);
			}
		}
		waitingForExplorationAnts = waitTime;

	}

	protected void intendPath(TimeLapse time) {
		if (!this.intention.isPresent() || !endPoint.isPresent())
			return;

		PathIntentionAnt pa = new PathIntentionAnt(new LinkedList<Point>(intention.get()), this, endPoint.get(), null);

		// if the intention size = 0 then we are already on the node, so we just ask our
		// reservation from the node on which we are located
		// and extend our reservation
		if (intention.get().size() == 0) {
			if (verbose)
				System.out.println("[INFO] AGV is already on the right location");

			nodeOn.extendReservation(time, 2, this);
			// To see if the reservations succeeded we only need the last reservation with
			// the right endgoal to not be equal to null
			RouteReservation currentReservation = nodeOn.getCurrentReservation(time);
			reservations = new ArrayList<RouteReservation>();
			reservations.add(currentReservation);
		} else {
			sendIntentionAnts++;
			if (isOnNode)
				pa.handleAnt(nodeOn, time);
			else {
				sendAnt(pa, time);
				waitingForIntentionAnts = (int) (intention.get().size() * 2);
			}
		}

		if (doingPickUp)
			refreshTaskIntention(time, pa.getPath());
	}

	private void lookForBestTasks(TimeLapse time) {
		// We cant decide what the best task is, so we send TaskExplorationAnts to all
		// nearby tasks
		long distanceToConsider = SimulationSettings.TASK_DISTANCE_TO_CONSIDER_START;
		int distanceFactor = SimulationSettings.TASK_CONSIDERING_FACTOR;
		int taskAmmount = SimulationSettings.TASK_AMMOUNT_TO_CONSIDER;
		Set<Point> taskStarts = darModel.getAllTaskPoints();
		Set<Point> tasksWeConsider = new HashSet<Point>();
		Set<Point> lookedAtTask = new HashSet<Point>();

		do {
			for (Point pt : taskStarts) {
				try {
					if (!lookedAtTask.contains(pt)) {
						if (!previousSelectedTasks.contains(pt) && !tasksWeConsider.contains(pt)
								&& ((StorageAgent) darModel.getAgentAt(pt)).lookingForTaskConsumers()) {
							double distance = Point.distance(this.getPosition().get(), pt);
							if (distance < distanceToConsider) {
								tasksWeConsider.add(pt);
								lookedAtTask.add(pt);
							}
						}
					}
				} catch (Exception e) {
				}
			}
			distanceToConsider *= distanceFactor;
		} while (tasksWeConsider.size() < taskAmmount && taskStarts.size() != tasksWeConsider.size()
				&& distanceToConsider < battery.currentCap / 10);

		if (tasksWeConsider.isEmpty()
				&& this.getRemainingBatteryCapacityPercentage() <= SimulationSettings.BATTERY_LOAD_PERCENTAGE) {
			tasksNotAvailableDueToBattery = true;
			return;
		}

		previousSelectedTasks.clear();
		previousSelectedTasks.addAll(tasksWeConsider);

		// Now that we have the points we think are reachable that may have a task
		// maybe also limit the amount of paths here?
		int waitTime = SimulationSettings.Minimum_Wait_Time;
		sendExplorationAnts = 0;
		for (Point pt : tasksWeConsider) {
			List<List<Point>> paths = AStar.getInstance().getAlternativePaths(this.getPosition().get(), pt, rng,
					staticGraph);
			for (List<Point> path : paths) {
				waitTime = Math.max(waitTime, path.size() * 2);
				// should I keep this stored somewhere?
				BasicAnt ba = new TaskExplorationAnt(path, this);
				sendAnt(ba, time);
				sendExplorationAnts++;
			}
		}
		waitingForExplorationAnts = waitTime;
	}

	private boolean checkTasksAvailable() {

		return !darModel.getAllTasks().isEmpty();

		// WE used to look if a path was available but we just assume this is the case
		// because this caused to heave slowdowns
		/*
		 * double maxDistance = battery.currentCap; for (SingleTask st :
		 * darModel.getAllTasks()) { ArrayList<Point> dests = new ArrayList<Point>();
		 * dests.add(this.getPosition().get()); dests.add(st.beginPosition);
		 * dests.add(st.endPosition);
		 * 
		 * List<Point> path = AStar.getInstance().getAlternativePathsConcat(dests, rng,
		 * staticGraph);
		 * 
		 * if (path.isEmpty()) { // we didnt find a path so we dont check distance and
		 * dont add break; }
		 * 
		 * long distance = AStar.calculateDistanceFromPath(path);
		 * 
		 * if (distance <= maxDistance) { // If we find 1 task that we can reach we know
		 * we can reach atleast 1 task so we // can look for tasks return true; } }
		 * return false;
		 */
	}

	public boolean lookForPath(TimeLapse time, boolean waiting, ListenableGraph<LengthData> graph,
			boolean checkForDependentAnts) {
		// In this case we dont have an intention present, just an endPoint we want to
		// reach
		// the next node should be the next reservation

		if (isOnNode) {
			Collection<Point> con = graph.getOutgoingConnections(this.getPosition().get());
			if (con.size() == 0) {
				new SimulationException(
						"Trying to find a path from a node that has no exits " + this.getPosition().get())
								.printStackTrace();
				return false;
			}
		}

		Point node;
		if (movingFailed) {
			node = this.getPosition().get();
		} else if (movingTo != null)
			node = movingTo;
		else if (isOnNode) {
			node = this.getPosition().get();
		} else {
			node = new Point(roundToClosestEven(this.getPosition().get().x),
					roundToClosestEven(this.getPosition().get().y));
		}

		if (waiting) {
			this.nodeOn.extendReservationTime(time, 15 * 1000, this);
		}

		if (!endPoint.isPresent()) {
			if (verbose)
				System.out.println("[ERROR] Endpoint is not present and looking for a path to said endPoint");
			return false;
		}

		int waitTime = SimulationSettings.Minimum_Wait_Time;
		sendExplorationAnts = 0;
		List<List<Point>> paths = AStar.getInstance().getAlternativePaths(node, endPoint.get(), rng, graph);

		if (paths == null || paths.isEmpty()) {
			return false;
		}
		for (List<Point> path : paths) {
			// look into
			if (path.size() == 1 && this.getPosition().get().equals(path.get(0))) {
				// this means we are already on the correct location so we dont need to send out
				// a path
				// Instead we can just take the last reservation (and extend it and set the
				// intention by just loading the path)
				reservations = new ArrayList<>();
				RouteReservation current = nodeOn.getCurrentReservation(time);
				if (current == null) {
					nodeOn.firstReservation(this, time.getTime());
				}
				if (waiting) {
					nodeOn.extendReservationTime(time, SimulationSettings.SearchTime, this);
				}
				reservations.add(nodeOn.getCurrentReservation(time));
				setIntentionFromPath(path);
				if (!(this.doingCharge || this.hasParcel() || this.forcedMove))
					this.doingPickUp = true;

				return false;
			}

			waitTime = Math.max(waitTime, path.size() * 2);
			// should I keep this stored somewhere?

			BasicAnt ba = new PathExplorationAnt(path, this, null, endPoint.get());
			ba.sendDependingAnts = checkForDependentAnts;
			try {
				sendAnt(ba, darModel.getAgentAt(node), time);
			} catch (SimulationException e) {
				System.err.println("AGV" + this.ID % 1000 + " IS STUCK, It cannot find a node to send a message to");
				e.printStackTrace();
				if (!this.isOnNode && reservations == null) {
					System.err.println("Stuck on a connection between nodes without reservations");
					this.darModel.AGVNeedsRescue(this, time);
					return false;
				}
				return false;
			}
			if (waiting)
				sendExplorationAnts++;
		}
		if (waiting)
			waitingForExplorationAnts = waitTime;

		return true;
	}

	protected void refreshTaskIntention(TimeLapse timelapse) {
		if (this.pickUpTaskOptions.isEmpty())
			return;

		if (this.reservations == null) {
			refreshTaskIntention(timelapse, AStar.getInstance().getAlternativePaths(1, this.getPosition().get(),
					this.pickUpTaskOptions.get(0).getBeginPoint(), rng, staticGraph).get(0));

		} else
			refreshTaskIntention(timelapse, getPathFromReservations(this.reservations));
	}

	private void refreshTaskIntention(TimeLapse timeLapse, List<Point> path) {
		if (endPoint.isPresent() && parcel == null && !pickUpTaskOptions.isEmpty()) {
			try {
				InfrastructureAgent endAgent = darModel.getAgentAt(pickUpTaskOptions.get(0).getBeginPoint());
				TaskIntentionAnt taskIntentionAnt = new TaskIntentionAnt(path, this, endPoint.get(), this.reservations,
						new ArrayList<TaskOptions>(this.pickUpTaskOptions));
				taskIntentionAnt.handleAnt(endAgent, timeLapse);
			} catch (SimulationException e) {
				e.printStackTrace();
			}
		}

	}

	private void refreshPathIntention(TimeLapse time) {
		if (!this.endPoint.isPresent())
			return;

		if (this.reservations == null)
			return;

		PathIntentionAnt pa = new PathIntentionAnt(getPathFromReservations(this.reservations), this, endPoint.get(),
				new ArrayList<RouteReservation>(reservations));

		for (RouteReservation rr : this.reservations) {
			rr.resetEvaporation();
		}

		if (!reservations.isEmpty()) {
			nodeOn.refreshCurrentReservation(time);
			pa.handleAnt(reservations.get(0).responsibleNode, time);
		}
	}

	// add these should not look for a nearby chargingstation as this takes to
	// long
	// We just assume we can get there without a problem
	private void lookForBetterPath(TimeLapse time) {
		if (reservations == null)
			return;
		if (this.reservations.isEmpty())
			return;

		if (isOnNode && !explorationAnts.isEmpty()) {
			this.handlePathExplorationAnt(null, time);
			return;
		}

		if (verbose)
			System.out.println("[INFO-RECONSIDERING] looking for better path started");
		lookForPath(time, false, staticGraph, false);
	}

	protected void lookForBetterTasks(TimeLapse time) {
		if (parcel != null) {
			return;
		}

		if (!this.endPoint.isPresent()) {
			this.lookForBestTasks(time);
			return;
		}

		if (isOnNode && !explorationAnts.isEmpty()) {
			this.handleTaskExplorationAnt(null, time);
			return;
		}

		// We cant decide what the next best task is, so we send TaskExplorationAnts to
		// all
		// nearby tasks
		long distanceToConsider = SimulationSettings.TASK_DISTANCE_TO_CONSIDER_START;
		int distanceFactor = SimulationSettings.TASK_CONSIDERING_FACTOR;
		int taskAmmount = SimulationSettings.TASK_AMMOUNT_TO_CONSIDER;
		Set<Point> taskStarts = darModel.getAllTaskPoints();
		Set<Point> tasksWeConsider = new HashSet<Point>();
		Set<Point> lookedAtTask = new HashSet<Point>();

		do {
			for (Point pt : taskStarts) {
				try {
					if (!lookedAtTask.contains(pt) && !endPoint.get().equals(pt)) {
						if (!previousSelectedTasks.contains(pt) && !tasksWeConsider.contains(pt)
								&& ((StorageAgent) darModel.getAgentAt(pt)).lookingForTaskConsumers()) {
							double distance = Point.distance(this.getPosition().get(), pt);
							if (distance < distanceToConsider) {
								tasksWeConsider.add(pt);
								lookedAtTask.add(pt);
							}
						}
					}
				} catch (Exception e) {
				}
			}
			distanceToConsider *= distanceFactor;
		} while (tasksWeConsider.size() < taskAmmount && taskStarts.size() != tasksWeConsider.size()
				&& distanceToConsider < battery.currentCap / 10);

		if (tasksWeConsider.isEmpty()
				&& this.getRemainingBatteryCapacityPercentage() <= SimulationSettings.BATTERY_LOAD_PERCENTAGE) {
			tasksNotAvailableDueToBattery = true;
			return;
		}

		previousSelectedTasks.clear();
		previousSelectedTasks.addAll(tasksWeConsider);

		// Now that we have the points we think are reachable that may have a task
		int waitTime = SimulationSettings.Minimum_Wait_Time;
		sendExplorationAnts = 0;
		for (Point pt : tasksWeConsider) {
			List<List<Point>> paths = AStar.getInstance().getAlternativePaths(this.getPosition().get(), pt, rng,
					staticGraph);
			for (List<Point> path : paths) {
				waitTime = Math.max(waitTime, path.size() * 2);
				// should I keep this stored somewhere?
				BasicAnt ba = new TaskExplorationAnt(path, this);
				sendAnt(ba, time);
				sendExplorationAnts++;
			}
		}
		waitingForExplorationAnts = waitTime;
	}

	public void handleChargingIntentionAnt(ChargingIntentionAnt chargingIntentionAnt, TimeLapse timeLapse) {
		if (chargingIntentionAnt.getAccepted()) {
			this.pickUpTaskOptions.clear();
			doingPickUp = false;
			doingCharge = true;
			co = chargingIntentionAnt.getchargingOptions();
			if (reservations == null) {
				this.endPoint = Optional.of(chargingIntentionAnt.getEndPoint());
				this.lookForPath(timeLapse, true, staticGraph, false);
			}
		} else {
			doingPickUp = false;
			doingCharge = false;
			this.endPoint = Optional.absent();
			reservations = null;
			this.co = null;
		}
	}

	public void handleChargingExplorationAnt(ChargingExplorationAnt ant, TimeLapse timeLapse) {
		// we are still waiting for explorationAnts to return
		// We should not do anything but save the ants that returned
		if (ant != null) {
			explorationAnts.add(ant);
		} else {
			if (this.explorationAnts.isEmpty())
				return;
		}
		if (!waitingForAnts()) {
			// If we got here we assume we are no longer waiting for ants because all the
			// ants that were send have arrived or the timeout occurred
			if (doingCharge && reservations != null) {
				// we are already on our way to pickup a task, so we compare the tasks to the
				// task we already have selected
				if (isOnNode)
					chooseBetterCharging(timeLapse);
			} else {
				// Choose the best task from the explorationAnts
				chooseBestCharging(timeLapse);
			}
		}
	}

	private void chooseBetterCharging(TimeLapse time) {
		ArrayList<ExplorationAnt> tempAnts = new ArrayList<ExplorationAnt>();
		for (ExplorationAnt ant : explorationAnts) {
			if (ant instanceof ChargingExplorationAnt) {
				tempAnts.add((ChargingExplorationAnt) ant);
			}
		}

		explorationAnts.removeAll(tempAnts);

		if (tempAnts.isEmpty()) {
			new SimulationException("No chargingExlorationAnt found while the chooseBestCharging Method was called")
					.printStackTrace();
			return;
		}

		// Add the current reservations also to the list
		ChargingExplorationAnt tempAnt = new ChargingExplorationAnt(this.getPathFromReservations(this.reservations),
				this, reservations, co);
		tempAnts.add(tempAnt);

		tempAnts.removeAll(this.getBatteryConstrainedPaths(tempAnts));

		ArrayList<ChargingExplorationAnt> chargingTemp = new ArrayList<ChargingExplorationAnt>();
		for (ExplorationAnt ant : tempAnts) {
			chargingTemp.add((ChargingExplorationAnt) ant);
		}
		chargingTemp.sort(new ChargingExplorationComparator());

		ChargingExplorationAnt bestAnt = (ChargingExplorationAnt) chargingTemp.get(0);
		if (bestAnt.equals(tempAnt)) {
			// we found no better option
			return;
		}

		List<Point> tempPath = bestAnt.getPath();
		ChargingIntentionAnt pa = new ChargingIntentionAnt(tempPath, this, endPoint.get(), bestAnt.reservationsCopy(),
				bestAnt.chargingOptions());
		sendIntentionAnts++;
		sendAnt(pa, time);

		waitingForIntentionAnts = (int) (tempPath.size() * 1.5);
	}

	private void chooseBestCharging(TimeLapse timeLapse) {
		ArrayList<ChargingExplorationAnt> tempAnts = new ArrayList<ChargingExplorationAnt>();
		for (ExplorationAnt ant : explorationAnts) {
			if (ant instanceof ChargingExplorationAnt) {
				tempAnts.add((ChargingExplorationAnt) ant);
			}
		}

		explorationAnts.removeAll(tempAnts);

		if (tempAnts.isEmpty()) {
			explorationAnts.clear();
			return;
		}

		tempAnts.removeAll(this.getBatteryConstrainedPaths(tempAnts));
		if (tempAnts.isEmpty()) {
			darModel.AGVNeedsRescue(this, timeLapse);
			return;
		}

		tempAnts.sort(new ChargingExplorationComparator());

		for (ChargingExplorationAnt bestAnt : tempAnts) {
			List<Point> tempPath = bestAnt.getPath();

			if (bestAnt.chargingOptions().getChargingStationUtil().hasPlace(this)) {

				if (tempPath == null) {
					System.err.print(new SimulationException("Path = NULL"));
					return;
				} else {
					this.endPoint = Optional.of(bestAnt.getEndPoint());
					ChargingIntentionAnt pa = new ChargingIntentionAnt(tempPath, this, endPoint.get(),
							bestAnt.reservationsCopy(), bestAnt.chargingOptions());
					pa.handleAnt(bestAnt.chargingOptions().getChargingStation(), timeLapse);
					return;
				}
			}
		}

		// No suitable chargingPlace found
		// We go in rest mode
		// And look again after this
		if (isOnNode && nodeOn.canRestHere(this)) {
			this.waiting = true;
			this.timeToWait = SimulationSettings.RestWait / timeLapse.getTickLength();
			this.nodeOn.extendReservationTime(timeLapse, SimulationSettings.RestWait, this);
		} else {
			this.seekShelter(timeLapse);
		}
	}

	public void handlePathIntentionAnt(PathIntentionAnt pathIntentionAnt, TimeLapse timeLapse) {
		if (verbose)
			System.out.println("[INFO] AGV Received PathIntentionAnt");
		
		if (this.ID % 1000 == 6)
			System.out.print("");
		
		if (!pathIntentionAnt.accepted) {
			if (isOnNode) {
				reservations = null;
				intention = Optional.absent();
				endPoint = Optional.absent();
			} else {
				resetReservation = true;
			}
			return;
		}
		List<RouteReservation> tempReservations = pathIntentionAnt.reservationsCopy();
		// To see if the reservations succeeded we only need the last reservation with
		// the right endgoal to not be equal to null

		for (RouteReservation rr : tempReservations) {
			if (rr instanceof DeadLockRouteReservation) {
				if (isOnNode) {
					reservations = null;
					intention = Optional.absent();
					endPoint = Optional.absent();
				} else {
					resetReservation = true;
				}
				return;
			}
			if(rr==null) {
				System.err.println("GOT a Null RouteReservation");
			}
		}

		RouteReservation lastReservation = tempReservations.get(tempReservations.size() - 1);

		if (!this.endPoint.isPresent())
			this.endPoint = Optional.of(pathIntentionAnt.getEndPoint());

		if (lastReservation != null && lastReservation.node.equals(this.endPoint.get())) {

			if (tempReservations.get(0).node.equals(this.getPosition().get()))
				tempReservations.remove(0);

			refreshPath = false;
			this.reservations = tempReservations;
			this.setIntentionFromPath(pathIntentionAnt.path);
			this.waiting = false;
		}
	}

	public void handlePathExplorationAnt(PathExplorationAnt pathExplorationAnt, TimeLapse timeLapse) {
		// we are still waiting for explorationAnts to return
		// We should not do anything but save the ants that returned
		if (pathExplorationAnt != null) {
			explorationAnts.add(pathExplorationAnt);
		} else {
			if (this.explorationAnts.isEmpty())
				return;
			if (pathExplorationAnt == null && !this.explorationAnts.isEmpty() && isOnNode) {
				chooseBetterPath(timeLapse);
				return;
			}
		}

		if (!waitingForAnts() || forcedMove) {

			// If we got here we assume we are no longer waiting for ants because all the
			// ants that were send have arrived or the timeout occurred

			if (intention.isPresent()) {
				// we are already on our way to pickup a task, so we compare the tasks to the
				// task we already have selected
				if (isOnNode)
					chooseBetterPath(timeLapse);
			} else {
				// Choose the best task from the explorationAnts
				chooseBestPath(timeLapse);
			}
		}
	}

	private void chooseBestPath(TimeLapse timeLapse) {
		// The best path is just the path with the lowest endTime

		ExplorationAnt bestAnt = getBestPath(timeLapse);
		if (bestAnt == null) {
			lookForPath(timeLapse, true, staticGraph, false);
			this.explorationAnts.clear();
			return;
		}
		if (!endPoint.isPresent()) {
			return;
		}

		List<Point> tempPath = bestAnt.getPath();

		if (tempPath == null) {
			System.err.print(new SimulationException("Path = NULL"));
			return;
		}
		// Now that we have a path that we want to follow we should sent intentionants
		// over said path
		// We also should resend the intention for the task just to be sure -> if we
		// have an endPoint and doing PickUp

		if (!endPoint.isPresent())
			return;

		if (doingPickUp && endPoint.isPresent() && !forcedMove && allocatedTask == null) {
			List<TaskOptions> temp = new ArrayList<TaskOptions>();
			temp.addAll(this.pickUpTaskOptions);
			TaskIntentionAnt taskIntentionAnt = new TaskIntentionAnt(tempPath, this, endPoint.get(), null, temp);
			try {
				InfrastructureAgent ia = darModel.getAgentAt(endPoint.get());
				if (!(ia instanceof StorageAgent))
					throw new SimulationException("beginPoint of task is not a storageAgent");

				if (verbose)
					System.out.println("[INFO] path was selected " + this);

				taskIntentionAnt.handleAnt(ia, timeLapse);

			} catch (SimulationException e) {
				e.printStackTrace();
			}
		}

		if (!endPoint.isPresent())
			return;

		this.setIntentionFromPath(tempPath);
		PathIntentionAnt pa = new PathIntentionAnt(tempPath, this, endPoint.get(), null);
		sendIntentionAnts++;
		sendAnt(pa, timeLapse);
		waitingForIntentionAnts = (int) (tempPath.size() * 1.5);
	}

	private void chooseBetterPath(TimeLapse timeLapse) {
		if (!this.endPoint.isPresent())
			return;
		if (this.reservations == null) {
			this.chooseBestPath(timeLapse);
			return;
		}

		if (this.reservations.isEmpty()) {
			if (verbose)
				System.out.println("[INFO-RECONSIDER] AGV " + this
						+ " is looking for a better path, but no reservations are present, so this is skipped");
			return;
		}
		// The best path is just the path with the lowest endTime
		ExplorationAnt bestFound = getBestPath(timeLapse);
		if (bestFound == null) {
			return;
		}
		List<Point> tempPath = bestFound.getPath();

		if (refreshPath) {
			System.out.println("We are obliged to look for a new better path, because of extended Reservations");
		}

		// System.out.println((getEndTime(bestFound.reservations) - timeLapse.getTime())
		// * SimulationSettings.INTENTIONS_CHANGE_FACTOR);
		// System.out.println(getEndTime(this.reservations)- timeLapse.getTime());

		if (!refreshPath && (getEndTime(bestFound.reservations) - timeLapse.getTime())
				* SimulationSettings.INTENTIONS_CHANGE_FACTOR > getEndTime(this.reservations) - timeLapse.getTime()) {
			if (verbose)
				System.out.println("[INFO-RECONSIDER] AGV " + this.ID % 1000 + " didn't find a better path");
			return;
		}

		// if (verbose)
		System.out.println("[INFO-RECONSIDER] AGV " + this.ID % 1000 + " found a better path, rerouting");
		if (doingPickUp && endPoint.isPresent() && !forcedMove) {
			TaskIntentionAnt taskIntentionAnt = new TaskIntentionAnt(tempPath, this, endPoint.get(), null,
					this.pickUpTaskOptions);
			try {
				InfrastructureAgent ia = darModel.getAgentAt(endPoint.get());
				if (!(ia instanceof StorageAgent))
					throw new SimulationException("beginPoint of task is not a storageAgent");

				taskIntentionAnt.handleAnt(ia, timeLapse);

				if (!endPoint.isPresent())
					return;

			} catch (SimulationException e) {
				e.printStackTrace();
			}
		}

		System.out.println("--INFO-- BETTER PATH SELECTED FOR " + this);
		darModel.betterPathFor(this, timeLapse);
		this.setIntentionFromPath(tempPath);
		PathIntentionAnt pa = new PathIntentionAnt(tempPath, this, endPoint.get(), null);
		sendIntentionAnts++;
		sendAnt(pa, timeLapse);

		waitingForIntentionAnts = (int) (tempPath.size() * 1.5);
	}

	private long getEndTime(List<RouteReservation> reservations) {
		if (reservations == null)
			return 0;
		else
			return reservations.get(reservations.size() - 1).startTime;
	}

	public void handleTaskExplorationAnt(TaskExplorationAnt ant, TimeLapse timeLapse) {
		// we are still waiting for explorationAnts to return
		// We should not do anything but save the ants that returned
		// Change back later on, SHOULD ALWAYS ADD AND LOOK FOR BETTER TASKS
		// if (endPoint.isPresent())
		// return;
		if (ant == null && !this.explorationAnts.isEmpty() && isOnNode && doingPickUp) {
			chooseBetterTask(timeLapse);
			return;
		}

		if (ant == null)
			return;

		if (!ant.hasDependentAnt()) {
			// we didnt find a path to the endpoint of the taskExploration W
			// We should not consider this ant in that case
			// System.err.println(ant + " Has no dependant ants " + this);
			return;
		}

		if (ant != null)
			explorationAnts.add(ant);

		if (!waitingForAnts()) {
			// If we got here we assume we are no longer waiting for ants because all the
			// ants that were send have arrived or the timeout occurred
			if (doingPickUp) {
				// we are already on our way to pickup a task, so we compare the tasks to the
				// task we already have selected
				if (isOnNode)
					chooseBetterTask(timeLapse);

			} else {
				// Choose the best task from the explorationAnts
				chooseBestTask(timeLapse);
			}
		}

	}

	protected void chooseBetterTask(TimeLapse time) {
		if (this.pickUpTaskOptions.isEmpty()) {
			this.chooseBestTask(time);
			return;
		}
		List<TaskExplorationAnt> tempAnts = new ArrayList<TaskExplorationAnt>();

		for (ExplorationAnt ant : explorationAnts) {
			// normally ants should be single type of ants that were send out but we have a
			// safeguard just in case
			if (ant instanceof TaskExplorationAnt) {
				if (ant.hasDependentAnt() || ((TaskExplorationAnt) ant).holdingTask) {
					// we didnt find a path to the endpoint of the taskExploration W
					// We should not consider this ant in that case
					tempAnts.add((TaskExplorationAnt) ant);
				} else {
					System.err.println(ant + " Has no dependant ants " + this);
				}
				// We choose the closest ant with an dependant ants endpoint that is the
				// lowest/reachable
			}
		}

		if (tempAnts.isEmpty()) {
			if (verbose)
				System.err.print("[ERROR] we didnt have any taskExplorationAnts");
			// explorationAnts.clear();
			return;
		}

		if (tempAnts.size() < 5) {
			return;
		}

		// include batterylife if no dependant is available we just assume that our
		// battery needs to be >75% at the end
		ArrayList<ExplorationAnt> toRemove = new ArrayList<ExplorationAnt>();
		for (ExplorationAnt ant : tempAnts) {

			if (this.battery.batteryLifeAfterDistance(
					ant.getTotalPathLength() * SimulationSettings.BATTERY_OVERKILL_PERCENTAGE) <= 0.15) {
				toRemove.add(ant);
			}
		}
		tempAnts.removeAll(toRemove);

		if (tempAnts.isEmpty()) {
			this.tasksNotAvailableDueToBattery = true;
			return;
		}

		tempAnts.sort(new TaskComparator());
		List<TaskOptions> tempOptions = this.pickUpTaskOptions.get(0).taskAgent.getTaskOptions(time);
		if (tempOptions.isEmpty()) {
			return;
		}
		this.pickUpTaskOptions = tempOptions;
		int highestPriority = -1;
		for (TaskOptions to : pickUpTaskOptions) {
			highestPriority = Math.max(highestPriority, to.getPriority());
		}

		for (TaskExplorationAnt potentialAnt : tempAnts) {
			TaskExplorationAnt tempAnt = potentialAnt;

			if (tempAnt.getHighestPriotity() <= highestPriority) {
				this.refreshTaskIntention(time);
				break;
			}

			try {

				// We have chosen a task, we send
				Point lastPoint = tempAnt.getPath().get(tempAnt.getPath().size() - 1);
				InfrastructureAgent ia = darModel.getAgentAt(lastPoint);
				if (!(ia instanceof StorageAgent))
					throw new SimulationException("beginPoint of task is not a storageAgent");

				StorageAgent sa = (StorageAgent) ia;

				if (sa.hasTask() && sa.lookingForTaskConsumers()) {
					TaskIntentionAnt taskIntentionAnt = new TaskIntentionAnt(tempAnt.getPath(), this, lastPoint,
							tempAnt.reservations, tempAnt.getTaskOptions());
					taskIntentionAnt.handleAnt(ia, time);

					System.out.println("--INFO-- BETTER TASKS SELECTED FOR " + this);
					darModel.betterTaskFor(this, time);
					if (doingPickUp) {
						this.waiting = false;
						break;
					}
				}
			} catch (SimulationException e) {
				e.printStackTrace();
			}
		}
		explorationAnts.removeAll(tempAnts);

	}

	private void chooseBestTask(TimeLapse timeLapse) {
		// implement time windows, priorities and other heuristics
		// ATM we only look for the task we can reach the fastest!
		// modify for interdependent tasks, we now just assume there is just a
		// single task present

		List<TaskExplorationAnt> tempAnts = new ArrayList<TaskExplorationAnt>();

		for (ExplorationAnt ant : explorationAnts) {
			// normally ants should be single type of ants that were send out but we have a
			// safeguard just in case
			if (ant instanceof TaskExplorationAnt) {
				if (ant.hasDependentAnt() || ((TaskExplorationAnt) ant).holdingTask) {
					// we didnt find a path to the endpoint of the taskExploration W
					// We should not consider this ant in that case
					tempAnts.add((TaskExplorationAnt) ant);
				}
				// We choose the closest ant with an dependant ants endpoint that is the
				// lowest/reachable
			}
		}
		if (tempAnts.isEmpty()) {
			System.err.print("[ERROR] we didnt have any taskExplorationAnts");
			// explorationAnts.clear();
			return;
		}

		// include batterylife if no dependant is available we just assume that our
		// battery needs to be >75% at the end
		ArrayList<ExplorationAnt> toRemove = new ArrayList<ExplorationAnt>();
		for (ExplorationAnt ant : tempAnts) {
			if (this.battery.batteryLifeAfterDistance(
					ant.getTotalPathLength() * SimulationSettings.BATTERY_OVERKILL_PERCENTAGE) <= 0.15) {
				toRemove.add(ant);
			}
		}
		tempAnts.removeAll(toRemove);

		if (tempAnts.isEmpty()) {
			this.tasksNotAvailableDueToBattery = true;
			explorationAnts.clear();
			return;
		}

		tempAnts.sort(new TaskComparator());

		for (TaskExplorationAnt potentialAnt : tempAnts) {
			TaskExplorationAnt tempAnt = potentialAnt;

			try {

				// We have chosen a task, we send
				Point lastPoint = tempAnt.getPath().get(tempAnt.getPath().size() - 1);
				InfrastructureAgent ia = darModel.getAgentAt(lastPoint);
				if (!(ia instanceof StorageAgent))
					throw new SimulationException("beginPoint of task is not a storageAgent");

				StorageAgent sa = (StorageAgent) ia;

				if (sa.hasTask() && sa.lookingForTaskConsumers()) {
					TaskIntentionAnt taskIntentionAnt = new TaskIntentionAnt(tempAnt.getPath(), this, lastPoint,
							tempAnt.reservations, tempAnt.getTaskOptions());
					taskIntentionAnt.handleAnt(ia, timeLapse);
					if (doingPickUp)
						break;
				}
			} catch (SimulationException e) {
				e.printStackTrace();
			}
		}

		explorationAnts.clear();
	}

	public void handleTaskIntentionAnt(TaskIntentionAnt taskIntentionAnt, TimeLapse timeLapse) {
		if (taskIntentionAnt.getTaskOptions().isEmpty())
			return;
		if (allocatedTask != null)
			return;
		if (taskIntentionAnt.getAccepted()) {
			doingPickUp = true;
			this.pickUpTaskOptions.clear();
			this.previousSelectedTasks.clear();
			if (taskIntentionAnt.getTaskOptions().isEmpty())
				return;
			this.pickUpTaskOptions.addAll(taskIntentionAnt.getTaskOptions());

			if (!forcedMove) {
				this.endPoint = Optional.of(taskIntentionAnt.getEndPoint());

				if (!this.intention.isPresent())
					this.lookForPath(timeLapse, true, this.staticGraph, false);

			}
		} else {
			doingPickUp = false;
			if (isOnNode) {
				reservations = null;
				intention = Optional.absent();
				endPoint = Optional.absent();
			} else
				this.resetReservation = true;
			this.pickUpTaskOptions.clear();
			this.previousSelectedTasks.clear();
		}
	}

	@Override
	public void handleDeadLockDetectionAnt(DeadLockDetectionAnt dlAnt, TimeLapse time) {
		if (movingTo == null) {
			// We are currently not moving
			// So no edge should be added
			// But we are the reason a deadlock has occurred

			// System.err.println("A non mover was found; " + this);
			// if(!dlAnt.getDeadLock().isEmpty())
			dlAnt.setNonMover(this, time);
		} else if (!movingTo.equals(this.getPosition().get())) {
			dlAnt.addEdge(new DeadLockedEdge(this.getPosition().get(), movingTo, this));
			try {
				dlAnt.handleAnt(darModel.getAgentAt(movingTo), time);
			} catch (SimulationException e) {
			}
			// nodeOn.sendAntToNeighbour(dlAnt, movingTo, time);
		}
	}

	// TODO sometimes this gets really confused and fucks up a lot of things
	// TODO change this
	public void reservationExtendedReSearchNeeded(TimeLapse time) {
		this.extendReservation += 1;
		if (endPoint.isPresent() && reservations != null) {
			if (this.isOnNode) {
				this.refreshPath = true;
				this.intendPath(time); // this.lookForPath(time, false, staticGraph, false);
				this.lookForBetterPath(time);
				startEdgeChasing(time);
				this.refreshPath = false;
			} else {
				this.refreshPath = true;
			}
		}

	}

	protected void reconsiderActions(TimeLapse time) throws SimulationException {

		// An AGVAgent should consider the following actions
		// if waiting for ants wait till we have enough ants or the timer has finished
		// after we send ants we set the waitingforants to 0;
		// ---------------------
		// if the battery is low look for charging stations explorationAnt
		// -------------------
		// Check for tasks if non allocated exporationAnt
		// If Multiple tasks present at the location the agent should be able to do all
		// the tasks
		// After X amount of time has past relook for tasks to see if a better one was
		// available
		// --------------------
		// If no tasks that are available look for Charging station
		// If no tasks are doable because the battery is low -> look for charging
		// station -> set rechargeNeeded = true
		// ----------------------
		// Check for the best route to the location, recheck
		// after some time or recheck if something went wrong
		// if going to charge this is the closest Chargingstation
		// If more chargingstattions are available look for the best one that is
		// reachable

		// if going todo pickup this is the best route to the pickup location
		// if a task is present this is the endLocation of the task
		// -----------------------
		// After x amount of time resend the intention ants if no other choises were
		// made

		try {
			if (isOnNode) {
				if (!nodeOn.routeReservations.isEmpty() && !nodeOn.routeReservations.get(0).agv.equals(this)
						&& nodeOn.routeReservations.get(0).isIn(time.getTime())) {

					System.err.println(nodeOn.routeReservations.get(0).agv + " is not equal to this" + this);
					System.err.println("Smt went wrong");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(rechargeNeeded && this.getRemainingBatteryCapacityPercentage()> 30 && !darModel.noTaskActive()) {
			rechargeNeeded = false;tasksNotAvailableDueToBattery=false;
		}

		if (waitingForAnts()) {
			if (verbose)
				System.out.println("[INFO] AGV is waiting for ants");

			if (resendIntentionNeeded()) {
				timeSinceLastIntention = 0;
				refreshPathIntention(time);
				if (doingPickUp)
					refreshTaskIntention(time);
				if (doingCharge)
					refreshChargingIntention(time);
			}

			// nodeOn.extendReservation(time, 5, this);
			return;
		}

		if (noTasksActive && this.allocatedTask == null && !darModel.noTaskActive()
				&& battery.getRemainingCharge() > 25) {
			noTasksActive = false;
			this.rechargeNeeded = false;
		}

		if (verbose)
			System.out.println("[INFO] AGV is making a decision");

		if (reservations == null && !isOnNode) {
			darModel.AGVNeedsRescue(this, time);
			return;
		}

		if (forcedMove) {
			if (intention.isPresent()) {
				if (verbose)
					System.out.println("[INFO] AGV has an intention for charging");

				if (reservations == null) {
					lookForPath(time, true, staticGraph, true);
				}

				if (resendExplorationNeeded()) {
					// check for better paths
					lookForBetterPath(time);
				}

				// refresh intentions
				if (resendIntentionNeeded()) {
					timeSinceLastIntention = 0;
					refreshPathIntention(time);
					refreshChargingIntention(time);
					refreshTaskIntention(time);
				}
			} else {
				forcedMove = false;
			}
			return;
		}

		if (reservations == null && !isOnNode) {
			darModel.AGVNeedsRescue(this, time);
			return;
		}

		// We dont have an allocated Task
		if (allocatedTask == null) {

			if (verbose)
				System.out.println("[INFO] AGV has no allocated task");
			// If we need to charge or are moving towards a chargingstation
			if (doingCharge || rechargeNeeded) {

				// we have a path to a charging Station
				if (intention.isPresent()) {
					if (verbose)
						System.out.println("[INFO] AGV has an intention for charging");

					if (reservations == null) {
						intendPath(time);
					}

					if (resendExplorationNeeded()) {
						// check for better paths
						lookForBetterPath(time);
						lookForBetterChargingPoint(time);
					}

					// refresh intentions
					if (resendIntentionNeeded()) {
						timeSinceLastIntention = 0;
						refreshPathIntention(time);
						refreshChargingIntention(time);
					}
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}
				}
				// We dont have a path so we should look for a path
				else if (this.co != null) {
					if (!endPoint.isPresent())
						this.endPoint = Optional.of(co.getChargingStation().position);
					this.chooseBestPath(time);
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}
				} else {
					if (!nodeOn.canSearchHere(this)) {
						this.gotoSearchPoint(time);
						return;
					} else {
						// check for best path to a chargingStation that is available
						handleChargingExplorationAnt(null, time);
						if (!waitingForAnts())
							lookForBestChargingStation(time);
					}
				}
			}
			// We have a task we want to go and pickup
			// We want to do a pickup but we dont have an intention
			else if (doingPickUp && intention.isPresent() && reservations == null) {
				if (intention.get().size() == 1) {
					// we are already on the correct node and should not look for a path
					// We can just remove the intentions and be fine
					if (verbose)
						System.out.println("[INFO] AGV is standing on pickup");
					reservations = new ArrayList<>();
					reservations.add(nodeOn.getCurrentReservation(time));
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}
					return;
				}
				if (verbose)
					System.out.println("[INFO] AGV is sending an intention for a path");

				this.intendTask(time);
				intendPath(time);

				if (reservations == null && !isOnNode) {
					darModel.AGVNeedsRescue(this, time);
					return;
				}
			} else if (doingPickUp && intention.isPresent() && reservations != null) {

				if (verbose)
					System.out.println("[INFO] AGV has an intention for pickUp");

				if (resendExplorationNeeded()) {
					// Look for better paths
					lookForBetterPath(time);
					// look for better tasks
					lookForBetterTasks(time);

				}
				if (reservations == null && !isOnNode) {
					darModel.AGVNeedsRescue(this, time);
					return;
				}
				if (resendIntentionNeeded()) {
					timeSinceLastIntention = 0;
					// confirm task
					refreshTaskIntention(time);
					// confirm route
					refreshPathIntention(time);
				}
				if (reservations == null && !isOnNode) {
					darModel.AGVNeedsRescue(this, time);
					return;
				}
			} else if (doingPickUp && !intention.isPresent()) {

				// ENDPOINT IS ABSEBCT MAAR WE DOEN EEN PICKUP
				// I think this happens because a taskIntention was send but not accepted
				// and after which a setting is not properly reset
				if (!endPoint.isPresent()) {
					doingPickUp = false;
					if (verbose)
						System.err.println(
								"[ERROR] AGV is looking for an intention for pickup but has no endPoint it is looking for");
					return;
				}
				if (reservations == null && !isOnNode) {
					darModel.AGVNeedsRescue(this, time);
					return;
				}
				if (verbose)
					System.out.println("[INFO] AGV is looking for an intention for pickup");
				if (endPoint.get().equals(this.getPosition().get())) {
					ArrayList<Point> tempPath = new ArrayList<Point>();
					tempPath.add(this.endPoint.get());
					setIntentionFromPath(tempPath);

					this.reservations = new ArrayList<RouteReservation>();
					this.reservations.add(nodeOn.getCurrentReservation(time));
				}
				lookForPath(time, true, staticGraph, true);
				if (reservations == null && !isOnNode) {
					darModel.AGVNeedsRescue(this, time);
					return;
				}
			}
			// We dont have a tasks, we dont need to charge, and we dont have a task we want
			// to pickup
			// So we start looking for a task
			else {
				// Check if there are tasks available within batteryReach in best case
				if (verbose)
					System.out.println("[INFO] AGV is looking for a task ");
				if (this.tasksNotAvailableDueToBattery) {
					this.rechargeNeeded = true;
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}
					return;
				}
				if (isOnNode && nodeOn instanceof StorageAgent && ((StorageAgent) nodeOn).hasTask()) {
					this.setIntentionFromPath(new LinkedList<Point>());
					this.pickUpTaskOptions = ((StorageAgent) nodeOn).getTaskOptions(time);
					this.reservations = new ArrayList<RouteReservation>();
					this.doingPickUp = true;
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}
					return;
				}

				if (!nodeOn.canSearchHere(this)) {
					this.gotoSearchPoint(time);
					return;
				} else {
					if (checkTasksAvailable()) {
						if (!this.broadCastAnts.isEmpty()) {
							this.broadCastAnts.clear();
						}
						// look for best task
						this.hasShelter = false;
						lookForBestTasks(time);
					} else {
						if (verbose)
							System.err.println("[INFO] " + this + " could not find a nearby task");
						// if there is no tasks available (within range)
						// canCharge -> go charge
						// cantCharge -> Move to depot.
						if ((battery.getRemainingCharge() <= 15 || tasksNotAvailableDueToBattery)
								|| (darModel.noTaskActive() && battery.getRemainingCharge() <= 50)) {
							rechargeNeeded = true;
							noTasksActive = true;
							tasksNotAvailableDueToBattery = false;
						} else {
							if (verbose)
								System.err.println("[INFO] " + this + " LOOKING FOR HINDING HOLE");
							if (isOnNode && nodeOn.canRestHere(this)) {
								this.reset();
								this.hasShelter = true;
								nodeOn.extendReservationTime(time, SimulationSettings.RestWait, this);
								this.waiting = true;
								this.timeToWait = SimulationSettings.RestWait / time.getTickLength() - 5;
							} else {
								this.hasShelter = false;
								if (waitingForBroadCastAnts <= 0) {
									if (broadCastAnts.isEmpty())
										this.seekShelter(time);
									else
										this.selectShelter(time, this.staticGraph);
								}
							}
						}
						if (reservations == null && !isOnNode) {
							darModel.AGVNeedsRescue(this, time);
							return;
						}
					}
				}

			}
		}
		// We have an allocated Tasks so we want to execute it
		// this might also need logic where if a path takes long and the AGV loses
		// all charge it can decide to do something else
		else {
			if (verbose)
				System.out.println("[INFO] AGV has an allocated task");

			if (allocatedTask instanceof DeliveryTask) {
				if (reservations == null || !intention.isPresent()) {
					if (verbose)
						System.out.println("[INFO] AGV has task " + allocatedTask + " but no path");
					if (containsNoPaths()) {
						chooseBestPath(time);
					} else
						lookForPath(time, true, staticGraph, true);
				} else {
					if (verbose)
						System.out.println("[INFO] AGV has a task and is driving towards it");

					if (resendExplorationNeeded()) {
						// look for better Paths
						lookForBetterPath(time);
					}

					if (resendIntentionNeeded()) {
						timeSinceLastIntention = 0;
						// confirm route
						refreshPathIntention(time);
					}
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}
				}
			} else {
				HoldingTask mt = (HoldingTask) allocatedTask;
				if (mt.returnAllowed) {
					if (reservations == null || !intention.isPresent()) {
						if (verbose)
							System.out.println("[INFO] AGV has task " + allocatedTask + " but no path");
						if(intention.isPresent()) {
							this.intendPath(time);
						}else if (!containsNoPaths()) {
							chooseBestPath(time);
						} else
							lookForPath(time, true, staticGraph, false);
					} else {
						if (verbose)
							System.out.println("[INFO] AGV has a task and is driving towards it");

						if (resendExplorationNeeded()) {
							// look for better Paths
							lookForBetterPath(time);
						}

						if (resendIntentionNeeded()) {
							timeSinceLastIntention = 0;
							// confirm route
							refreshPathIntention(time);
						}
						if (reservations == null && !isOnNode) {
							darModel.AGVNeedsRescue(this, time);
							return;
						}
					}
				} else {
					if (isOnNode && nodeOn.canRestHere(this)) {
						nodeOn.extendReservationTime(time, SimulationSettings.RestWait, this);
						this.waiting = true;
						this.timeToWait = SimulationSettings.RestWait / time.getTickLength() - 5;
					} else {
						if (endPoint.isPresent() && reservations != null) {
							if (resendIntentionNeeded()) {
								timeSinceLastIntention = 0;
								// confirm route
								refreshPathIntention(time);
							}
							if (reservations == null && !isOnNode) {
								darModel.AGVNeedsRescue(this, time);
								return;
							}
							return;
						}
						if(endPoint.isPresent() && intention.isPresent() && reservations==null) {
							this.intendPath(time);
						}else if (endPoint.isPresent() && !intention.isPresent()) {
							this.lookForPath(time, true, this.staticGraph, false);
						} else if (waitingForBroadCastAnts <= 0) {
							if (broadCastAnts.isEmpty())
								this.seekShelter(time);
							else
								this.selectShelter(time, this.staticGraph);
						}
					}
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}

				}
			}
			if (reservations == null && !isOnNode) {
				darModel.AGVNeedsRescue(this, time);
				return;
			}
		}
	}

	private void gotoSearchPoint(TimeLapse time) {
		if (endPoint.isPresent()) {
			if (!this.pickUpTaskOptions.isEmpty()) {
				this.intendTask(time);
				if (this.doingPickUp)
					this.lookForPath(time, true, staticGraph, false);
			} else {
				InfrastructureAgent node;
				try {
					node = darModel.getAgentAt(endPoint.get());
				} catch (Exception e) {
					this.intention = Optional.absent();
					endPoint = Optional.absent();
					return;
				}

				if (!node.canRestHere(this)) {
					this.intention = Optional.absent();
					endPoint = Optional.absent();
				} else {
					if (reservations != null) {
						if (this.resendIntentionNeeded()) {
							timeSinceLastIntention = 0;
							this.refreshPathIntention(time);
						}
						if (reservations == null && !isOnNode) {
							darModel.AGVNeedsRescue(this, time);
							return;
						}
						return;
					}
					if (intention.isPresent())
						this.intendPath(time);
					else
						this.lookForPath(time, true, staticGraph, false);
				}
			}
			if (reservations == null && !isOnNode) {
				darModel.AGVNeedsRescue(this, time);
				return;
			}
			return;
		} else if (waitingForBroadCastAnts <= 0) {
			if (broadCastAnts.isEmpty()) {
				this.reset();
				this.seekShelter(time);
			} else {
				this.selectShelter(time, this.staticGraph);
				this.lookForPath(time, true, this.staticGraph, false);
			}

			if (reservations == null && !isOnNode) {
				darModel.AGVNeedsRescue(this, time);
				return;
			}
		}
	}

	protected void intendTask(TimeLapse time) {
		if (!this.pickUpTaskOptions.isEmpty()) {
			List<List<Point>> paths = AStar.getInstance().getAlternativePaths(1, this.getPosition().get(),
					pickUpTaskOptions.get(0).getBeginPoint(), rng, staticGraph);
			for (List<Point> p : paths) {
				TaskIntentionAnt ant = new TaskIntentionAnt(p, this, pickUpTaskOptions.get(0).getBeginPoint(), null,
						new ArrayList<TaskOptions>(pickUpTaskOptions));
				ant.handleAnt(pickUpTaskOptions.get(0).taskAgent, time);
			}
		}
	}

	// has way to many possible locations
	// Something goes wrong
	public boolean selectShelter(TimeLapse time, ListenableGraph<LengthData> graph) {
		if (broadCastAnts.isEmpty())
			return false;

		if (!isOnNode)
			return false;
		// No way out, wait for more AGV being moved out of the way of the deadlock
		if (graph.getOutgoingConnections(this.getPosition().get()).size() == 0) {
			return false;
		}

		try {
			if (this.endPoint.isPresent() && darModel.getAgentAt(this.endPoint.get()).canRestHere(this)) {
				if (!this.explorationAnts.isEmpty()) {
					this.lookForPath(time, true, graph, false);
					
				}
				return true;
			} else {
				this.endPoint = Optional.absent();
			}
		} catch (SimulationException e) {
		}

		// This means we are already looking for a path towards the endgoal of resting
		// somewhere
		List<findRestingPlaceAnt> acceptedPoints = new ArrayList<findRestingPlaceAnt>();
		for (BroadCastAnt ant : broadCastAnts) {
			if (ant instanceof findRestingPlaceAnt) {
				findRestingPlaceAnt tempAnt = (findRestingPlaceAnt) ant;
				if (tempAnt.acceptedPoint.canRestHere(this))
					acceptedPoints.add(tempAnt);
			}
		}

		//TODO added
		if(isOnNode)
			this.intention=Optional.absent();
		broadCastAnts.clear();

		if (acceptedPoints.isEmpty()) {
			return false;
		}

		acceptedPoints.sort(new distanceComparator(this.getPosition().get()));
		int acceptedPointsize = acceptedPoints.size();
		int i = 0;

		this.nodeOn.extendReservation(time, 5, this);

		do {
			if (i >= acceptedPointsize) {
				return false;
			}

			this.endPoint = Optional.of(acceptedPoints.get(i).acceptedPoint.position);
			i++;
			// System.out.println(graph.getOutgoingConnections(this.getPosition().get()));
		} while (!this.lookForPath(time, true, graph, false));
		this.waiting = false;
		this.hasShelter = false;
		return true;
	}

	protected void seekShelter(TimeLapse time) {
		seekShelter(new HashSet<Point>(), time);
	}

	public void seekShelter(Set<Point> disallowedNodes, TimeLapse time) {
		// this.broadCastAnts.clear();

		// nodeOn.extendReservationTime(time, time.getTime() + 30 * 1000, this);
		doingPickUp = false;
		waitingForBroadCastAnts = (int) (SimulationSettings.RETRY_DEADLOCKS_SOLVER / SimulationSettings.TICK_LENGTH
				+ 1);
		findRestingPlaceAnt tempAnt = new findRestingPlaceAnt(this, disallowedNodes, SimulationSettings.SEARCH_HORIZON);

		commDevice.send(tempAnt, nodeOn);
	}

	@Override
	public void handleRestingPlaceAnt(findRestingPlaceAnt rp, TimeLapse time) {
		if(this.ID%1000==13)
			System.out.print("");
		
		try {
			if (isInShelter() || (this.endPoint.isPresent() && darModel.getAgentAt(endPoint.get()).canRestHere(this))) {
				// System.out.println("ALREADY HAS A RESTINGPLACE");
				this.waitingForBroadCastAnts = 0;
				return;
			}
		} catch (SimulationException e) {
		}
		this.broadCastAnts.add(rp);

		if (this.broadCastAnts.size() > 5) {
				if (!this.hasShelter && this.selectShelter(time, staticGraph))
					this.waitingForBroadCastAnts = 0;
		}
	}

	public boolean isInShelter() {
		return isOnNode && nodeOn.canRestHere(this);
	}

	@Override
	public void searchForRestingPlace(Set<Point> nodes, long endTime, TimeLapse time,
			ListenableGraph<LengthData> graph) {
		if (verbose)
			System.out.println(this + " is looking for a hinding space");

		// this.pickUpTaskOptions.clear();
		if (!nodes.contains(this.getPosition().get()) && isInShelter()) {
			this.timeToWait = time.getTime() - endTime / time.getTickLength();
			nodeOn.extendReservationTime(time, time.getTime() - endTime + 500, this);
		} else {

			if (endPoint.isPresent()) {
				try {
					if (!nodes.contains(endPoint.get()) && darModel.getAgentAt(endPoint.get()).canRestHere(this)) {
						if (!intention.isPresent()) {
							this.lookForPath(time, true, graph, false);
							return;
						} else {
							 waiting = false;
							 /* this.explorationAnts.clear(); this.waitingForExplorationAnts
							 * = 0; this.waitingForIntentionAnts = 0; this.intentionAnts.clear();
							 * this.broadCastAnts.clear(); this.waitingForBroadCastAnts = 0;
							 */
							return;
						}
					} else
						endPoint = Optional.absent();
				} catch (Exception e) {
				}
			}

			if (broadCastAnts.isEmpty() || !this.selectShelter(time, graph)) {
				nodeOn.extendReservationTime(time, 15 * 1000, this);
				this.seekShelter(nodes, time);
			}
		}
	}
}
