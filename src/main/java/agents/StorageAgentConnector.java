package agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import models.DARModel;
import simulator.SimulationException;
import tasks.DeliveryTask;
import tasks.TreeNode;

public class StorageAgentConnector implements RoadUser {

	private Point position;
	public DualStorageAgent da1;
	public DualStorageAgent da2;
	private DARModel darModel;

	private List<DeliveryTask> tasks = new ArrayList<DeliveryTask>();

	public StorageAgentConnector(DARModel darModel) {
		this.darModel = darModel;
	}

	public void setDualAgent(DualStorageAgent da1, DualStorageAgent da2){
		if(da1 == null || da2 ==null)
			new SimulationException("").printStackTrace();;
		this.da1 = da1;
		this.da2 = da2;
		this.position = new Point((da1.position.x + da2.position.x) / 2, (da1.position.y + da2.position.y) / 2);
	}

	public void registerTask(DeliveryTask st) {
		this.tasks.add(st);
		calculateMinimumAmmountOfMoves();
	}
	
	public void registerMultipleTask(Collection<DeliveryTask> st) {
		this.tasks.addAll(st);
		calculateMinimumAmmountOfMoves();
	}

	private void calculateMinimumAmmountOfMoves() {
		da1.SACRemoveTASKS();
		da2.SACRemoveTASKS();
		darModel.removePointFromTaskList(da1);
		darModel.removePointFromTaskList(da2);
		
		LinkedList<String> cars = (LinkedList<String>) da1.getCarIds();
		int size = cars.size();
		
		if(size == tasks.size()) {
			for(DeliveryTask bt: this.tasks) {
				if(cars.indexOf(bt.carId) < size/2) {
					da1.SACAddTask(bt);
				}else {
					da2.SACAddTask(bt);
				}
			}
			return;
		}
		
		List<TreeNode> todoNodes = new ArrayList<TreeNode>();
		List<TreeNode> nextNodes = new ArrayList<TreeNode>();
		
		todoNodes.add(new TreeNode(null, null, 0, 0, false));
		
		for(DeliveryTask st: tasks) {
			int begin = cars.indexOf(st.carId)+1;
			int end = size + 1 - begin;
			for(TreeNode tn: todoNodes) {
				tn.generateChilderen(st, begin, end);
				nextNodes.add(tn.getChild1());
				nextNodes.add(tn.getChild2());
			}
			
			todoNodes = nextNodes;
			nextNodes = new ArrayList<TreeNode>();
		}
		
		int lowestAmmount = size +1;
		TreeNode chosenTn = null;
		
		for(TreeNode tn: todoNodes) {
			if(tn.getTotalTasks(size) < lowestAmmount) {
				lowestAmmount = tn.getTotalTasks(size);
				chosenTn = tn;
			}
		}
		
		if(lowestAmmount == size) {
			for(DeliveryTask bt: this.tasks) {
				if(cars.indexOf(bt.carId) < size/2) {
					da1.SACAddTask(bt);
				}else {
					da2.SACAddTask(bt);
				}
			}
			return;
		}
		
		if(chosenTn == null) {
		 	System.err.println("FOUND NO PLACE FOR THE TASKS");
		}
		
		while(chosenTn.getParent()!=null){
			if(chosenTn.child1) {
				da1.SACAddTask(chosenTn.bt);
			}else {
				da2.SACAddTask(chosenTn.bt);
			}
			chosenTn = chosenTn.getParent();
		};
	}

	public void setCars(LinkedList<String> ids, DualStorageAgent dualStorageAgent) {
		DualStorageAgent otherAgent = this.getOtherAgent(dualStorageAgent);
		dualStorageAgent.SACSetCars(ids);
		Collections.reverse(ids);
		otherAgent.SACSetCars(ids);
	}

	private DualStorageAgent getOtherAgent(DualStorageAgent dualStorageAgent) {
		if (this.da1.equals(dualStorageAgent))
			return da2;
		else
			return da1;
	}

	public void addCar(String id, DualStorageAgent dualStorageAgent) {
		DualStorageAgent otherAgent = this.getOtherAgent(dualStorageAgent);
		dualStorageAgent.SACaddCar(id);
		otherAgent.SACaddCarEND(id);
	}

	public Optional<Point> getPosition() {
		return Optional.of(this.position);
	}

	public void removeCar(String carId, DualStorageAgent dualStorageAgent) {
		DualStorageAgent otherAgent = this.getOtherAgent(dualStorageAgent);
		dualStorageAgent.SACremoveCar(carId);
		otherAgent.SACremoveCar(carId);
	}

	@Override
	public void initRoadUser(RoadModel model) {
		model.addObjectAt(this, this.position);
	}

}
