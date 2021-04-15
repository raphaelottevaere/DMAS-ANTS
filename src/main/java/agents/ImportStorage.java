package agents;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import ants.path.ChargingExplorationAnt;
import ants.path.PathAnt;
import ants.path.TaskExplorationAnt;
import ants.path.TaskIntentionAnt;
import ants.path.TaskPathExplorationAnt;
import graphs.AStar;
import models.DARModel;
import reservations.TaskReservation;
import tasks.BasicTask;
import tasks.DeliveryTask;

public class ImportStorage extends StorageAgent {
	
	private boolean import_1_cons = false;

	public ImportStorage(Point position, int nodeDistance, double agvSpeed, long tickLength, boolean verbose,
			RandomGenerator rng, ListenableGraph<LengthData> staticGraph, DARModel darModel, boolean import_1_cons,long ttl, boolean only_1_taskCons, int max_fast_coalition, int HEUR_TASKCONS_SIZE_FACTOR,int HEUR_PICKUPTASK_SIZE_FACTOR,int HEUR_1_TASK_AMMOUNT,int HEUR_HOUR_REMAINING_TIME,
			int HEUR_30MIN_REMAINING_TIME, int HEUR_15MIN_REMAINING_TIME, int HEUR_URGENT_TIME) {
		super(position, nodeDistance, agvSpeed, tickLength, verbose, rng, staticGraph, darModel, 40,ttl,only_1_taskCons, max_fast_coalition,HEUR_TASKCONS_SIZE_FACTOR,HEUR_PICKUPTASK_SIZE_FACTOR, HEUR_1_TASK_AMMOUNT, HEUR_HOUR_REMAINING_TIME,
				HEUR_30MIN_REMAINING_TIME, HEUR_15MIN_REMAINING_TIME, HEUR_URGENT_TIME);
		this.import_1_cons=import_1_cons;
	}

	private String randomCarId(RandomGenerator rng) {
		return ("IA" + this.getPosition().get().x + this.getPosition().get().y + "-" + rng.nextInt(10000));
	}

	public void addCarToImport(String car) {
		this.carIDs.addLast(randomCarId(rng));
	}

	public String getNewCarId() {
		return randomCarId(this.rng);
	}

	@Override
	public boolean canRestHere(AGVAgent agv) {
		return false;
	}

	@Override
	public boolean canSearchHere(AGVAgent agv) {
		return false;
	}
	
	public boolean lookingForTaskConsumers() {
		if (import_1_cons)
			return this.taskConsumers.isEmpty() && this.routeReservations.isEmpty();

		return super.lookingForTaskConsumers();
	}

	public void handleTaskIntentionAnt(TaskIntentionAnt ant, TimeLapse timeLapse) {
		if (import_1_cons) {
			if (ant.checkEndPoint(this.position)) {
				CommUser agent = ant.agent;
				TaskReservation tr = taskConsumers.get(agent);
				if (tr == null) {
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
		} else {
			super.handleTaskIntentionAnt(ant, timeLapse);
		}
	}

	public void handleTaskExplorationAnt(TaskExplorationAnt ea, TimeLapse timeLapse) {
		if (import_1_cons) {
			if (verbose)
				System.out.println("Handling ExplorationAnt " + this.position + "; for AGV " + ea.sendBy.toString());

			if (ea.isReturning)
				handleReturningAnt(ea, timeLapse);
			else {
				addMeToAntNonRouteBinding(ea, timeLapse);

				if (!ea.checkEndPoint(this.position)) {
					sendAllongPath(ea, timeLapse);
				} else {
					if (tasks.isEmpty())
						return;
					// Add my taskOptions
					TaskExplorationAnt ta = ea;
					addTaskOptions(ta, timeLapse);
					ta.isReturning = true;
					List<PathAnt> ants = new ArrayList<PathAnt>();
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
							ea.holdingTask = true;
							ChargingExplorationAnt ba = new ChargingExplorationAnt(path, ea.agent);
							ba.sendBy = this;
							ba.addRouteReservation(ta.getLastReservation());
							sendTimeDependantAnts.put(ba.id, 0);
							dependantAnts.put(ba.id, ta);
							ants.add(ba);
						}
					}
					outGoingAnts.addAll(ants);
				}
			}
		} else {
			super.handleTaskExplorationAnt(ea, timeLapse);
		}
	}
}
