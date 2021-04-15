package agents;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import models.DARModel;

public class ExportStorage extends StorageAgent {

	public ExportStorage(Point position, int nodeDistance, double agvSpeed, long tickLength, boolean verbose,
			RandomGenerator rng, ListenableGraph<LengthData> staticGraph, DARModel darModel, 
			long ttl, boolean only_1_taskCons, int max_fast_coalition, int HEUR_TASKCONS_SIZE_FACTOR,
			int HEUR_PICKUPTASK_SIZE_FACTOR, int HEUR_1_TASK_AMMOUNT, int HEUR_HOUR_REMAINING_TIME,
			int HEUR_30MIN_REMAINING_TIME, int HEUR_15MIN_REMAINING_TIME, int HEUR_URGENT_TIME) {
		super(position, nodeDistance, agvSpeed, tickLength, verbose, rng, staticGraph, darModel, 2,ttl,
				only_1_taskCons, max_fast_coalition, HEUR_TASKCONS_SIZE_FACTOR, HEUR_PICKUPTASK_SIZE_FACTOR,
				HEUR_1_TASK_AMMOUNT, HEUR_HOUR_REMAINING_TIME, HEUR_30MIN_REMAINING_TIME, HEUR_15MIN_REMAINING_TIME,
				HEUR_URGENT_TIME);
	}

	public void afterTick(TimeLapse timeLapse) {
		super.afterTick(timeLapse);

		if (!this.carIDs.isEmpty()) {
			this.carIDs.clear();
			darModel.addCarToImport(rng);
		}
	}

	@Override
	public boolean canRestHere(AGVAgent agv) {
		return false;
	}

	@Override
	public boolean canSearchHere(AGVAgent agv) {
		return false;
	}
}
