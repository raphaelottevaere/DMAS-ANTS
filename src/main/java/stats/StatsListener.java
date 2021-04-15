package stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.rinde.rinsim.core.model.pdp.PDPModelEvent;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;

import agents.AGVAgent;
import models.DAREvent;
import models.DAREventType;
import models.DARModel;
import models.DLEvent;
import simulator.SimulationSettings;
import tasks.BasicTask;
import tasks.CarPackage;
import tasks.DeliveryTask;

public class StatsListener implements Listener {

	private Clock clock;

	public int totalSingleTasks = 0;

	public long startTimeReal = 0L;
	public long startTimeSim = 0L;
	public long computationTime = 0L;
	public long simulationTime = 0L;

	public int pickedUpPackage;
	public int totalTempmoves;
	public long totalTardiness;
	public int totalcharging;
	public int currentCharging;
	public long totalChargingTime;
	public int stuckAGV;
	public List<BasicTask> completedTasks = new ArrayList<BasicTask>();
	public int totaldeadlocks;
	public Map<Integer, Long> deadlocks = new HashMap<Integer, Long>();
	public long deadlocksSolveTime;
	public int totalmoves;
	public int droppedCars;
	public int totalpickups;
	private DARModel darModel;
	public long totalIdleTime;
	public long travelTime;
	public int BetterTaskSelected = 0;;
	public int BetterPathSelected = 0;
	public int totalTempPickups = 0;
	public int CreatedPackage = 0;
	public int total_to_Late_deliveries;
	public int total_made_tasks=0;

	public StatsListener(Clock clock, DARModel d) {
		this.clock = clock;
		this.darModel = d;
	}

	public void handleEvent(Event e) {

		// Correct
		if (e.getEventType() == Clock.ClockEventType.STARTED) {
			startTimeReal = System.currentTimeMillis();
			startTimeSim = clock.getCurrentTime();
			computationTime = 0;

			// Correct
		} else if (e.getEventType() == Clock.ClockEventType.STOPPED) {
			computationTime = System.currentTimeMillis() - startTimeReal;
			simulationTime = clock.getCurrentTime() - startTimeSim;
			List<AGVAgent> agvs = darModel.getAllAGV();

			for (AGVAgent agv : agvs) {
				this.totalIdleTime += agv.getIdleTime() / 1000;
				this.travelTime += agv.totalTravelTime / 1000;
			}

			// Correct Every package that is created
		} else if (e.getEventType() == PDPModelEventType.NEW_PARCEL) {
			final PDPModelEvent pme = (PDPModelEvent) e;
			final CarPackage p = (CarPackage) pme.parcel;
			assert p != null;

			CreatedPackage++;

		} else if (e.getEventType() == PDPModelEventType.END_PICKUP) {
			final PDPModelEvent pme = (PDPModelEvent) e;
			final CarPackage p = (CarPackage) pme.parcel;
			assert p != null;

			this.pickedUpPackage++;

		} else if (e.getEventType() == DAREventType.NEW_TASK) {

			final DAREvent pme = (DAREvent) e;
			final CarPackage p = (CarPackage) pme.parcel;
			assert p != null;
			
			total_made_tasks++;
		} else if (e.getEventType() == DAREventType.END_TASK) {

			final DAREvent pme = (DAREvent) e;
			final CarPackage p = (CarPackage) pme.parcel;
			final Vehicle v = pme.vehicle;
			final BasicTask bt = pme.basicTask;
			assert p != null;
			assert v != null;

			totalmoves++;
			if (bt instanceof DeliveryTask) {
				totalSingleTasks++;
				if (bt.completionTime > bt.getEndTime()) {
					total_to_Late_deliveries++;
					totalTardiness += (bt.completionTime - bt.getEndTime()) / 1000;
				}
			}
			completedTasks.add(bt);

		} else if (e.getEventType() == DAREventType.AGV_AT_CHARGING_STATION) {
			final DAREvent pme = (DAREvent) e;
			final Vehicle v = pme.vehicle;
			assert v != null;

			totalcharging++;
			currentCharging++;

		} else if (e.getEventType() == DAREventType.AGV_LEAVING_CHARGING_STATION) {
			final DAREvent pme = (DAREvent) e;
			final Vehicle v = pme.vehicle;
			assert v != null;

			currentCharging--;
			totalChargingTime += pme.time * SimulationSettings.TICK_LENGTH / 1000;

		} else if (e.getEventType() == DAREventType.AGV_STUCK) {
			final DAREvent pme = (DAREvent) e;
			final Vehicle v = pme.vehicle;
			assert v != null;

			stuckAGV++;

		} else if (e.getEventType() == DAREventType.END_TEMP_MOVE) {

			final DAREvent pme = (DAREvent) e;
			final Vehicle v = pme.vehicle;
			assert v != null;

			totalTempmoves++;

		} else if (e.getEventType() == DAREventType.DEADLOCK_ADDED) {

			final DLEvent pme = (DLEvent) e;
			deadlocks.put(pme.d.ID, pme.time);
			totaldeadlocks++;

		} else if (e.getEventType() == DAREventType.DEADLOCK_FINISHED) {
			final DLEvent pme = (DLEvent) e;
			deadlocksSolveTime += (pme.time - deadlocks.get(pme.d.ID)) / 1000;
		} else if (e.getEventType() == DAREventType.PICKUP_TASK) {

			final DAREvent pme = (DAREvent) e;
			final CarPackage p = (CarPackage) pme.parcel;
			final Vehicle v = pme.vehicle;
			assert p != null;
			assert v != null;

			totalpickups++;

		} else if (e.getEventType() == DAREventType.PICKUP_TEMP_TASK) {

			final DAREvent pme = (DAREvent) e;
			final CarPackage p = (CarPackage) pme.parcel;
			final Vehicle v = pme.vehicle;
			assert p != null;
			assert v != null;

			totalTempPickups++;

		} else if (e.getEventType() == DAREventType.DROP_CAR) {

			final DAREvent pme = (DAREvent) e;
			final Vehicle v = pme.vehicle;
			final CarPackage p = (CarPackage) pme.parcel;
			assert v != null;
			assert p != null;
			droppedCars++;

			// Correct
		} else if (e.getEventType() == DAREventType.BETTER_TASK_SELECTED) {
			final DAREvent pme = (DAREvent) e;
			final Vehicle v = pme.vehicle;
			assert v != null;
			BetterTaskSelected++;

			// Correct
		} else if (e.getEventType() == DAREventType.BETTER_PATH_SELECTED) {
			final DAREvent pme = (DAREvent) e;
			final Vehicle v = pme.vehicle;
			assert v != null;
			BetterPathSelected++;
		}
	}

}
