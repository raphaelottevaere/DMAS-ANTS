package simulator;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.pdptw.common.StatsPanel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.View.Builder;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;

import agents.DecisionAGV;
import agents.RestingAgent;
import agents.ChargingStation;
import deadlocks.DeadlockSolver;
import experiments.ExperimentParameters;
import graphs.GraphCreator;
import graphs.ModelPopulator;
import models.DARModel;
import renderers.AGVRenderer;
import renderers.TaskRenderer;
import stats.StatsTracker;
import tasks.TaskHub;

public class DARSimulator {

	public static void main(String[] args) throws IOException {
		System.out.println("DMAS Simulation start");

		run();
	}

	private static void run() throws IOException {
		String s = "simple-harbour.dot";
		// long seed = System.currentTimeMillis();

		ArrayList<String> graphs = new ArrayList<String>();
		graphs.add(s);

		// graph for the AGV (static)
		ListenableGraph<LengthData> staticGraph = new ListenableGraph<LengthData>(
				GraphCreator.loadGraph("harbour", graphs));
		// graph for dynamic use (has been saved in GraphCreator, so no double
		// generation
		ListenableGraph<LengthData> dynamicGraph = new ListenableGraph<LengthData>(
				GraphCreator.loadGraph("harbour", graphs));

		// Build simulator
		final Simulator.Builder simBuilder = Simulator.builder()
				.addModel(RandomModel.builder().withSeed(System.currentTimeMillis()))
				.addModel(DefaultPDPModel.builder()).addModel(CommModel.builder())
				.addModel(RoadModelBuilders.dynamicGraph((dynamicGraph))
						.withDistanceUnit(SimulationSettings.DISTANCE_UNIT).withSpeedUnit(SimulationSettings.SPEED_UNIT)
						.withModificationCheck(true).withCollisionAvoidance())
				.addModel(DARModel.builder(SimulationSettings.TICK_LENGTH, SimulationSettings.INFRASTRUCTURE_VERBOSE,
						new ExperimentParameters(s, "standard", 0)))
				.addModel(StatsTracker.builder());

		if (SimulationSettings.REALTIME) {
			simBuilder.addModel(TimeModel.builder().withTickLength(SimulationSettings.TICK_LENGTH).withRealTime());
		} else {
			simBuilder.addModel(TimeModel.builder().withTickLength(SimulationSettings.TICK_LENGTH));
		}

		if (SimulationSettings.GUI) {
			// Building ViewBuilder to show custom View
			Builder viewBuilder;

			

			if (SimulationSettings.withStatsPanel) {
				viewBuilder = View.builder().withTitleAppendix("DMAS Simulator")
						.with(GraphRoadModelRenderer.builder().withNodeCircles()// .withDirectionArrows()//.withNodeCoordinates()
								.withMargin(2 * SimulationSettings.AGV_Length))
						.with(TaskRenderer.builder())
						// Add custom roadusers here
						.with(RoadUserRenderer.builder().withImageAssociation(ChargingStation.class, "/chargingStation-or.png").withCircleAroundObjects()
								.withImageAssociation(RestingAgent.class, "/rp.png")
								.withImageAssociation(DecisionAGV.class, "/agv.png").withCircleAroundObjects())
						.with(AGVRenderer.builder()).withResolution(SimulationSettings.WIDTH, SimulationSettings.HEIGHT)
						.withAutoPlay().withSpeedUp(SimulationSettings.SimulationSpeed).with(StatsPanel.builder());
			}else {
				viewBuilder = View.builder().withTitleAppendix("DMAS Simulator")
						.with(GraphRoadModelRenderer.builder().withNodeCircles()// .withDirectionArrows()//.withNodeCoordinates()
								.withMargin(2 * SimulationSettings.AGV_Length))
						.with(TaskRenderer.builder())
						// Add custom roadusers here
						.with(RoadUserRenderer.builder()
								.withImageAssociation(ChargingStation.class, "/chargingStation-or.png").withCircleAroundObjects()
								.withImageAssociation(RestingAgent.class, "/rp.png")
								.withImageAssociation(DecisionAGV.class, "/agv.png").withCircleAroundObjects())
						.with(AGVRenderer.builder()).withResolution(SimulationSettings.WIDTH, SimulationSettings.HEIGHT)
						.withAutoPlay().withSpeedUp(SimulationSettings.SimulationSpeed);
			}
			
			if (SimulationSettings.ASYNCVIEW)
				viewBuilder.withAsync();
			simBuilder.addModel(viewBuilder);
		}

		final Simulator sim = simBuilder.build();

		final RandomGenerator rng = sim.getRandomGenerator();

		final DARModel darModel = sim.getModelProvider().getModel(DARModel.class);

		// Create Agents
		ModelPopulator model = new ModelPopulator(s);
		DeadlockSolver dl = new DeadlockSolver(darModel, staticGraph);
		darModel.setDeadLockSolver(dl);
		sim.addTickListener(dl);
		model.addAllStaticAgents(darModel, staticGraph.getNodes(), rng, staticGraph, dl);
		model.addAllAGV(darModel, staticGraph.getNodes(), rng, staticGraph);
		model.populateStorage(darModel, rng);

		sim.addTickListener(new TaskHub(rng, darModel, SimulationSettings.MinSingleTasks, SimulationSettings.MaxTasks,
				SimulationSettings.NEW_TASK_PROB));

		System.out.println("Starting Simulation");
		sim.start();
	}

}