package experiments;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;

import stats.StatsTracker;
import tasks.BasicTask;

import com.github.rinde.rinsim.experiment.PostProcessor;

public class DARPostProcessor implements PostProcessor<Pair<String, List<BasicTask>>> {
	 private int id;
	 private int run = 0;

	public DARPostProcessor(int id) {
		this.id = id;
	}

	public Pair<String, List<BasicTask>> collectResults(Simulator sim, SimArgs args) {
        StatsTracker statsTracker = sim.getModelProvider().getModel(StatsTracker.class);

        long timeElapsed = (System.currentTimeMillis() - statsTracker.getStatsListener().startTimeSim) / 1000;
        run++;
        System.out.println("Stopping experiment " + id + ", run " + run + ", " + timeElapsed + "s");

        return Pair.of(statsTracker.getStatistics().toString(),statsTracker.getTasksStatistics());
	}

	public FailureStrategy handleFailure(Exception e, Simulator sim, SimArgs args) {
        System.err.println("EXPERIMENT " + id + ", run " + run + " failed");
		return FailureStrategy.ABORT_EXPERIMENT_RUN;
	}

}
