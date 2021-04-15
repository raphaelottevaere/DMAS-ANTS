package experiments;

import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;

public class ParcelEventHandlers {



	public static TimedEventHandler<AddParcelEvent> defaultHandler() {
		return Handler.INSTANCE;
	}

	enum Handler implements TimedEventHandler<AddParcelEvent> {
		INSTANCE {
			public void handleTimedEvent(@NotNull AddParcelEvent event, @NotNull SimulatorAPI sim) {
		/*		DARModel dar = ((Simulator) sim).getModelProvider().getModel(DARModel.class);
				((Simulator) sim).addTickListener(new TaskHub(sim.getRandomGenerator(), dar));
				DeadlockSolver.getInstance().setGraph(graph);
			((Simulator) sim).addTickListener(DeadlockSolver.getInstance());
			*/}

			public String toString() {
				return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
			}
		}

	}
}