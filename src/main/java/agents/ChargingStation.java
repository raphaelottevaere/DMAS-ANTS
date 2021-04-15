package agents;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import ants.path.ChargingExplorationAnt;
import ants.path.ChargingIntentionAnt;
import models.DARModel;
import reservations.ChargingOptions;
import reservations.RouteReservation;
import simulator.SimulationException;

public class ChargingStation extends InfrastructureAgent {

	public ChargingStationUtil cu;
	private DARDepot leavingLine;
	private DARDepot chargingLine;

	public ChargingStation(Point position, int nodeDistance, double agvSpeed, long tickLength, boolean verbose,
			ListenableGraph<LengthData> staticGraph, DARModel darModel, int maxAGV, double chargingStationRecharge, RandomGenerator rng) {
		super(position, nodeDistance, agvSpeed, tickLength, verbose, staticGraph, darModel, rng);
		this.leavingLine = new DARDepot(position);
		this.chargingLine = new DARDepot(position);

		ChargingStationUtil csu = new ChargingStationUtil(position, chargingLine, chargingStationRecharge);
		darModel.registerTickListener(csu);
		cu = csu;
		
	}

	// maybe change so that if AGV position == (-5,-5) it already is on this node, so just change so it is directly send to the ChargingUtils
	public void handleChargingExplorationAnt(ChargingExplorationAnt explorationAnt, TimeLapse time) {
		if (verbose)
			System.out.println("Handling ChargingExplorationAnt " + this.position + "; for AGV "
					+ explorationAnt.sendBy.toString());

		if (explorationAnt.isReturning)
			handleReturningAnt(explorationAnt, time);
		else {
			addMeToAntNonRouteBinding(explorationAnt, time);

			if (!explorationAnt.checkEndPoint(this.position)) {
				sendAllongPath(explorationAnt, time);
			} else {
				// Add chargingOptions
				ChargingExplorationAnt ca = explorationAnt;
				//change this?
				List<ChargingExplorationAnt> list = addChargingOptions(ca, time);
				for(ChargingExplorationAnt cea: list) {
					cea.isReturning = true;
					handleReturningAnt(cea, time);
				}
			}
		}
	}

	private List<ChargingExplorationAnt> addChargingOptions(ChargingExplorationAnt ca, TimeLapse time) {
		List<ChargingExplorationAnt> list = new ArrayList<ChargingExplorationAnt>();
			ChargingOptions cr = cu.newChargingOptions(ca, time, this);
			ca.addChargingOption(cr);
			list.add(ca);
		return list;
	}

	@Override
	public void handleChargingIntentionAnt(ChargingIntentionAnt ant, TimeLapse time) {
		//addMeToAntNonRouteBinding(ant, time);
		if (ant.checkEndPoint(this.position)) {
			ChargingOptions co = ant.getchargingOptions();
			if (co == null || !co.getChargingStation().equals(this)) {
				ant.setAccepted(false);
			} else {
				ChargingStationUtil csu;
				csu = co.getChargingStationUtil();
				if (csu == cu) {
					boolean accepted = false;
					if (csu.checkForExistingReservation(ant, time)) {
						accepted = true;
					} else {
						accepted = csu.AddReservation(ant, time);
					}
					ant.setAccepted(accepted);
				} else {
					ant.setAccepted(false);
				}
			}
			ant.isReturning = true;
			handleReturningAnt(ant, time);
		} else {
			
			sendAllongPath(ant,time);
		}

	}

	public void afterTick(TimeLapse timeLapse) {
		// Pheromone deletion
		super.afterTick(timeLapse);
		checkMoveToCharging(timeLapse);
		moveAGVonRoad(timeLapse);
	}

	private void checkMoveToCharging(TimeLapse time) {
		// needs to check entire waiting line to see if 1 can be moved
		for (AGVAgent r : chargingLine.getAllAgv()) {
			if (r.co != null) {
				if (!r.co.getChargingStation().equals(this) && r.getPosition().get().equals(this.position)) {
					darModel.AGVNeedsRescue(r, time);
				}
				ChargingStationUtil temp = cu;
				if (temp == null)
					darModel.AGVNeedsRescue(r, time);

				moveAGVtoCharging(r.co, r, time);
				
			}
		}

	}

	private void moveAGVonRoad(TimeLapse time) {
		if (!leavingLine.hasAGV())
			return;
		if (canBePutOnRoad(time)) {
			AGVAgent a = leavingLine.getFirst();
			a = darModel.registerForCharging(a,this, time);

			firstReservation(a, time.getTime());
		}	else {
			if (!routeReservations.isEmpty() && routeReservations.get(0).isIn(time.getTime())) {
				routeReservations.get(0).agv.seekShelter(time);}
		}
			
	}

	public void moveAGVtoOut(TimeLapse time, AGVAgent r, int chargingStationUtilID) {
		ChargingStationUtil csu = this.cu;
		if (csu == null) {
			new SimulationException("No chargingStationUtil with the ID is present on " + this).printStackTrace();
			return;
		}
		if (csu.getAGV().equals(r)) {
			csu.removeAGV();
			leavingLine.addAGVToLine(r);
			r.waiting=true;
		}
	}

	private void moveAGVtoCharging(ChargingOptions co, AGVAgent r, TimeLapse time) {
		if (co == null)
			return;
		ChargingStationUtil csu = co.getChargingStationUtil();
		if (csu == null) {
			new SimulationException("No chargingStationUtil with the ID is present on " + this).printStackTrace();
			return;
		}
		if (csu.getAGV() == null && csu.hasReservation(r, co, time)) {
			csu.addAGV(r, time);
			r.isCharging = true;
			chargingLine.removeAGVFromLine(r);
			if(routeReservations.isEmpty()) {
				//TODO what happend here?
				System.err.println("We have no reservations on this node " + this +" which shoudlnt happen");
			}
			else
				this.routeReservations.remove(0);
		}
	}

	private void addmitAGVWaitLine(AGVAgent r) {
		chargingLine.addAGVToLine(r);
	}

	public boolean canAddmitAGVtoCharging(AGVAgent r, ChargingOptions co, TimeLapse time) {
		ChargingStationUtil csu = co.getChargingStationUtil();
		if (co.getChargingStation().equals(this)) {
			if (cu != null) {
				if (csu.hasReservation(r, co, time)) {
					return true;
				}
			}
		}
		return false;
	}

	public void AGVArrivesAtChargingStation(AGVAgent r, TimeLapse time) {
		if (r.getPosition().get().equals(this.position)) {
			darModel.unregisterForCharging(r);
			addmitAGVWaitLine(r);
			r.waiting = true;
			r.timeToWait=-1;
		}
	}

	public void AGVLeavingChargingStation(AGVAgent r, int csuID, TimeLapse timelapse) {
		// TODO call darModel to send to statsTracker
		moveAGVtoOut(timelapse, r, csuID);
		r.isCharging = false;
		r.waiting = true;
	}

	public boolean canBePutOnRoad(TimeLapse timelapse) {
		if (this.routeReservations.isEmpty())
			return true;
		RouteReservation firstReservation = this.routeReservations.get(0);
		if (firstReservation.isIn(timelapse.getTime())) {
			return false;
		}
		if (timelapse.getTime() + this.robotTimePerHop * 2 < firstReservation.getBeginTime())
			return true;

		return false;
	}
	
	public boolean canSearchHere() {
		return false;
	}
	
	public boolean canRestHere() {
		return false;
	}

	public boolean CUhasPlace(AGVAgent agv) {
		return this.cu.hasPlace(agv);
	}
}
