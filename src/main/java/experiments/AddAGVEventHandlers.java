package experiments;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import graphs.ModelPopulator;
import models.DARModel;

public class AddAGVEventHandlers {
	private static ListenableGraph<LengthData> graph;
	private static String graphName;
	static boolean hasAdded=false;

    public static TimedEventHandler<AddVehicleEvent> defaultHandler(
            ListenableGraph<LengthData> graph,
            String graphName
    ) {
    	AddAGVEventHandlers.graph = graph;
    	AddAGVEventHandlers.graphName = graphName;
        return AddAGVEventHandlers.Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddVehicleEvent> {
        INSTANCE {
            public void handleTimedEvent(@NotNull AddVehicleEvent event, @NotNull SimulatorAPI sim) {       		
            	
				if(hasAdded)
            		return;
            	
        		DARModel dar = ((Simulator) sim).getModelProvider().getModel(DARModel.class);
                
        		if(dar.getDeadLockSolver()==null)
        			throw new IllegalStateException("Can't add AGV without initiating a deadlock solver in the DARModel");
        		
         		ModelPopulator model;
 				try {
 					model = new ModelPopulator(graphName);
 					model.addAllAGV(dar, graph.getNodes(), sim.getRandomGenerator(), graph);
 					hasAdded=true;
 				} catch (IOException e) {
 						e.printStackTrace();		
 				}
             }
            public String toString() {
                return AddVehicleEvent.class.getSimpleName() + ".defaultHandler()";
            }
        }
    }
}
