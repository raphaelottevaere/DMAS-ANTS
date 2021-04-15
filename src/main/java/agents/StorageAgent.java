package agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import ants.findRestingPlaceAnt;
import ants.path.ChargingExplorationAnt;
import ants.path.PathAnt;
import ants.path.PathExplorationAnt;
import ants.path.TaskExplorationAnt;
import ants.path.TaskIntentionAnt;
import ants.path.TaskPathExplorationAnt;
import graphs.AStar;
import models.DARModel;
import reservations.RouteReservation;
import reservations.TaskOptions;
import reservations.TaskReservation;
import simulator.SimulationSettings;
import tasks.BasicTask;
import tasks.CarPackage;
import tasks.HoldingTask;
import tasks.DeliveryTask;

/**
 * Special subcase of the basic infrastructure agent Needs to process the
 * movement of goods to the AGV and needs to deal with TaskAllocation and
 * RouteAlloction ants differently
 * 
 * @author rapha
 *
 */

public class StorageAgent extends InfrastructureAgent {

	public LinkedList<String> carIDs = new LinkedList<String>();
	public List<BasicTask> tasks = new LinkedList<BasicTask>();
	public Map<AGVAgent, TaskReservation> taskConsumers = new HashMap<AGVAgent, TaskReservation>();
	long loadTime;
	protected final int maxCars;
	protected List<HoldingTask> pickedUpTasks = new ArrayList<HoldingTask>();
	protected AGVAgent returningAgent;
	protected final boolean only_1_TaskConsumer;
	protected final int max_fast_coalition;
	
	protected final int HEUR_TASKCONS_SIZE_FACTOR;
	protected final int HEUR_PICKUPTASK_SIZE_FACTOR;
	protected final int HEUR_1_TASK_AMMOUNT;
	
	protected final int  HEUR_HOUR_REMAINING_TIME;
	protected final int HEUR_30MIN_REMAINING_TIME;
	protected final int HEUR_15MIN_REMAINING_TIME;
	protected final int HEUR_URGENT_TIME;

	public StorageAgent(Point position, int nodeDistance, double agvSpeed, long tickLength, boolean verbose,
			RandomGenerator rng, ListenableGraph<LengthData> staticGraph, DARModel darModel, int maxCars, long ttl, boolean only_1_taskCons, int max_fast_coalition, int HEUR_TASKCONS_SIZE_FACTOR,int HEUR_PICKUPTASK_SIZE_FACTOR,int HEUR_1_TASK_AMMOUNT,int HEUR_HOUR_REMAINING_TIME,
			int HEUR_30MIN_REMAINING_TIME, int HEUR_15MIN_REMAINING_TIME, int HEUR_URGENT_TIME) {
		super(position, nodeDistance, agvSpeed, tickLength, verbose, staticGraph, darModel, rng);
		this.maxCars = maxCars;
		loadTime = ttl;
		waitingForDependant = (int) SimulationSettings.WAITING_ON_DEPENDANT_ANTS;
		this.only_1_TaskConsumer = only_1_taskCons;
		this.max_fast_coalition=max_fast_coalition;
		
		this.HEUR_TASKCONS_SIZE_FACTOR=HEUR_TASKCONS_SIZE_FACTOR;
		this.HEUR_PICKUPTASK_SIZE_FACTOR=HEUR_PICKUPTASK_SIZE_FACTOR;
		this.HEUR_1_TASK_AMMOUNT=HEUR_1_TASK_AMMOUNT;
		
		this.HEUR_HOUR_REMAINING_TIME=HEUR_HOUR_REMAINING_TIME;
		this.HEUR_30MIN_REMAINING_TIME=HEUR_30MIN_REMAINING_TIME;
		this.HEUR_15MIN_REMAINING_TIME=HEUR_30MIN_REMAINING_TIME;
		this.HEUR_URGENT_TIME=HEUR_URGENT_TIME;
	}

	public void afterTick(TimeLapse timeLapse) {
		super.afterTick(timeLapse);

		// Remove all AGV's that didn't update their intentions
		List<AGVAgent> toRemove = new ArrayList<AGVAgent>();
		for (Entry<AGVAgent, TaskReservation> es : taskConsumers.entrySet()) {
			if (es.getValue().evaporates == 0)
				toRemove.add(es.getKey());

			else {
				es.getValue().evaporates -= 1;
			}
		}

		for (AGVAgent remove : toRemove) {
			taskConsumers.remove(remove);
		}

		if (tasks.size() == 0 && pickedUpTasks.size() != 0 && returningAgent == null && this.routeReservations.isEmpty()) {
			HoldingTask task = pickedUpTasks.get(pickedUpTasks.size() - 1);
			task.returnAllowed = true;
			task.agv.returnCar(timeLapse);
			this.returningAgent = task.agv;
		}
		
		if(returningAgent!=null && !returningAgent.active  && returningAgent.hasParcel()) {
			pickedUpTasks.remove(pickedUpTasks.size() - 1);
			darModel.dropCarParcel(returningAgent, returningAgent.parcel, timeLapse.getTime());
			returningAgent=null;
		}
		
		if(tasks.size()==0 && pickedUpTasks.size()==0 && darModel.getAllTaskPoints().contains(this.position)) {
			darModel.removePointFromTaskList(this);
		}
		//36.0,38.0
		if(this.position.equals(new Point(36,38)))
			System.out.print("");
		if(tasks.size()!=0 && !darModel.getAllTaskPoints().contains(this.position)) {
			darModel.addPointToTaskList(this, tasks.get(tasks.size()-1));
		}
	}

	public void registerTask(DeliveryTask st) {	
		if (carIDs.contains(st.carId)) {

			if (carIDs.size() == 1) {
				tasks.add(st);
				this.darModel.addPointToTaskList(this, st);
				return;
			}

			st.beginPosition = this.position;
			int location = carIDs.indexOf(st.carId);
			// for all cars before the one that has the tasks			
			if (tasks.size() - 1 >= location) {
				tasks.remove(location);
				tasks.add(location, st);
			} else {
				for (int i = tasks.size(); i != location; i++) {
					try {
						tasks.add(new HoldingTask(this.position, st.startTime, carIDs.get(i), st.getEndTime(), st,  HEUR_HOUR_REMAINING_TIME,  HEUR_30MIN_REMAINING_TIME,  HEUR_15MIN_REMAINING_TIME,  HEUR_URGENT_TIME));
					} catch (IndexOutOfBoundsException e) {
						System.out.print("");
						e.printStackTrace();
					}
				}
				tasks.add(location, st);
			}
		}
		st.beginPosition = this.position;
		this.darModel.addPointToTaskList(this, st);
	}

	/****************
	 * ANT HANDLERS *
	 ****************/

	public void handleTaskExplorationAnt(TaskExplorationAnt ea, TimeLapse timeLapse) {
		if (verbose)
			System.out.println("Handling ExplorationAnt " + this.position + "; for AGV " + ea.sendBy.toString());

		if (ea.isReturning)
			handleReturningAnt(ea, timeLapse);
		else {
			addMeToAntNonRouteBinding(ea, timeLapse);

			if (!ea.checkEndPoint(this.position)) {
				sendAllongPath(ea, timeLapse);
			} else {
				
				if(tasks.isEmpty())
					return;
				// Add my taskOptions
				TaskExplorationAnt ta = ea;
				addTaskOptions(ta, timeLapse);
				ta.isReturning = true;
				List<PathAnt> ants = new ArrayList<PathAnt>();

				if (only_1_TaskConsumer) {
					BasicTask bt = tasks.get(0);
					if (bt instanceof DeliveryTask) {
						DeliveryTask st = (DeliveryTask) bt;
						List<List<Point>> possiblePaths = AStar.getInstance().getAlternativePaths(1, this.position,
								st.endPosition, rng, staticGraph);
						for (List<Point> path : possiblePaths) {
							PathAnt ba = new TaskPathExplorationAnt(path, ta.agent, st.endPosition, st);
							ba.sendBy = this;
							ba.addRouteReservation(ta.getLastReservation());
							sendTimeDependantAnts.put(ba.id, 0);
							dependantAnts.put(ba.id, ta);
							ants.add(ba);
						}
					} else {
						Point endPoint = this.getClosestChargingStation().position;
						List<List<Point>> possiblePaths = AStar.getInstance().getAlternativePaths(1, this.position,
								endPoint, rng, staticGraph);
						for (List<Point> path : possiblePaths) {
							ea.holdingTask=true;
							ChargingExplorationAnt ba = new ChargingExplorationAnt(path, ea.agent);
							ba.sendBy = this;
							ba.addRouteReservation(ta.getLastReservation());
							sendTimeDependantAnts.put(ba.id, 0);
							dependantAnts.put(ba.id, ta);
							ants.add(ba);
						}
					}
				} else {
					//maybe change so only the ammount of tasks that can be pickedUp are done?
					// This is prob not very efficient but maybe just leave it like this
					
					for (BasicTask bt : tasks) {
						if (bt instanceof DeliveryTask) {
							DeliveryTask st = (DeliveryTask) bt;
							List<List<Point>> possiblePaths = AStar.getInstance().getAlternativePaths(1, this.position,
									st.endPosition, rng, staticGraph);
							for (List<Point> path : possiblePaths) {
								PathAnt ba = new TaskPathExplorationAnt(path, ta.agent, st.endPosition, st);
								ba.sendBy = this;
								ba.addRouteReservation(ta.getLastReservation());
								sendTimeDependantAnts.put(ba.id, 0);
								dependantAnts.put(ba.id, ta);
								ants.add(ba);
							}
						}
					}
				}
				outGoingAnts.addAll(ants);
			}
		}

	}

	protected void checkDependantAnts(TaskPathExplorationAnt tpa) {
		PathAnt dependant = dependantAnts.get(tpa.id);
		if (dependant != null) {
			dependant.setDependentAnt(tpa);
		}
	}

	@Override
	public void handleRestingPlaceAnt(findRestingPlaceAnt rp, TimeLapse timeLapse) {
		if(rp.agent.ID%1000==13)
			System.out.print("");
		
		if (!rp.hasVisitedNode(this.position)) {
			if (canRestHere(rp.agent)) {
				rp.addVisitedNode(this.position);
				rp.wasAccepted();
				findRestingPlaceAnt tempAnt = rp.copy();
				tempAnt.acceptedPoint = this;
				tempAnt.setAccepted(true);
				tempAnt.isReturning = true;
				tempAnt.handleAnt((BasicAgent) rp.getAgent(), timeLapse);
			} else {
				super.handleRestingPlaceAnt(rp, timeLapse);
			}
		}
	}

	public void handlePathExplorationAnt(PathExplorationAnt explorationAnt, TimeLapse timeLapse) {
		if (verbose)
			System.out.println("[INFO] Handling PathExplorationAnt " + this.position + "; for "
					+ explorationAnt.sendBy.toString());

		if (explorationAnt.sendBy.equals(this) && explorationAnt instanceof TaskPathExplorationAnt) {
			TaskPathExplorationAnt tpa = (TaskPathExplorationAnt) explorationAnt;
			this.checkDependantAnts(tpa);
			return;
		}

		super.handlePathExplorationAnt(explorationAnt, timeLapse);
	}

	public void handleTaskIntentionAnt(TaskIntentionAnt ant, TimeLapse timeLapse) {
		if (ant.checkEndPoint(this.position)) {
			CommUser agent = ant.agent;
			TaskReservation tr = taskConsumers.get(agent);
			if (tr == null) {
				if (only_1_TaskConsumer) {
					if (taskConsumers.size() < 1) {
						ant.isReturning = true;
						ant.setAccepted(true);
						// addMeToAntNonRouteBinding(ant, timeLapse);
						taskConsumers.put((AGVAgent) agent, new TaskReservation((AGVAgent) agent));
					} else {
						ant.isReturning = true;
						ant.setAccepted(false);
					}

				} else {

					if (taskConsumers.size() < tasks.size() && taskConsumers.size()< max_fast_coalition) {
						ant.isReturning = true;
						ant.setAccepted(true);
						taskConsumers.put((AGVAgent) agent, new TaskReservation((AGVAgent) agent));
					} else {
						ant.isReturning = true;
						ant.setAccepted(false);
					}

				}
			} else {
				if (!tasks.isEmpty()) {
					tr.resetEvaporation();
					taskConsumers.put((AGVAgent) agent, tr);
					ant.setAccepted(true);
				} else {
					ant.setAccepted(false);
				}
				ant.isReturning = true;
			}

			handleReturningAnt(ant, timeLapse);
		} else {
			// addMeToAntNonRouteBinding(ant, timeLapse);
			sendAllongPath(ant, timeLapse);
		}
	}

	public void removeTaskIntention(AGVAgent agv) {
		taskConsumers.remove(agv);
	}

	// Might proof to be better to safe the tasksoptions so less calculations needed
	protected void addTaskOptions(TaskExplorationAnt explorationAnt, TimeLapse time) {
		if (tasks.isEmpty())
			return;

		int priority = HEUR_TASKCONS_SIZE_FACTOR * this.taskConsumers.size() + HEUR_PICKUPTASK_SIZE_FACTOR* this.pickedUpTasks.size();

		if (this.tasks.size() == 1) {
			priority += HEUR_1_TASK_AMMOUNT;
		}

		for (BasicTask bt : tasks) {
			explorationAnt.addTaskOptions(
					new TaskOptions(bt, this, priority + bt.getTimePriority(time.getTime()) + bt.getPriority()));
		}

		explorationAnt.calculatePriority();
	}

	public List<TaskOptions> getTaskOptions(TimeLapse time) {
		List<TaskOptions> to = new ArrayList<TaskOptions>();

		if (tasks.isEmpty())
			return to;

		int priority = this.taskConsumers.size() + HEUR_PICKUPTASK_SIZE_FACTOR*this.pickedUpTasks.size();

		if (this.tasks.size() == 1) {
			priority += 2;
		}

		for (BasicTask bt : tasks) {
			to.add(new TaskOptions(bt, this, priority + bt.getTimePriority(time.getTime()) + bt.getPriority()));
		}

		return to;
	}

	@Override
	protected boolean deadLockDetection(RouteReservation newRR, RouteReservation lastReservation) {
		return false;
	}

	/*****************
	 * Extra methods *
	 ****************/

	public LinkedList<String> getCarIds() {
		return this.carIDs;
	}
	
	public void registerTask(Collection<DeliveryTask> st) {
		for(DeliveryTask t: st) {
			this.registerTask(t);
		}
	}

	public void setCars(LinkedList<String> ids) {
		this.carIDs = new LinkedList<String>();
		this.carIDs.addAll(ids);
	}

	public void addCar(String id) {
		carIDs.addFirst(id);
	}

	/**
	 * @return Random Id of a car in its list or NUll if no car is available
	 */
	public String getRandomCarId() {
		Object[] candidates = carIDs.toArray();
		if (candidates.length == 0)
			return null;
		return (String) candidates[rng.nextInt(candidates.length)];
	}

	public boolean canDeliver(int agvId, int taskID, long time) {
		return (agvHasReservation(agvId, time));
	}

	public void deliverCar(AGVAgent agv, int taskID, String carId, TimeLapse time) {
		if (canDeliver(agv.ID, taskID, time.getTime())) {
			if (this.returningAgent != null && this.returningAgent.equals(agv)) {
				this.returningAgent = null;
				this.pickedUpTasks.remove(pickedUpTasks.size() - 1);

				if (this.pickedUpTasks.isEmpty() && tasks.isEmpty()) {
					darModel.removePointFromTaskList(this);
					darModel.addPointToRestPlace(this.position);
				}
				agv.reset();
				agv.seekShelter(time);
			}
			addCar(carId);
			this.extendReservationTime(time, loadTime, agv);
		}
	}

	public BasicTask getFirstTask() {
		if(tasks.isEmpty())
			return null;
		return tasks.get(0);
	}

	public CarPackage getFirstCarPackage(TimeLapse time, AGVAgent agv) {
		BasicTask ts = tasks.get(0);
		if(ts == null) {
			agv.discardTask();
			return null;}
		if (ts instanceof DeliveryTask) {
			DeliveryTask st = (DeliveryTask) ts;
			CarPackage cp = darModel.createCarParcel(ts, this.position, st.endPosition, time.getTime(),agv);
			darModel.removeSingleTaskFromTaskList(st, this, this.pickedUpTasks.isEmpty());
			tasks.remove(0);
			taskConsumers.remove(agv);
			this.removeCar(st.carId);
			ts.pickedup = true;
			extendReservationTime(time, loadTime, agv);
			return cp;
		} else {
			HoldingTask mt = (HoldingTask) ts;
			CarPackage cp = darModel.createCarParcel(ts, this.position, this.position, time.getTime(),agv);
			tasks.remove(0);
			taskConsumers.remove(agv);
			this.removeCar(mt.carId);
			ts.pickedup = true;
			mt.agv=agv;
			this.pickedUpTasks.add(mt);
			extendReservationTime(time, loadTime, agv);
			return cp;
		}
	}

	protected void removeCar(String carId) {
		if(carIDs.get(0).equals(carId))
			carIDs.remove(carId);
		else
			System.err.println("Not picking the fist car");
	}

	public boolean hasTask() {
		return !this.tasks.isEmpty();
	}

	public boolean checkCarHasTask(String s) {
		for (BasicTask b : this.tasks) {
			if (b.carId.equals(s)) {
				return true;
			}
		}
		return false;
	}

	public int getMaxCars() {
		return maxCars;
	}

	@Override
	public boolean canRestHere(AGVAgent agv) {
		return (darModel.inRestingPlaces(this.position) && !this.hasTask()
				&& this.pickedUpTasks.isEmpty() && checkReservationForSingleAGV(agv) );
	}

	protected boolean checkReservationForSingleAGV(AGVAgent agv) {
		for(RouteReservation RR: routeReservations) {
			if(!RR.agv.equals(agv))
				return false;
		}
		return true;
	}

	@Override
	public boolean canSearchHere(AGVAgent agv) {
		return this.canRestHere(agv);
	}

	public boolean lookingForTaskConsumers() {
		if (only_1_TaskConsumer)
			return this.taskConsumers.isEmpty() && this.routeReservations.isEmpty();

		return this.tasks.size() > this.taskConsumers.size() && this.taskConsumers.size() < max_fast_coalition;
	}

	public int getTaskAmmount() {
		return this.pickedUpTasks.size() + this.tasks.size();
	}

	public boolean hasCars() {
		return !this.carIDs.isEmpty();
	}

	public boolean canStoreCars() {
		return this.carIDs.size()< this.maxCars;
	}

	public boolean hasReturn() {
		return !this.pickedUpTasks.isEmpty();
	}

}
