package agents;


import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

import ants.*;
import ants.path.ChargingExplorationAnt;
import ants.path.ChargingIntentionAnt;
import ants.path.PathExplorationAnt;
import ants.path.PathIntentionAnt;
import ants.path.TaskExplorationAnt;
import ants.path.TaskIntentionAnt;

/**
 * Represent basic agent structure
 * @author rapha
 *
 */
public interface BasicAgent extends CommUser, TickListener{

	void handleChargingIntentionAnt(ChargingIntentionAnt chargingIntentionAnt, TimeLapse timeLapse);

	void handlePathIntentionAnt(PathIntentionAnt pathIntentionAnt, TimeLapse timeLapse);

	void handleChargingExplorationAnt(ChargingExplorationAnt chargingExplorationAnt, TimeLapse timeLapse);

	void handlePathExplorationAnt(PathExplorationAnt pathExplorationAnt, TimeLapse timeLapse);

	void handleTaskExplorationAnt(TaskExplorationAnt taskExplorationAnt, TimeLapse timeLapse);

	void handleTaskIntentionAnt(TaskIntentionAnt taskIntentionAnt, TimeLapse timeLapse);

	void handleDeadLockDetectionAnt(DeadLockDetectionAnt deadLockDetectionAnt, TimeLapse timeLapse);

	void handleRestingPlaceAnt(findRestingPlaceAnt rp, TimeLapse timeLapse);

}
