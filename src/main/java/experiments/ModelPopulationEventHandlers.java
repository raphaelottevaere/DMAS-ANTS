package experiments;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;

import deadlocks.DeadlockSolver;
import graphs.ModelPopulator;
import models.DARModel;
import tasks.TaskHub;

public class ModelPopulationEventHandlers {

    private static ListenableGraph<LengthData> graph;
	private static String graphName;
	private static ExperimentParameters p;

    public static TimedEventHandler<AddDepotEvent> defaultHandler(
            ListenableGraph<LengthData> graph,
            String graphName, ExperimentParameters p
    ) {
        ModelPopulationEventHandlers.graph = graph;
        ModelPopulationEventHandlers.graphName = graphName;
        ModelPopulationEventHandlers.p = p;
        
        return Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddDepotEvent> {
        INSTANCE {
            public void handleTimedEvent(@NotNull AddDepotEvent event, @NotNull SimulatorAPI sim) {
                DARModel dar = ((Simulator) sim).getModelProvider().getModel(DARModel.class);
                DeadlockSolver dl = new DeadlockSolver(dar, graph);
                ((Simulator) sim).addTickListener(dl);
                dar.setDeadLockSolver(dl);
                
        		ModelPopulator model;
				try {
					model = new ModelPopulator(graphName);
					model.addAllStaticAgents(dar, graph.getNodes(), sim.getRandomGenerator(), graph, dl);
	        		model.populateStorage(dar, sim.getRandomGenerator());
				} catch (IOException e) {
						e.printStackTrace();		
				}
				
				((Simulator) sim).addTickListener(new TaskHub(sim.getRandomGenerator(), dar, p.minTask, p.maxTasks, p.probNewDeliveryTask));
				
            }
            
            

            public String toString() {
                return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };

    }
}

