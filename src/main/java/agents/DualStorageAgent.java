package agents;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import models.DARModel;
import tasks.HoldingTask;
import tasks.DeliveryTask;

public class DualStorageAgent extends StorageAgent {

	private StorageAgentConnector sac;

	public DualStorageAgent(Point position, int nodeDistance, double agvSpeed, long tickLength, boolean verbose,
			RandomGenerator rng, ListenableGraph<LengthData> staticGraph, DARModel darModel, int maxCars,
			StorageAgentConnector sac, long ttl, boolean only_1_taskCons, int max_fast_coalition,
			int HEUR_TASKCONS_SIZE_FACTOR, int HEUR_PICKUPTASK_SIZE_FACTOR, int HEUR_1_TASK_AMMOUNT,
			int HEUR_HOUR_REMAINING_TIME, int HEUR_30MIN_REMAINING_TIME, int HEUR_15MIN_REMAINING_TIME,
			int HEUR_URGENT_TIME) {
		super(position, nodeDistance, agvSpeed, tickLength, verbose, rng, staticGraph, darModel, maxCars, ttl,
				only_1_taskCons, max_fast_coalition, HEUR_TASKCONS_SIZE_FACTOR, HEUR_PICKUPTASK_SIZE_FACTOR,
				HEUR_1_TASK_AMMOUNT, HEUR_HOUR_REMAINING_TIME, HEUR_30MIN_REMAINING_TIME, HEUR_15MIN_REMAINING_TIME,
				HEUR_URGENT_TIME);
		this.sac = sac;
	}
	
	public StorageAgentConnector returnConnector() {
		return sac;
	}

	public void registerTask(DeliveryTask st) {
		sac.registerTask(st);
	}

	public void registerTask(Collection<DeliveryTask> st) {
		sac.registerMultipleTask(st);
	}

	public void setCars(LinkedList<String> ids) {
		sac.setCars(ids, this);
	}

	public void addCar(String id) {
		sac.addCar(id, this);
	}

	protected void removeCar(String carId) {
		sac.removeCar(carId, this);
	}

	public void SACaddCar(String id) {
		carIDs.addFirst(id);
	}

	public void SACaddCarEND(String id) {
		carIDs.addLast(id);
	}

	public void SACRemoveTASKS() {
		this.tasks.clear();
	}

	public void SACremoveCar(String carId) {
		this.carIDs.remove(carId);
	}

	public void SACAddTask(DeliveryTask st) {

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
						tasks.add(new HoldingTask(this.position, st.startTime, carIDs.get(i), st.getEndTime(), st, HEUR_HOUR_REMAINING_TIME,  HEUR_30MIN_REMAINING_TIME,  HEUR_15MIN_REMAINING_TIME,  HEUR_URGENT_TIME));
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

	public void SACSetCars(LinkedList<String> ids) {
		super.setCars(ids);
	}

	public void addMultipleTask(List<DeliveryTask> tasks) {
		for (DeliveryTask t : tasks) {
			this.SACAddTask(t);
		}
	}

	public StorageAgentConnector getConnector() {
		return this.sac;
	}

	@Override
	public boolean canRestHere(AGVAgent agv) {
		return (darModel.inRestingPlaces(this.position) && this.carIDs.size() < this.getMaxCars() && !this.hasTask()
				&& this.pickedUpTasks.isEmpty() && checkReservationForSingleAGV(agv));
	}
}
