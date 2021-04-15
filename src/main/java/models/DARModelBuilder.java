package models;

import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.RoadModel;

import experiments.ExperimentParameters;

public class DARModelBuilder extends AbstractModelBuilder<DARModel, DARUser> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final long tickLength;
	public final boolean verbose;
	public final ExperimentParameters p;

	public DARModelBuilder(long tickLength, boolean verbose,ExperimentParameters p) {
        this.tickLength = tickLength;
        this.verbose = verbose;
        this.p=p;

        setProvidingTypes(DARModel.class);
        setDependencies(RoadModel.class, SimulatorAPI.class);
	}

	public DARModel build(@NotNull DependencyProvider dependencyProvider) {
        return new DARModel(
                dependencyProvider.get(RoadModel.class),
                dependencyProvider.get(SimulatorAPI.class),
                tickLength,
                verbose, 
                p
        );
	}

}
