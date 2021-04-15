package stats;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;

import models.DARModel;

public class StatsTrackerBuilder extends AbstractModelBuilder<StatsTracker, Object>{
	
	private static final long serialVersionUID = 1L;

	@Override
	public StatsTracker build(DependencyProvider dependencyProvider) {
		return new StatsTracker(
				dependencyProvider.get(Clock.class),
				dependencyProvider.get(PDPModel.class),
				dependencyProvider.get(DARModel.class),
				dependencyProvider.get(RoadModel.class)
				
				);
	}
	
	public StatsTrackerBuilder(){
		setDependencies(Clock.class, RoadModel.class, PDPModel.class, DARModel.class);
		setProvidingTypes(StatsProvider.class);
	}

}
