package ants.path;

import java.util.List;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.BasicAgent;
import reservations.ChargingOptions;
import reservations.RouteReservation;

public class ChargingExplorationAnt extends ExplorationAnt {

	private ChargingOptions chargingOptions = null;

	public ChargingExplorationAnt(List<Point> path, AGVAgent agent, List<RouteReservation> r,
			ChargingOptions c) {
		super(path, agent, r);
		chargingOptions = c;
	}

	public ChargingExplorationAnt(List<Point> path,AGVAgent agent) {
		super(path, agent, null);
		chargingOptions = null;
	}

	public ChargingExplorationAnt(List<Point> path, AGVAgent agent, List<RouteReservation> r,
			ChargingOptions c, int id) {
		super(path, agent, r, id);
		chargingOptions = c;
	}

	@Override
	public ChargingExplorationAnt copy() {
		ChargingExplorationAnt ant = new ChargingExplorationAnt(pathCopy(), agent, reservationsCopy(),
				chargingOptions(), id);
		ant.isReturning = this.isReturning;
		ant.sendBy = this.sendBy;
		return ant;
	}

	public ChargingOptions chargingOptions() {
		return chargingOptions;
	}

	public void addChargingOption(ChargingOptions c) {
		chargingOptions=(c);
	}

	@Override
	public void handleAnt(BasicAgent cm, TimeLapse timeLapse) {
		cm.handleChargingExplorationAnt(this, timeLapse);
	}
}
