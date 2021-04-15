package agents;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import ants.path.ChargingExplorationAnt;
import ants.path.ChargingIntentionAnt;
import reservations.ChargingOptions;
import reservations.ChargingReservation;
import simulator.SimulationSettings;
import simulator.SimulationException;

public class ChargingStationUtil extends Depot implements TickListener {

	private List<ChargingReservation> chargingReservations = new LinkedList<ChargingReservation>();
	private AGVAgent agv = null;
	public static int count = 0;
	public final int ID;
	private final double chargingAmmount;
	private long treshHoldChargingTime = SimulationSettings.CHARGINGTIME_THRESHHOLD;
	private DARDepot waitLine;

	// Sometimes an agv comes out of the chargingstation with only like 50% charged
	// Has it even charged?

	public ChargingStationUtil(Point position, DARDepot waitLine, double chargingAmmount) {
		super(position);
		this.ID = count++;
		this.waitLine = waitLine;
		this.chargingAmmount = chargingAmmount;
	}

	// We assume same chargers like with a tesla (currently available on site)
	// So 0-100 in 1.5Hours
	// Has a range of 400km (Taken from tesla Model X)
	// 100 kWh battery
	// 39 kWh/160 km
	/*
	 * Divide the load power by 1,000 for a value in kilowatts. For example: 3,680 W
	 * = 3.7 kilowatts Divide the power of your battery (also in kW) by the figure
	 * obtained to get the charging time. For example: 24 kW/ 3.7 kW= 6.5 hours
	 * First calculate your load power (P), by multiplying the voltage (U in volts)
	 * by the current (I, in amps). You get a value in watts. P = U x I For example:
	 * 16 A x 230 V = 3,680 W
	 */
	public void charge(TimeLapse time) {
		if (agv != null) {
			agv.getBattery().increaseCharge(chargingAmmount);
		}
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if (agv != null && !this.chargingReservations.isEmpty() && !this.chargingReservations.get(0).agv.equals(agv)) {
			// if the current agv is not equal to the reservation we throw it out,
			// agv.leaveChargingStation(timeLapse);
		}
		charge(timeLapse);
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// Remove all routeReservations that are older then the current time
		// Remove all routeReservations where the pheromones ran out
		List<ChargingReservation> toRemove = new ArrayList<ChargingReservation>();
		for (ChargingReservation rr : chargingReservations) {
			if (rr.getEndTime() < timeLapse.getTime()) {
				toRemove.add(rr);
				if (agv != null)
					agv.leaveChargingStation(timeLapse);
			} else if (rr.evaporates == 0)
				if (this.agv != rr.agv || !waitLine.contains(rr.agv))
					toRemove.add(rr);
				else if (!rr.agv.equals(agv))
					rr.evaporates -= 1;
		}
		if (!toRemove.isEmpty()) {
			if (SimulationSettings.INFRASTRUCTURE_VERBOSE)
				System.err.println("[INFO] Removed Chargingreservations: " + toRemove);
			chargingReservations.removeAll(toRemove);
		}
	}

	public boolean hasReservation(AGVAgent r, ChargingOptions co, TimeLapse time) {
		if (this.equals(co.getChargingStationUtil())) {
			if (this.chargingReservations.isEmpty()) {
				chargingReservations
						.add(new ChargingReservation(r, SimulationSettings.INTENTION_REFRESH_TIME, time.getTime(),
								time.getTime() + SimulationSettings.MAX_CHARGETIME * SimulationSettings.TICK_LENGTH));
				return true;
			}
			//  might need to expand this, so AGVS are more easily admitted
			else if (this.chargingReservations.get(0).isIn(time.getTime())) {
				if (this.chargingReservations.get(0).agv.equals(r))
					return true;
			}
		}
		return false;
	}

	public ChargingOptions newChargingOptions(ChargingExplorationAnt ca, TimeLapse time,
			ChargingStation chargingStation) {
		ChargingOptions co = new ChargingOptions(chargingStation, this);
		co.setCr(nextReservation(ca, time));
		return co;
	}

	public boolean checkForExistingReservation(ChargingIntentionAnt ant, TimeLapse time) {
		ChargingReservation cr = ant.getchargingOptions().getCr();
		if (chargingReservations.contains(cr)) {
			cr.resetEvaporation();
			return true;
		}
		return false;
	}

	public ChargingReservation nextReservation(ChargingExplorationAnt ca, TimeLapse time) {
		// The last reservation is the moment it enters the ChargingStation, From the
		// moment it enter it can immediately be imported into the chargingStationUtil
		// I think, maybe this should still be tested and looked at! 
		long enterTime;
		if (ca.getLastReservation() == null) {
			// already present on the node, we can take now as the entertime
			enterTime = time.getTime();
		} else
			enterTime = ca.getLastReservation().getEndTime();

		long chargingTime=0;
		try {
			chargingTime = (long) (ca.agent.getBattery().maxCap
					* (1 - ca.agent.getBattery().getRemainingCharge() / 100));
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (chargingReservations.isEmpty()) {
			ChargingReservation cc = new ChargingReservation(ca.agent,
					SimulationSettings.INTENTION_RESERVATION_LIFETIME, enterTime,
					enterTime + chargingTime * SimulationSettings.TICK_LENGTH);
			return cc;
		}

		if (chargingReservations.get(chargingReservations.size() - 1).endTime < enterTime) {
			return new ChargingReservation(ca.agent, SimulationSettings.INTENTION_RESERVATION_LIFETIME, enterTime,
					enterTime + chargingTime * SimulationSettings.TICK_LENGTH);
		}

		if (chargingReservations.get(0).startTime > enterTime) {
			return new ChargingReservation(ca.agent, SimulationSettings.INTENTION_RESERVATION_LIFETIME, enterTime,
					chargingReservations.get(0).startTime);
		}

		for (int i = 0; i < chargingReservations.size(); i++) {
			ChargingReservation cr = chargingReservations.get(i);
			// if (cr.getEndTime() <= enterTime) {
			ChargingReservation cc;
			if (i + 1 < chargingReservations.size()) {
				if (chargingReservations.get(i + 1).getBeginTime() - cr.endTime >= treshHoldChargingTime) {
					cc = new ChargingReservation(ca.agent, SimulationSettings.INTENTION_RESERVATION_LIFETIME,
							cr.getEndTime(), chargingReservations.get(i + 1).getBeginTime());
					return cc;
				}
			} else {
				cc = new ChargingReservation(ca.agent, SimulationSettings.INTENTION_RESERVATION_LIFETIME,
						cr.getEndTime(), cr.getEndTime() + chargingTime * SimulationSettings.TICK_LENGTH);
				return cc;
			}
			// }
		}
		new SimulationException("A charging reservation should always be found").printStackTrace();
		return null;
	}

	public boolean AddReservation(ChargingIntentionAnt ca, TimeLapse time) {
		if (ca.getchargingOptions() == null) {
			return false;
		}
		ChargingReservation chargingReservation = ca.getchargingOptions().getCr();

		if (chargingReservations.isEmpty()) {
			chargingReservations.add(chargingReservation);
			return true;
		}

		/*
		 * List<ChargingReservation> toRemove = new ArrayList<>();
		 * for(ChargingReservation cr : chargingReservations) {
		 * if(cr.agv.equals(ca.agent)); toRemove .add(cr); }
		 * chargingReservations.removeAll(toRemove);
		 * 
		 * for (int i = 0; i < chargingReservations.size(); i++) { ChargingReservation
		 * cr = chargingReservations.get(i);
		 * 
		 * if (cr.getEndTime() < chargingReservation.getBeginTime()) { if (i + 1 <
		 * chargingReservations.size()) { if (chargingReservations.get(i +
		 * 1).getBeginTime() >= chargingReservation.endTime) {
		 * chargingReservations.add(chargingReservation); chargingReservations.sort(new
		 * ReservationComparator()); return true; } } else { // We have hit the last
		 * reservation, so the endTime doesnt matter in this case
		 * chargingReservations.add(chargingReservation); chargingReservations.sort(new
		 * ReservationComparator()); return true; } } }
		 */
		return false;
	}

	public boolean addAGV(AGVAgent a, TimeLapse time) {
		if (this.chargingReservations.isEmpty()) {
			return false;
		}
		if (this.chargingReservations.get(0).isIn(time.getTime())) {
			this.agv = a;
			return true;
		}
		return false;
	}

	public AGVAgent getAGV() {
		return agv;
	}

	public AGVAgent removeAGV() {
		AGVAgent temp = agv;
		if (temp != null) {
			if (!chargingReservations.isEmpty() && chargingReservations.get(0).agv.equals(temp)) {
				chargingReservations.remove(0);
			}
		}
		agv = null;
		return temp;
	}

	public boolean compare(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof ChargingStationUtil))
			return false;
		if (((ChargingStationUtil) o).ID == this.ID)
			return true;
		return false;
	}

	public boolean hasPlace(AGVAgent agv2) {
		return chargingReservations.isEmpty() || chargingReservations.get(0).agv.equals(agv2);
	}
}
