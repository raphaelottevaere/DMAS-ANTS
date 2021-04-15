package stats;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STARTED;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STOPPED;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import models.DAREventType;
import models.DARModel;
import simulator.SimulationSettings;
import tasks.BasicTask;

public class StatsTracker extends AbstractModelVoid implements StatsProvider {

	private Clock clock;
	@SuppressWarnings("unused")
	private PDPModel pdpModel;
	private DARModel darModel;
	private RoadModel roadModel;
	private EventDispatcher eventDispatcher;
	private StatsListener statsListener;

	public StatsTracker(Clock clock, PDPModel pdpModel, DARModel dar, RoadModel roadModel) {
		this.clock=clock;
		this.pdpModel=pdpModel;
		this.roadModel=roadModel;
		this.darModel=dar;
		this.eventDispatcher = new EventDispatcher(StatsProvider.EventTypes.values());		
		statsListener=new StatsListener(clock,darModel);
		this.clock.getEventAPI().addListener(statsListener, STARTED, STOPPED);
		this.darModel.getEventAPI().addListener(statsListener,DAREventType.values());
		pdpModel.getEventAPI().addListener(statsListener, PDPModelEventType.values());
	}

	@Override
	public StatsDTO getStatistics() {
		
		StatsListener tl = statsListener;

        long compTime = tl.computationTime;
        if (compTime == 0) {
            compTime = System.currentTimeMillis() - tl.startTimeReal;
        }

        return new StatsDTO(
                tl.totalSingleTasks, 
                tl.CreatedPackage,
                tl.pickedUpPackage,
                tl.totalTempPickups,
                tl.travelTime,
                tl.totalIdleTime,
                compTime,
                clock.getCurrentTime(),
                false,
                SimulationSettings.AGV_AMMOUNT,
                tl.totalChargingTime,
                tl.totalTardiness,
                clock.getTimeUnit(),
                roadModel.getDistanceUnit(),
                roadModel.getSpeedUnit(),
                tl.totalTempmoves,
                tl.stuckAGV,
                tl.totaldeadlocks,
                tl.deadlocksSolveTime,
                tl.droppedCars,
                tl.totalpickups,
                tl.totalmoves,
                tl.BetterPathSelected,
                tl.BetterTaskSelected,
                tl.totalcharging,
                tl.total_to_Late_deliveries, 
                tl.total_made_tasks);
	}
	
	public List<BasicTask> getTasksStatistics() {
		StatsListener tl = statsListener;
        return tl.completedTasks;
	}

	public EventAPI getEventAPI() {
		return eventDispatcher.getPublicEventAPI();
	}
	
    public static StatsTrackerBuilder builder() {
        return new StatsTrackerBuilder();
    }
    
    @NotNull
    @Override
    public <U> U get(Class<U> type) {
        return type.cast(this);
    }
    
    public StatsListener getStatsListener() {
        return statsListener;
    }

}
