package deadlocks;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import ants.DeadLockDetectionAnt;
import simulator.SimulationSettings;

public class DeadLock_Circle extends DeadLocks {
	protected final Point lastNode;

	public DeadLock_Circle(DeadLockDetectionAnt ant, ListenableGraph<LengthData> graph, TimeLapse time) {
		super(ant, graph, time);
		this.lastNode = ant.getLastNode();

		if(edges.size() == 1) {
			this.active = false;
			return;
		}
		
		System.out.println("---------------DEADLOCK----------------");
		for (DeadLockedEdge e : edges) {
			if (e.getAgvAgent().getPosition().get().equals(e.from)) {
				e.getAgvAgent().setWatitTime(SimulationSettings.RETRY_DEADLOCKS_SOLVER, time);
				System.out.println(e.getAgvAgent());
			}
		}
		System.out.println("---------------END----------------");
		
		this.solveDeadlock(time);
	}

	public boolean checkIfDeadlockStillActive() {
		if(!active)
			return false;
		
		for (DeadLockedEdge e : edges) {
			if (e.from.equals(e.getAgvAgent().getPosition().get())) {
				return true;
			}
		}

		// All agents have been moved from their locations and the Deadlock has been
		// solved
		return false;
	}

	@Override
	protected void solve(TimeLapse time, ListenableGraph<LengthData> dynamicGraph) {
		for (DeadLockedEdge e : edges) {
						System.out.println(e.getAgvAgent());
						e.getAgvAgent().forcedMove = true;
						e.getAgvAgent().searchForRestingPlace(this.nodes, time.getTime()+30*1000, time, this.staticGraph);
						e.getAgvAgent().waiting=false;
		}
	}

	@Override
	protected void freeAgents() {
		for (DeadLockedEdge e : this.edges) {
			e.getAgvAgent().waiting=false;
			resetGraph(e.getAgvAgent());
		}
	}
}
