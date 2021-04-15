package agents;

import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import models.DARModel;

public class HoldIA extends InfrastructureAgent {

	private InfrastructureAgent connectedAgent = null;
	private boolean checkedConnected = false;
	private boolean hold_ia_active= false;

	public HoldIA(Point position, int nodeDistance, double agvSpeed, long tickLength, boolean verbose,
			ListenableGraph<LengthData> staticGraph, DARModel darModel, RandomGenerator rng, boolean active) {
		super(position, nodeDistance, agvSpeed, tickLength, verbose, staticGraph, darModel, rng);
		hold_ia_active=active;
	}

	public void afterTick(@NotNull TimeLapse timeLapse) {
		if (!checkedConnected && this.connectedAgent == null && !trie && !neighbours.isEmpty()) {
			for (BasicAgent a : neighbours) {
				if (a instanceof RestingAgent || a instanceof ChargingStation || a instanceof StorageAgent) {
					this.connectedAgent = (InfrastructureAgent) a;
					this.checkedConnected = true;
					break;
				}

			}
			this.checkedConnected = true;
		}

		super.afterTick(timeLapse);
	}

	public long nonBlockedCheck(Point point, TimeLapse time, AGVAgent agv) {
		if (hold_ia_active) {
			if (connectedAgent == null)
				return (long) 0d;
			if (point.equals(connectedAgent.position))
				return connectedAgent.endCurrentReservationMotAgv(time, agv);
			else
				return (long) 0d;
		} else {
			return (long) 0d;
		}
	}

}
