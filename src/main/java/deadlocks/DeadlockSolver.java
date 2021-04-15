package deadlocks;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;

import agents.AGVAgent;
import agents.InfrastructureAgent;
import ants.DeadLockDetectionAnt;
import models.DARModel;
import simulator.SimulationSettings;

public class DeadlockSolver implements TickListener {

	private List<DeadLocks> lockedGraphList = new ArrayList<DeadLocks>();
	private ListenableGraph<LengthData> graph;
	private DARModel darModel;

	public DeadlockSolver(DARModel dar, ListenableGraph<LengthData> gph) {
		this.darModel = dar;
		this.graph= gph;
	}

	@Override
	public void tick(TimeLapse timeLapse) {

		// System.out.println(timeLapse.getTime()/1000 +" " +
		// System.currentTimeMillis());

	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		ArrayList<DeadLocks> toRemove = new ArrayList<>();

		try {
			for (DeadLocks d : lockedGraphList) {
				if (d.timeLastTry + SimulationSettings.RETRY_DEADLOCKS_SOLVER <= timeLapse.getTime()) {
					if (d.checkIfDeadlockStillActive()) {
						d.solveDeadlock(timeLapse);
					} else {
						d.freeAgents();
						darModel.removeDeadLock(timeLapse, d);
						toRemove.add(d);
					}
				} else if (!d.toSendGraph.isEmpty()) {
					d.sendGraphs(timeLapse);
				}
			}
		} catch (ConcurrentModificationException e) {
			e.printStackTrace();
		}

		if (!toRemove.isEmpty())
			lockedGraphList.removeAll(toRemove);

	}

	public void setGraph(ListenableGraph<LengthData> staticGraph) {
		this.graph = staticGraph;
	}

	public void addDeadlock(DeadLockDetectionAnt ant, TimeLapse time) {
		for (DeadLocks d : lockedGraphList) {
			if (d.containsDeadlock(ant)) {
				d.expandDeadLock(ant);
				return;
			}
		}

		DeadLocks d;
		if (ant.getNonMover() != null) {
			d = new DeadLock_NonMover(ant, graph, time);
			darModel.addDeadLock(time, d);
			lockedGraphList.add(d);
		} else {
			d = new DeadLock_Circle(ant, graph, time);
			darModel.addDeadLock(time, d);
			lockedGraphList.add(d);
		}
	}

	public void hasFinishedForcedMove(AGVAgent agvAgent, InfrastructureAgent nodeOn) {
		for (DeadLocks d : lockedGraphList) {
			if (d.containsAGV(agvAgent)) {
				d.removeAgent(agvAgent);
				if (!d.checkIfDeadlockStillActive()) {
					d.freeAgents();
				}
				break;
			}
		}
	}

	public void askingForMove(AGVAgent agvAgent, TimeLapse time) {
		for (DeadLocks d : lockedGraphList) {
			if (d.containsAGV(agvAgent)) {
				d.sendGraph(agvAgent, time);
				break;
			}
		}
	}

	public void addDARModel(DARModel dar) {
		this.darModel = dar;
	}

}
