package tasks;

import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

import models.DARModel;

public class TaskHub implements TickListener {

	private RandomGenerator rng;
	private DARModel dar;
	private int minTask;
	private int maxTask;
	private double newTaskProb;

	public TaskHub(RandomGenerator rng, DARModel dar, int minTasks, int maxtasks, double newTaskProb) {
		this.rng = rng;
		this.dar = dar;
		this.minTask = minTasks;
		this.maxTask = maxtasks;
		this.newTaskProb=newTaskProb;
	}

	// Makes changes in task/shows new tasks
	// Main goal is the deadlines that are already present from 8hours before
	// This means not a lot of tasks need to be added dynamicly during the run
	// This is mainly simulated in the experiments
	// Here we (for testing uses push a lot of tasks to see if the DMAS
	// configuration holds
	// Might be interesting to make this all dependent on settings so that the
	// experiments can also use this!
	public void tick(@NotNull TimeLapse timeLapse) {
		int tasks = dar.taskAmmount();
		int tries = 0;
		if (tasks < minTask) {
			System.out.println("--------ADDING TASK ----------");
			while (tasks < minTask) {
				tries++;
				tasks += dar.createSingleTasks(rng, timeLapse.getTime(), maxTask-tasks);
				if (tries > 10)
					return;
			}
			System.out.println("--------DONE ADDING TASK ----------");
		}
		if (tasks >= maxTask)
			return;

		if (rng.nextDouble() < newTaskProb) {
			System.out.println("--------ADDING TASK ----------");
			int loop = rng.nextInt(4);

			for (int i = 0; i < loop + 1; i++) {
				tries++;
				tasks +=dar.createSingleTasks(rng, timeLapse.getTime(), maxTask-tasks);
				if (tasks >= maxTask)
					return;
				if (tries > 10)
					return;
			}
			System.out.println("--------DONE ADDING TASK ----------");
		}
	}

	public void afterTick(TimeLapse timeLapse) {
		// Nothing needs to be done here
		// Might be interesting to check task related stuff here to see if something
		// went wrong?
	}

}
