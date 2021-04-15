package agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.measure.unit.SI;

import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;

import AGV.Battery;
import ants.*;
import ants.path.ExplorationAnt;
import ants.path.IntentionAnt;
import ants.path.PathAnt;
import ants.path.PathExplorationAnt;
import comperators.AntReservationComparator;
import deadlocks.DeadlockSolver;

import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;

import models.DARModel;
import reservations.ChargingOptions;
import reservations.DeadLockRouteReservation;
import reservations.RouteReservation;
import reservations.TaskOptions;
import simulator.SimulationSettings;
import simulator.SimulationException;
import tasks.BasicTask;
import tasks.CarPackage;
import tasks.HoldingTask;
import tasks.DeliveryTask;

/**
 * This represents a basic AGV agent The main concerns for this agent is Task
 * and Route allocation Works using BDI scheme
 * 
 * @author rapha
 *
 */
public abstract class AGVAgent extends Vehicle implements BasicAgent, RandomUser, MovingRoadUser {
	// Vars for setup of AGVAgent
	final protected boolean verbose;
	final public int ID;
	protected boolean isOnNode = true;
	protected InfrastructureAgent nodeOn;

	// THis is not completly accurate as we rescale some parts to make it easier
	protected long totalDistanceTravelled = 0;
	public long totalTravelTime = 0;

	// Vars for Init
	protected CommDevice commDevice;
	protected RoadModel roadModel;
	protected PDPModel pdpModel;
	protected DARModel darModel;
	public ListenableGraph<LengthData> staticGraph;
	// final protected ListenableGraph<LengthData> dynamicGraph;
	protected RandomGenerator rng;
	protected boolean movingFailed;

	// SETUP for decisions and moving
	protected List<RouteReservation> reservations;
	protected long idleTime = 0;
	protected int waitingForExplorationAnts = 0;
	protected int waitingForIntentionAnts = 0;
	protected int waitingForBroadCastAnts = 0;
	public Optional<Point> endPoint = Optional.absent();
	public Optional<Queue<Point>> intention = Optional.absent();
	protected Point movingTo;

	// Vars for ants
	// SEND IntentionAnts
	protected List<IntentionAnt> intentionAnts = new ArrayList<IntentionAnt>();
	// SEND ExplorationAnts
	protected List<ExplorationAnt> explorationAnts = new ArrayList<ExplorationAnt>();

	protected List<BroadCastAnt> broadCastAnts = new ArrayList<BroadCastAnt>();
	// ANTID -> Number of gottenResponses
	// private Map<Integer, Integer> waitingOnResponsFor = new HashMap<Integer,
	// Integer>();
	protected int alternativePathsExploration;
	protected int timeSinceLastIntention = 0;
	protected int reExploreAmmountOfNodes = SimulationSettings.EXPLORATION_PATH_REFRESH_TIME;

	// Vars for tasks
	public BasicTask allocatedTask;
	// List of the taskOptions on the location we are currently heading
	protected List<TaskOptions> pickUpTaskOptions = new ArrayList<TaskOptions>();
	protected CarPackage parcel;
	public boolean doingPickUp = false;
	public boolean doingCharge = false;
	protected int sendExplorationAnts;
	protected int sendIntentionAnts;

	// Vars for charging
	protected boolean rechargeNeeded = false;
	protected Battery battery;
	public boolean isCharging = false;
	public boolean waiting = false;
	public long chargeTime = 0;
	protected boolean tasksNotAvailableDueToBattery = false;
	public ChargingOptions co = null;
	protected long timeToWait = 0;
	public boolean forcedMove = false;
	protected boolean refreshPath = false;
	protected boolean resetReservation = false;
	protected DeadlockSolver dl;
	public boolean active = true;

	// I think the ammount of reconsidering of actions
	// relooking for better taks/charges/paths goes of way to littlw

	public AGVAgent(int id, Point startNode, double agvSpeed, int alternativePathsExploration,
			int explorationRefreshTime, int intentionRefreshTime, boolean verbose,
			InfrastructureAgent infrastructureAgent, int currentBattery, DeadlockSolver dl) {
		super(VehicleDTO.builder().capacity(1).startPosition(startNode).speed(agvSpeed).build());
		this.ID = id;
		this.alternativePathsExploration = alternativePathsExploration;
		this.verbose = verbose;
		battery = new Battery(SimulationSettings.AGVMaxCharge);
		battery.increaseCharge(currentBattery);
		// battery.increaseCharge(2000);

		// Demand a routereservation on the node for initiallizing (with half the
		// Time a node transfer takes)
		nodeOn = infrastructureAgent;
		nodeOn.firstReservation(this, 0);
		if(dl==null)
			throw new IllegalArgumentException("DL cant be null");
		this.dl = dl;
	}

	protected AGVAgent(int iD2, double aGVSpeed, boolean verbose2, InfrastructureAgent agent, Battery battery2, DeadlockSolver dl) {
		super(VehicleDTO.builder().capacity(1).startPosition(agent.position).speed(aGVSpeed).build());
		nodeOn = agent;
		battery = battery2;
		this.ID = iD2;
		this.verbose = verbose2;
		this.dl = dl;
	}

	/************************************
	 * Basic functionality of AGVAgent *
	 ************************************/

	// Maybe move the decision that the AGV makes, into the moment the AGV itself
	// ticks
	// So no decisions are made in between AGV ticks
	// Now that the messages are just send immediately this becomes more important

	public void afterTick(TimeLapse timeLapse) {
		if (this.ID % 1000 == 17)
			System.out.print("");
		if (reservations == null && !isOnNode && !this.getPosition().get().equals(new Point(-5, -5))) {
			darModel.AGVNeedsRescue(this, timeLapse);
			return;
		}

		// does something need to be happen after every tick? Maybe some battery?
		waitingForExplorationAnts--;
		waitingForIntentionAnts--;
		waitingForBroadCastAnts--;
		timeSinceLastIntention++;

		if (this.reservations != null && !this.reservations.isEmpty()) {
			try {
			if (this.reservations.get(0).endTime == timeLapse.getTime())
				nodeOn.extendReservation(timeLapse, 1, this);
			}
			catch (Exception e) {
				this.reservations=null;
			}
		} else if (!this.getPosition().get().equals(new Point(-5, -5)) && isOnNode) {
			/*if (!this.waitingForAnts() && this.isIdle())
				nodeOn.extendReservation(timeLapse, 10, this);*/
			if (nodeOn.getCurrentReservation(timeLapse) == null
					|| !nodeOn.getCurrentReservation(timeLapse).agv.equals(this)) {
				nodeOn.firstReservation(this, timeLapse.getTime());
			}
		}

		if (isOnNode && resetReservation) {
			reservations = null;
			intention = Optional.absent();
			endPoint = Optional.absent();
			this.resetReservation = false;
		}

		if (refreshPath && isOnNode) {
			this.intendPath(timeLapse);
			refreshPath = false;
		}

		if (this.endPoint.isPresent()) {
			if (this.getPosition().get().equals(this.endPoint.get())) {
				this.endPoint = Optional.absent();
			}
		}

		// General problem solving, in case something goes wrong where it shouldn't
		if (reservations != null && reservations.isEmpty()) {
			reservations = null;
		} else if (!this.endPoint.isPresent()) {
			if (intention.isPresent()) {
				this.intention = Optional.absent();
			} else if (allocatedTask != null && !forcedMove) {
				if (allocatedTask instanceof DeliveryTask)
					endPoint = Optional.of(((DeliveryTask) allocatedTask).endPosition);
				else {
					if (((HoldingTask) allocatedTask).returnAllowed) {
						this.endPoint = Optional.of(allocatedTask.beginPosition);
					}
				}
			} else if (!this.pickUpTaskOptions.isEmpty() && !forcedMove) {
				this.endPoint = Optional.of(pickUpTaskOptions.get(0).getBeginPoint());
			}
		}

		if (allocatedTask != null)
			doingPickUp = false;

		if (reservations == null && !isOnNode) {
			darModel.AGVNeedsRescue(this, timeLapse);
			return;
		}
	}

	protected abstract void intendPath(TimeLapse time);

	protected void tickImpl(@NotNull TimeLapse time) {
		//commDevice.clearOutbox();
		// AGVS should regularly inform the node that they are on what they
		// currently are doing
		// They should inform they are waiting on a node
		// They should demand a RouteReservation in the beginning
		if (!time.hasTimeLeft())
			return;
		
		if (this.ID%1000==17)
			System.out.print("");

		if (reservations == null && !isOnNode) {
			darModel.AGVNeedsRescue(this, time);
			return;
		}
		
		if(reservations!=null && !intention.isPresent()) {
			this.setIntentionFromReservations();
		}

		if (battery.isEmpty()) {
			darModel.AGVhasNoBattery(this, time);
			return;
		}

		if (this.isCharging) {
			chargeTime += 1;
			if (verbose)
				System.out.println("[WAITING-INFO] AGV: " + this.ID % 1000 + " Is charging; current battery = "
						+ this.getBattery().getRemainingCharge() + '%');
			if (this.getRemainingBatteryCapacityPercentage()
					/ 100 > SimulationSettings.Battery_Percentage_Needed_To_Leave_Charging)
				leaveChargingStation(time);
			return;
		}

		if (waiting) {
			if (!isOnNode)
				if (reservations != null) {
					this.waiting = false;
				} else
					new SimulationException("waiting while not on node" + this).printStackTrace();
		}

		if (waiting) {
			if (verbose)
				System.out.println("[WAITING-INFO] AGV: " + this.ID % 1000 + " Is waiting");

			nodeOn.extendReservationTime(time, 5000, this);

			if (doingPickUp && allocatedTask == null) {
				refreshTaskIntention(time);
				this.timeSinceLastIntention = 0;

				if (!this.waitingForAnts())
					this.lookForBetterTasks(time);
			}
			if (timeToWait == 0) {
				waiting = false;
				return;
			}
			timeToWait -= 1;
			return;
		}
		if (this.ID%1000==5)
			System.out.print("");

		if (battery.getRemainingCharge() <= 15) {
			rechargeNeeded = true;
		}

		if (this.getRemainingBatteryCapacityPercentage() < .10) {
			darModel.AGVNeedsRescue(this, time);
			return;
		}

		if (reservations == null && !isOnNode) {
			darModel.AGVNeedsRescue(this, time);
			return;
		}

		readMessages(time);
		try {
			reconsiderActions(time);
		} catch (SimulationException e) {
			e.printStackTrace();
		}

		if (reservations == null && !isOnNode) {
			darModel.AGVNeedsRescue(this, time);
			return;
		}

		// Make the robot do its next action
		if (this.intention.isPresent() && reservations != null && !this.isCharging) {
			if (this.waitingForAnts() && !endPoint.isPresent()) {
				nodeOn.extendReservation(time, 2, this);
			} else {
				try {
					this.action(time);
				} catch (SimulationException e) {
					e.printStackTrace();
				}
			}
		} else if (this.isIdle()) {
			this.idleTime += time.getTickLength();
			if (this.resendIntentionNeeded()) {
				if (!this.pickUpTaskOptions.isEmpty())
					this.intendTask(time);
				if (this.reservations != null)
					this.intendPath(time);
				if (this.co != null)
					this.refreshChargingIntention(time);
				timeSinceLastIntention = 0;
			}
		}

		if (reservations == null && !isOnNode) {
			darModel.AGVNeedsRescue(this, time);
			return;
		}
	}

	private void setIntentionFromReservations() {
		LinkedList<Point> tempList = new LinkedList<Point>();
		for(RouteReservation rr: this.reservations) {
			tempList.add(rr.node);
		}
		this.intention=Optional.of(tempList);
	}

	protected abstract void chooseBetterTask(TimeLapse time);

	protected abstract void lookForBetterTasks(TimeLapse time);

	protected abstract void refreshChargingIntention(TimeLapse time);

	protected abstract void refreshTaskIntention(TimeLapse time);

	protected abstract void reconsiderActions(TimeLapse time) throws SimulationException;

	public void leaveChargingStation(TimeLapse time) {
		if (darModel.robotWantsToLeaveCharging(time.getTickLength(), this)) {
			try {
				darModel.robotLeftChargingStation(this, co.getChargingStation().getPosition().get(), time);
				this.isCharging = false;
			} catch (SimulationException e) {
				e.printStackTrace();
			}

		}
	}

	private void action(TimeLapse time) throws SimulationException {
		// If an intention is present, make the robot follow it.

		if (this.waiting)
			return;

		if (verbose)
			System.out.println("[INFO] AGV is doing an Action");
		try {
			this.move(time);
		} catch (Exception e) {
			e.printStackTrace();
			nodeOn.extendReservation(time, 2, this);
		}

		if (reservations != null && reservations.isEmpty()) {
			reservations = null;
		}

		if (reservations == null && !isOnNode) {
			darModel.AGVNeedsRescue(this, time);
			return;
		}

		if (!this.intention.isPresent()) {
			// something went wrong in the move of the agv
			// we dont want to execute the rest
			return;

		}

		// If robot arrived at the destination of the intention, the intention will be
		// empty
		if (this.intention.get().isEmpty()) {
			this.deleteIntention();
			movingTo = null;
			this.endPoint = Optional.absent();
			if (forcedMove) {
				forcedMove = false;
				dl.hasFinishedForcedMove(this, this.nodeOn);
			} else if (allocatedTask != null) {
				this.allocatedTask.currentPosition = this.getPosition().get();
				if (allocatedTask instanceof DeliveryTask) {
					if (this.nodeOn.position.equals(((DeliveryTask) allocatedTask).endPosition)) {
						if (this.darModel.canDeliverCar(this, parcel, time.getTime())) {
							this.deliverParcel(parcel, time);
							this.reservations = null;
							this.allocatedTask = null;
							this.doingPickUp = false;
							explorationAnts.clear();
							intentionAnts.clear();
							this.intention = Optional.absent();
							endPoint = Optional.absent();
						} else {
							Exception e = new SimulationException(
									this + " is stuck as it cannot deliver the package at the end node of the package");
							e.printStackTrace();
						}
					}
				} else {
					if (((HoldingTask) allocatedTask).returnAllowed
							&& this.nodeOn.position.equals(((HoldingTask) allocatedTask).endPosition)) {
						if (this.darModel.canDeliverCar(this, parcel, time.getTime())) {
							this.deliverParcel(parcel, time);
							this.reservations = null;
							this.allocatedTask = null;
							this.doingPickUp = false;
							explorationAnts.clear();
							intentionAnts.clear();
							this.intention = Optional.absent();
							endPoint = Optional.absent();
						} else {
							Exception e = new SimulationException(
									"AGV is stuck as it cannot deliver the package at the end node of the package");
							e.printStackTrace();
						}
					} else if (this.isOnNode && this.nodeOn.canRestHere(this)) {
						this.timeToWait = SimulationSettings.RestWait / time.getTickLength() - 5;
						this.waiting = true;
						this.nodeOn.extendReservationTime(time, SimulationSettings.RestWait, this);
					} else {
						if (!this.selectShelter(time, staticGraph))
							this.seekShelter(time);
					}
				}
			} else if (doingPickUp && isOnNode && !this.pickUpTaskOptions.isEmpty()
					&& this.pickUpTaskOptions.get(0).getBeginPoint().equals(this.getPosition().get())) {
				if (nodeOn instanceof StorageAgent) {
					StorageAgent sa = (StorageAgent) nodeOn;
					if (!sa.hasTask()) {
						intention = Optional.absent();
						reservations = null;
						doingPickUp = false;
						this.pickUpTaskOptions.clear();
						throw new SimulationException("Trying todo a pickup where no tasks are present");
					}
					allocatedTask = sa.getFirstTask();
					if (allocatedTask == null)
						return;
					this.addParcel(sa.getFirstCarPackage(time, this), time);
					doingPickUp = false;
					if (allocatedTask instanceof DeliveryTask)
						endPoint = Optional.of(((DeliveryTask) allocatedTask).endPosition);
					else
						this.seekShelter(time);
				} else {
					throw new SimulationException("Trying todo a pickup on a non storagAgent");
				}
			} else if (doingCharge && isOnNode) {
				darModel.robotArrivedAtChargingStation(this, co.getChargingStation(), time,
						this.battery.maxCap - this.battery.currentCap);
			}
		}

		if (reservations == null && !isOnNode) {
			darModel.AGVNeedsRescue(this, time);
			return;
		}
	}

	protected abstract void seekShelter(TimeLapse time);

	private void readMessages(TimeLapse timeLapse) {
		if (!isOnNode)
			return;

		ImmutableList<Message> incomming = commDevice.getUnreadMessages();
		for (Message m : incomming) {
			if (m.getContents() instanceof BasicAnt) {
				if (verbose)
					System.out.println("Incomming message for " + this + ": " + m.getContents().toString());
				BasicAnt ba = (BasicAnt) m.getContents();
				if (ba.agent.equals(this))
					ba.handleAnt(this, timeLapse);
			}
		}
	}

	protected boolean containsNoPaths() {
		if (explorationAnts.isEmpty())
			return true;
		for (ExplorationAnt pa : explorationAnts) {
			if (pa instanceof PathExplorationAnt) {
				return false;
			}
		}
		return true;
	}

	protected boolean resendExplorationNeeded() {
		if (((doingPickUp) || (doingCharge) || allocatedTask != null) && intention.isPresent() && isOnNode
				&& reExploreAmmountOfNodes == 0) {
			reExploreAmmountOfNodes = SimulationSettings.EXPLORATION_PATH_REFRESH_TIME;
			return true;
		}
		return false;
	}

	protected boolean resendIntentionNeeded() {
		return timeSinceLastIntention >= SimulationSettings.INTENTION_REFRESH_TIME;
	}

	protected List<Point> getPathFromReservations(List<RouteReservation> reservations2) {
		if (reservations2 == null)
			return null;
		try {
			List<Point> tenp = new ArrayList<Point>();
			for (RouteReservation rr : reservations2) {
				tenp.add(rr.node);
			}
			return tenp;
		} catch (Exception e) {
			System.err.println(this);
			e.printStackTrace();
			return null;
		}
	}

	/****************
	 * Ant handlers *
	 ****************/

	protected boolean checkDeadLocks(PathAnt ba) {
		for (RouteReservation rr : ba.reservations) {
			if (rr instanceof DeadLockRouteReservation) {
				return true;
			}
		}
		return false;
	}

	protected ExplorationAnt getBestPath(TimeLapse time) {
		List<ExplorationAnt> tempPaths = new ArrayList<ExplorationAnt>();
		if (!endPoint.isPresent())
			return null;

		for (ExplorationAnt ea : explorationAnts) {
			if (ea instanceof PathExplorationAnt)
				tempPaths.add(ea);
		}

		if (tempPaths.isEmpty())
			// No path was found
			return null;

		// we sort the ants based on the last ReservationTime
		explorationAnts.removeAll(tempPaths);
		List<ExplorationAnt> toRemove = new ArrayList<>();
		for (ExplorationAnt ea : tempPaths) {
			if (checkDeadLocks(ea)) {
				toRemove.add(ea);
			}
			RouteReservation lastReservation = ea.reservations.get(ea.reservations.size() - 1);
			if (!lastReservation.node.equals(endPoint.get())) {
				toRemove.add(ea);
			}
			boolean good = false;
			for (RouteReservation rr : ea.reservations) {
				if (rr.node.equals(this.getPosition().get())) {
					good = true;
					break;
				}
			}

			if (good)
				this.trimReservations(ea.reservations);
			else
				toRemove.add(ea);
		}

		if (tempPaths.size() == toRemove.size()) {
			if (verbose)
				System.out.println("[INFO] All paths had a wrong endpoint or had deadlocks in them");
			try {
				movingTo = toRemove.get(0).getNextPoint(nodeOn.position);
			} catch (SimulationException e) {
				// e.printStackTrace();
			}
			
			// TODO change this
			if (tempPaths.size() != 1)
			//	this.startEdgeChasing(time);
			
			return null;
		}

		tempPaths.removeAll(toRemove);

		Collection<?> constrainedPaths = getBatteryConstrainedPaths(tempPaths);
		if (constrainedPaths.size() == tempPaths.size()) {
			this.tasksNotAvailableDueToBattery = true;
			return null;
		}

		tempPaths.removeAll(constrainedPaths);
		tempPaths.sort(new AntReservationComparator());
		return tempPaths.get(0);
		// Now that we have a path that we want to follow we should sent intentionants
		// over said path
		// We also should resend the intention for the task just to be sure -> if we
		// have an endPoint and doing PickUp
	}

	private void trimReservations(List<RouteReservation> reservations2) {
		List<RouteReservation> toRemove = new ArrayList<RouteReservation>();
		for (RouteReservation rr : reservations2) {
			if (rr.node.equals(this.getPosition().get())) {
				break;
			}
			toRemove.add(rr);
		}
		reservations2.removeAll(toRemove);
	}

	protected Collection<ExplorationAnt> getBatteryConstrainedPaths(List<? extends ExplorationAnt> tempPaths) {
		Collection<ExplorationAnt> toRemove = new ArrayList<ExplorationAnt>();
		for (ExplorationAnt ant : tempPaths) {
			if (ant.getTotalPathLength() >= battery.getRange() * SimulationSettings.BATTERY_OVERKILL_PERCENTAGE) {
				toRemove.add(ant);
			}
		}
		return toRemove;
	}

	protected long arrivalTime(List<RouteReservation> reservations2) {
		if (reservations2.isEmpty())
			return Long.MAX_VALUE;
		return reservations2.get(reservations2.size() - 1).getBeginTime();
	}

	protected void sendAnt(BasicAnt ba, TimeLapse timeLapse) {
		if(commDevice.getOutbox().size() > 50)
			System.out.print(commDevice.getOutbox());
		commDevice.send(ba, nodeOn);
		// ba.handleAnt(nodeOn, timeLapse);
	}

	protected void sendAnt(BasicAnt ba, InfrastructureAgent node, TimeLapse timeLapse) {
		if(commDevice.getOutbox().size() > 50)
			System.out.print(commDevice.getOutbox());
		commDevice.send(ba, node);
		// ba.handleAnt(node, timeLapse);
	}

	/****************
	 * Decisions *
	 ****************/

	// there has to be a better way to do this
	// Maybe make this so it waits as long as possible on the current node (before
	// having to extend the reservation)
	public boolean waitingForAnts() {
		return (!((waitingForExplorationAnts <= 0 || (explorationAnts.size() > sendExplorationAnts))
				&& (waitingForIntentionAnts <= 0 || !intentionAnts.isEmpty())) || waitingForBroadCastAnts > 0);
	}

	protected void setIntentionFromAnt(PathAnt ant) {
		this.intention = Optional.of(new LinkedList<>(ant.path));
	}

	protected void setIntentionFromPath(List<Point> path) {
		this.intention = Optional.of(new LinkedList<Point>(path));
	}

	private void deleteIntention() {
		this.intention = Optional.absent();
	}

	public boolean isIdle() {
		// Charging does not count as idle
		if (this.isCharging)
			return false;
		if (this.waiting)
			return false;
		if (this.intention.isPresent())
			// If intention is present, robot will move except when waiting for ants. So is
			// idle if waiting for ants
			return this.waitingForAnts();
		else
			// No intention present, so robot is standing still
			return true;
	}

	public long getAndResetIdleTime() {
		long time = this.idleTime;
		this.idleTime = 0;
		return time;
	}

	public long getAndResetChargeTime() {
		long time = this.chargeTime;
		this.chargeTime = 0;
		return time;
	}

	private boolean existsConnectionForNextMove() {
		if (!this.isOnNode || this.intention.get().peek().equals(this.getPosition().get()))
			return true;
		if (this.intention.isPresent()) {
			return this.staticGraph.hasConnection(this.getPosition().get(), this.intention.get().peek());
		}

		return false;
	}

	public void setNextAgent(InfrastructureAgent ag) {
		this.nodeOn = ag;
	}

	private void move(@NotNull TimeLapse time) throws SimulationException {
		if (verbose)
			System.out.println("[INFO] AGV is trying to move");
		
		if (this.ID%1000==6)
			System.out.print("");

		totalTravelTime+=time.getTickLength();
		
		if(!this.isOnNode) {
			if(!this.intention.isPresent()) {
				System.out.print("");
			}
			 if(reservations==null || reservations.isEmpty()) {
				 System.out.print("");
			 }
		}
		
		if(!this.endPoint.isPresent() && this.intention.isPresent() && this.reservations!=null && !reservations.isEmpty()) {
			this.endPoint = Optional.of(this.reservations.get(this.reservations.size()-1).node);
		}
		
		if (this.intention.isPresent() && !reservations.isEmpty()) {

			Queue<Point> path = this.intention.get();
			Point nextPosition = path.peek();

			if (reservations == null || reservations.isEmpty() || reservations.get(0) == null) {
				System.err.println("[Error] a move to " + nextPosition
						+ " is initiated but either no reservations are present or the reservations have finished but were not removed. Reservations: "
						+ reservations);
				reservations = null;
				movingTo = null;
				return;
			}
			
			if (isOnNode && !reservations.get(0).isIn(time.getTime())) {
				if (this.doingPickUp)
					this.intendTask(time);
				// we are only allowed to move to the next node if the reservation time is
				// if we are to early this is not a problem as we just wait for it
				// However if we are to late we should remove the reservations so a new path
				// with reservations is established

				// there is a problem with the routeReservations not being completely right
				// The begin time is modified but the end time is not
				// This delays the AGV and a new reservation is searched for
				if (reservations.get(0).endTime <= time.getTime()) {
					nodeOn.extendReservation(time, 1, this);
					reservations.clear();
					movingTo = null;
					movingFailed = true;
					reservations.add(nodeOn.getCurrentReservation(time));
					this.lookForPath(time, true, this.staticGraph, false);
					return;
				}

				if (verbose)
					System.out.println("[INFO] AGV is trying to move but the reservation is still to early"
							+ reservations.get(0) + " for currentTime: " + time.getTime());

				movingTo = null;
				return;
			}
			if (this.ID%1000==4)
				System.out.print("");

			// Use the dynamic graph to check if the connection still exists (can disappear)
			if (this.existsConnectionForNextMove()) {
				// Needs to check if a move is allowed by the routeReservations
				InfrastructureAgent nextNode = darModel.getAgentAt(nextPosition);

				if (verbose)
					System.out.println("[INFO] current reservation is " + reservations.get(0) + " for currentTime: "
							+ time.getTime());

				long potentialBlockTime = nextNode.nonBlockedCheck(this.endPoint.get(), time, this);
				if (isOnNode && potentialBlockTime != 0d) {
					System.out.println("[INFO] Being blocked by HOLDIA on node " + nextPosition + " " + this);
					// This means we cant move because a potential blockage
					this.reservations = null;
					reExploreAmmountOfNodes = 0;
					//TODO changed
					//this.timeToWait = (potentialBlockTime - time.getTime()) / time.getTickLength() + 10;
					//this.waiting = true;				
					movingTo = null;
					nextNode.removeMyReservation(this);
					if (this.doingPickUp)
						this.intendTask(time);
					return;
				}
				// a move should only by initiated if the next reservation is for the next node
				// we dont want to try to move if we dont have a reservation
				// isonNode was added we should make sure this still works
				else if (isOnNode && !nextNode.agvHasReservation(this.ID, time.getTime())) {
					RouteReservation nextReservation = nextNode.getCurrentReservation(time);
					if (nextReservation == null && nextNode.containsReservationForMe(this)) {
						// if (verbose)
						System.err.println(
								"[ERROR] MOVE IS NOT ALLOWED by " + this.ID % 1000 + " from " + this.getPosition().get()
										+ " to " + nextPosition + " because another agv is already present");
						movingTo = nextPosition;
						startEdgeChasing(time);
						movingFailed = true;
						// this.waiting = true;
						// this.timeToWait= (nextNode.nextReservation().endTime -
						// time.getTime())/time.getTickLength();
						this.intendPath(time);
						this.lookForPath(time, false, this.staticGraph, false);
						return;
					} else if (isOnNode && nextReservation == null) {
						// if (verbose) {
						System.err.println(
								"[ERR] " + this + "; next reservation is not available, reset path and reservations");
						System.err.println(this.reservations);
						System.err.println("=====================================");
						// }
						this.reservations = null;
						this.intention = Optional.absent();
						movingTo = null;
						movingFailed = true;
						this.lookForPath(time, true, this.staticGraph, false);
						return;
					}
					//
					else if (isOnNode &&nextReservation.endTime <= this.reservations.get(0).getBeginTime()) {
						// if (verbose)
						System.err.println("[INFO] AGV " + this.ID % 1000
								+ " cant move to next location because another reservation is holding us");
						intention = Optional.absent();
						reservations = null;
						movingFailed = true;
						movingTo = nextPosition;
						this.lookForPath(time, true, this.staticGraph, false);
						return;

					} else if (isOnNode && !nextNode.containsReservationForMe(this)) {
						// if (verbose)
						System.err.println("We no longer have a reservation through this node " + this.ID % 1000
								+ " from " + this.getPosition().get() + " to " + nextPosition);

						nodeOn.extendReservation(time, 10, this);
						movingTo = nextPosition;
						startEdgeChasing(time);
						reservations = null;
						movingFailed = true;
						this.lookForPath(time, true, this.staticGraph, false);
						return;
					}
				}

				if (reservations == null && !isOnNode) {
					darModel.AGVNeedsRescue(this, time);
					return;
				}

				if (reservations == null)
					return;

				RouteReservation nodeReservation = nextNode.getCurrentReservation(time);
				if (isOnNode && nodeReservation == null) {
					System.out.println("[ERROR] AGV " + this.ID % 1000 + " on position: " + this.getPosition());
					// something went wrong
					// we reset the path and the intention of this AGV
					this.reservations = null;
					this.intention = Optional.absent();
					movingFailed = true;
					movingTo = null;
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}
					return;
				} else if (!isOnNode || nodeReservation.agv.ID == this.ID) {
					// if the next reservation is for us we can move, otherwise we just wait
					// Perform the actual move
					MoveProgress progress = null;
					try {
						progress = this.roadModel.moveTo(this, nextPosition, time);
						movingFailed = false;
					} catch (VerifyException e) {
						System.err.println("[ERROR] AGV " + this.ID % 1000 + " on position: " + this.getPosition()
								+ ". Something goes wrong while trying to move");
						e.printStackTrace();
						movingTo = nextPosition;
						movingFailed = true;
						startEdgeChasing(time);
						this.reservations = null;
						this.intention = Optional.absent();
						if (reservations == null && !isOnNode) {
							darModel.AGVNeedsRescue(this, time);
							return;
						}
						return;
					}

					double newX = roundToClosestEven(this.getPosition().get().x);
					double newY = roundToClosestEven(this.getPosition().get().y);
					if (newX == nextPosition.x && newY == nextPosition.y) {
						nodeOn = nextNode;
					}

					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}

					this.isOnNode = this.getPosition().get().equals(nextPosition);
					if (this.isOnNode) {
						// Remove the next position in the intention when that position has been
						// reached.
						path.remove();
						reservations.remove(0);
						if (!reservations.isEmpty()) {
							reservations.get(0).resetEvaporation();
						}
						movingTo = nextPosition;
						reExploreAmmountOfNodes -= 1;
						if (verbose) {
							System.out
									.println("[INFO NEXT NODE] Joined the current node on timestip " + time.getTime());
							if (!this.reservations.isEmpty())
								System.out.println("[INFO NEXT NODE] " + this.reservations.get(0));
						}
					}
					double moved = progress.distance().doubleValue(SI.METER);
					this.battery.decreaseCharge(moved);
					this.totalDistanceTravelled += moved;
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}
					if (verbose)
						System.out.println("[INFO] AGV has moved");

					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}

				} else {
					// TODO possible problem here
					// we cant move so we check if the reservations are still correct
					// System.err.println(this + " need to check what happend with the
					// reservations");
					// System.out.println(nodeOn.routeReservations);
					this.startEdgeChasing(time);
					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}

				}

			} else {
				if (this.getPosition().get().equals(this.reservations.get(0).responsibleNode.position)) {
					this.reservations.remove(0);
				} else {
					new SimulationException(
							"Moving from " + this.getPosition().get() + " to " + this.reservations.get(0));
					this.deleteIntention();
					this.reservations = null;

					if (reservations == null && !isOnNode) {
						darModel.AGVNeedsRescue(this, time);
						return;
					}
					// throw new IllegalStateException("Trying to move towards position for which
					// there is no connection." + " position = " + this.getPosition().get() + ",
					// nextPosition = " + nextPosition);
				}
			}
		}

	}

	protected abstract void intendTask(TimeLapse time);

	protected static double roundToClosestEven(double d) {
		return Math.round(d / 2) * 2;
	}

	public void addParcel(CarPackage parcel, TimeLapse time) {
		this.pickUpTaskOptions.clear();
		this.battery.decreaseCharge(SimulationSettings.BATTERY_LOAD_IMPACT);
		this.parcel = parcel;
		parcel.task.pickupTime = time.getTime();
		parcel.task.pickedup = true;
		parcel.task.agvID=this.ID;
		pdpModel.pickup(this, parcel, time);
		doingPickUp = false;
		//this.waiting=true;
		//this.timeToWait=((StorageAgent)nodeOn).loadTime/time.getTickLength() /2;
		if (this.verbose) {
			System.out.println("Current parcel: " + this.parcel);
		}
	}

	public boolean hasParcel() {
		return this.parcel != null;
	}

	private void deliverParcel(CarPackage parcel, @NotNull TimeLapse time) throws SimulationException {
		if (this.verbose) {
			System.out.println("[PARCELINFO] AGVAgent deliverCarParcel " + this.parcel);
		}

		if (this.nodeOn instanceof StorageAgent) {
			StorageAgent sa = (StorageAgent) nodeOn;
			if (!sa.canDeliver(this.ID, parcel.taskId, time.getTime())) {
				throw new SimulationException("AGV " + this.ID % 1000 + "Cant deliver parcel cause no reservation");
			}
			sa.deliverCar(this, parcel.taskId, parcel.carID, time);

			if (allocatedTask instanceof DeliveryTask) {
				this.pdpModel.deliver(this, parcel, time);
				this.darModel.deliverCar(this, parcel, time);
			} else {
				this.darModel.tempMoveFinished(this, parcel, time);
				this.darModel.deliverCar(this, parcel, time);
			}
			this.endPoint = Optional.absent();
			this.reservations = null;
			this.parcel = null;
			this.allocatedTask = null;
			this.pickUpTaskOptions.clear();
			this.battery.decreaseCharge(SimulationSettings.BATTERY_LOAD_IMPACT);
			if (this.verbose) {
				System.out.println("[INFO] Delivered " + parcel + ".");
			}
		} else {
			System.err.println("[ERROR] Trying to deliver a carPackage to a storage Agent, IS NOT POSSIBLE");
			throw new SimulationException(
					"Trying to deliver a carPackage to a storage Agent, IS NOT POSSIBLE for AGV " + this.ID % 1000);
		}
	}

	/***********************
	 * Methods for init *
	 ***********************/

	@SuppressWarnings("unchecked")
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		this.pdpModel = pPdpModel;
		this.roadModel = pRoadModel;
		this.staticGraph = (ListenableGraph<LengthData>) ((DynamicGraphRoadModelImpl) pRoadModel).getGraph();
	}

	public Optional<Point> getPosition() {
		try {
			if (roadModel.containsObject(this))
				return Optional.of(roadModel.getPosition(this));
			else
				return Optional.of(new Point(-5, -5));
		} catch (Exception e) {
			return Optional.of(new Point(-5, -5));
		}
	}

	public void setCommDevice(@NotNull CommDeviceBuilder builder) {
		builder.setMaxRange(SimulationSettings.AGV_Length);
		commDevice = builder.build();
	}

	public void setRandomGenerator(RandomProvider provider) {
		this.rng = provider.newInstance();

	}

	public void initDARUser(DARModel model) {
		darModel = model;
	}

	/*********************************
	 * Methods for statsTracker
	 *********************************/

	public long getIdleTime() {
		return this.idleTime;
	}

	public String getCarId() {
		if (allocatedTask == null)
			return "";
		return allocatedTask.carId;
	}

	public double getRemainingBatteryCapacityPercentage() {
		return battery.getRemainingCharge();
	}

	public long getIntendedArrivalTime() {
		if (reservations == null)
			return 0;
		if (reservations.isEmpty())
			return 0;
		return reservations.get(reservations.size() - 1).endTime;
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof AGVAgent))
			return false;
		if (((AGVAgent) o).ID == this.ID)
			return true;
		return false;
	}

	public Battery getBattery() {
		return this.battery;

	}

	public long getChargeTime() {
		return chargeTime;
	}

	public String toString() {
		try {
			if (this.isRegistered() && this.getPosition().isPresent()) {
				return " AGV " + this.ID % 1000 + " on location " + this.getPosition().get().toString()
						+ " with package " + this.parcel;
			} else
				return " AGV " + this.ID % 1000 + " with no location ";
		} catch (Exception e) {
			e.printStackTrace();
			return " EXCEPTION ENCOUNTERED ";
		}
	}

	protected void startEdgeChasing(TimeLapse time) {
		if (verbose)
			System.out.println("[INFO] " + this + " has started an EDGE CHASE");
		new DeadLockDetectionAnt(this,dl).handleAnt(nodeOn, time);
		// commDevice.send(new DeadLockDetectionAnt(this), nodeOn);
	}

	public long getEndReservation(TimeLapse time) {
		try {
			return nodeOn.getCurrentReservation(time).endTime;
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public abstract AGVAgent createFalseCopy(InfrastructureAgent ia, double agvSpeed, TimeLapse time);

	protected abstract void reservationExtendedReSearchNeeded(TimeLapse time);

	public abstract void searchForRestingPlace(Set<Point> nodes, long endTime, TimeLapse time,
			ListenableGraph<LengthData> graph);

	public Optional<Point> finalPoint() {
		return this.endPoint;
	}

	public void setWatitTime(long timeToWait, TimeLapse time) {
		this.waiting = true;
		this.timeToWait = timeToWait / 1000 * time.getTickLength();
		this.nodeOn.extendReservationTime(time, timeToWait + 4000, this);
	}

	public long getWaitingTime() {
		return this.timeToWait * SimulationSettings.TICK_LENGTH;
	}

	public abstract boolean lookForPath(TimeLapse time, boolean b, ListenableGraph<LengthData> dynamicGraph,
			boolean checkForDependentAnts);

	public abstract boolean selectShelter(TimeLapse timeLapse, ListenableGraph<LengthData> dynamicGraph);

	public void returnCar(TimeLapse time) {
		if (this.allocatedTask instanceof HoldingTask) {
			this.endPoint = Optional.of(this.allocatedTask.beginPosition);
			this.waiting = false;
			this.timeToWait = 0;
			this.sendExplorationAnts = 0;
			this.sendIntentionAnts = 0;
			this.explorationAnts.clear();
			this.intentionAnts.clear();
			this.broadCastAnts.clear();
			this.lookForPath(time, true, this.staticGraph, false);
			darModel.tempMoveFinished(this, parcel, time);
		}
	}

	public void discardTask() {
		this.pickUpTaskOptions.clear();
		this.doingPickUp = false;
	}

	public void reset() {
		if (isOnNode) {
			this.intention = Optional.absent();
			this.reservations = null;
			this.endPoint = Optional.absent();
			this.waiting = false;
			this.timeToWait = 0;
			this.sendExplorationAnts = 0;
			this.sendIntentionAnts = 0;
			this.explorationAnts.clear();
			this.intentionAnts.clear();
			//this.broadCastAnts.clear();
		}
	}
}
