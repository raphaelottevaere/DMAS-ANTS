package experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.StatsPanel;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.generator.Depots;
import com.github.rinde.rinsim.scenario.generator.Parcels;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.github.rinde.rinsim.scenario.generator.Vehicles;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.View.Builder;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Optional;

import agents.ChargingStation;
import agents.DecisionAGV;
import agents.RestingAgent;
import graphs.GraphCreator;
import models.DARModel;
import renderers.AGVRenderer;
import renderers.TaskRenderer;
import simulator.SimulationSettings;
import stats.StatsOutput;
import stats.StatsTracker;

public class standardExperiment {
	public int id;
	private ExperimentParameters p;

	public standardExperiment(int id, ExperimentParameters params) {
		this.id = id;
		this.p = params;
	}

	public void run() throws IOException {
		String s = p.getGraphName();
		// long seed = System.currentTimeMillis();

		ArrayList<String> graphs = new ArrayList<String>();
		graphs.add(s);
		ListenableGraph<LengthData> dynamicGraph = new ListenableGraph<LengthData>(GraphCreator.loadGraph(s, graphs));

		// Building ViewBuilder to show custom View
		Builder viewBuilder = View.builder().withTitleAppendix("DMAS Simulator- Experiment " + p.id)
				.with(GraphRoadModelRenderer.builder().withNodeCircles().withDirectionArrows()// .withNodeCoordinates()
						.withMargin(SimulationSettings.AGV_Length))
				// Add custom roadusers here
				.with(RoadUserRenderer.builder().withImageAssociation(DecisionAGV.class, "/agv.png")
						.withImageAssociation(ChargingStation.class, "/chargingStation.png")
						.withImageAssociation(RestingAgent.class, "/rp.png"))
				.with(AGVRenderer.builder()).with(TaskRenderer.builder()).with(StatsPanel.builder())
				.withResolution(SimulationSettings.WIDTH, SimulationSettings.HEIGHT)
				.withSpeedUp(SimulationSettings.SimulationSpeed)
				.withSimulatorEndTime(p.simulationLength)
				.withAutoClose()
				.withAutoPlay()
				//.withAsync()
				;

		ScenarioGenerator generator = ScenarioGenerator.builder()
				.scenarioLength(p.simulationLength)
				.setStopCondition(StatsStopConditions.timeOutEvent())
			    .vehicles(getVehicleGenerator(p.numRobots, p.robotSpeed))
	            .parcels(getDeliveryTaskGenerator())
	            .depots(getDepotGenerator())	      
				.addModel(RoadModelBuilders.dynamicGraph((dynamicGraph))
						.withDistanceUnit(SimulationSettings.DISTANCE_UNIT).withSpeedUnit(SimulationSettings.SPEED_UNIT)
						.withModificationCheck(true).withCollisionAvoidance())
				.build();

		// The seed is strong
		long randomSeed = System.currentTimeMillis();
		List<Scenario> scenarios = new ArrayList<>();
		int numberOfDesiredScenarios = 1;
		for (int i = 0; i < numberOfDesiredScenarios; i++) {
			scenarios.add(generator.generate(new MersenneTwister(randomSeed), "Scenario " + Integer.toString(i + 1)));
		}

		final Optional<ExperimentResults> results = Experiment.builder()
				// Adds a configuration to the experiment. A configuration configures an
				// algorithm that is supposed to
				// handle or 'solve' a problem specified by a scenario. A configuration can
				// handle a scenario if it
				// contains an event handler for all events that occur in the scenario. The
				// scenario in this example
				// contains four different events and registers an event handler for each of
				// them.
				.addConfiguration(MASConfiguration.builder()
						// NOTE: this example uses 'namedHandler's for Depots and Parcels, while very
						// useful for
						// debugging these should not be used in production as these are not thread
						// safe.
						// Use the 'defaultHandler()' instead.
						.addEventHandler(AddDepotEvent.class,
								ModelPopulationEventHandlers.defaultHandler(dynamicGraph, s, p))

						// There is no default handle for vehicle events, here a non functioning handler
						// is added,
						// it can be changed to add a custom vehicle to the simulator.
						.addEventHandler(AddVehicleEvent.class, AddAGVEventHandlers.defaultHandler(dynamicGraph, s))
                        .addEventHandler(AddParcelEvent.class, ParcelEventHandlers.defaultHandler())
						.addEventHandler(TimeOutEvent.class, TimeOutStopper.stopHandler())
						// Note: if your multi-agent system requires the aid of a model (e.g. CommModel)
						// it can be added
						// directly in the configuration. Models that are only used for the solution
						// side should not
						// be added in the scenario as they are not part of the problem.
						.addModel(DARModel.builder(SimulationSettings.TICK_LENGTH, p.verbose, p))
						.addModel(StatsTracker.builder())
						.addModel(TimeModel.builder().withTickLength(SimulationSettings.TICK_LENGTH))
						.addModel(RandomModel.builder().withSeed(System.currentTimeMillis()))
						.addModel(DefaultPDPModel.builder())
						.addModel(CommModel.builder())
						.build())

				// Adds the newly constructed scenario to the experiment.
				// Every configuration will be run on every scenario.
				.addScenarios(scenarios)

				// The number of repetitions for each simulation.
				// Each repetition will have a unique random seed that is given to the
				// simulator.
				.repeat(p.repeat)

				// The master random seed from which all random seeds for the simulations will
				// be drawn.
				.withRandomSeed(randomSeed)

				// The number of threads the experiment will use, this allows to run several
				// simulations in parallel.
				// Note that when the GUI is used the number of threads must be set to 1.
				.withThreads(p.threads)

				// We add a post processor to the experiment. A post processor can read the
				// state of the simulator
				// after it has finished. It can be used to gather simulation results. The
				// objects created by the
				// post processor end up in the ExperimentResults object that is returned by the
				// perform(..) method
				.usePostProcessor(new DARPostProcessor(this.id))
				//GUI options
				.showGui(viewBuilder).showGui(p.showGUI)

				// Starts the experiment, but first reads the command-line arguments that are
				// specified for this
				// application. By supplying the '-h' option you can see an overview of the
				// supported options.
				.perform(System.out);

		if (results.isPresent()) {
			StatsOutput.writeToJson(p.id + this.id, this.p, results.get().getResults());
		} else {
			throw new IllegalStateException("Experiment did not complete.");
		}
	}
	
	 private static Vehicles.VehicleGenerator getVehicleGenerator(
	            int vehiclesAm, double vehicleSpeed
	    ) {
	        return Vehicles.builder()
	                .numberOfVehicles(StochasticSuppliers.constant(vehiclesAm))
	                .capacities(StochasticSuppliers.constant(1))
	                .speeds(StochasticSuppliers.constant(vehicleSpeed))
	                .startPositions(StochasticSuppliers.constant(new Point(2, 2)))
	                .build();
	    }

	    private static Depots.DepotGenerator getDepotGenerator() {
	        return Depots.builder()
	                .numerOfDepots(StochasticSuppliers.constant(1))
	                .build();
	    }

	    private static Parcels.ParcelGenerator getDeliveryTaskGenerator() {
	        return new DeliveryTaskGenerators();
	    }
}