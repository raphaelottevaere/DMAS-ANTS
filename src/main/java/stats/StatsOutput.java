package stats;

import java.io.BufferedWriter;

import com.github.rinde.rinsim.experiment.Experiment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableSet;

import experiments.ExperimentParameters;
import tasks.BasicTask;
import tasks.HoldingTask;
import tasks.DeliveryTask;

public class StatsOutput {

	@SuppressWarnings("unchecked")
	public static void writeToJson(String id, ExperimentParameters p, ImmutableSet<Experiment.SimulationResult> results) {
		String filename ="experiment-statistics/experiment-" + id
				+ ".json";
		System.out.println("Writing results to file: " + filename);

		try {
			/*
			File file = new File(filename);
			if (!file.exists())
				file.createNewFile();
				*/

			BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename));
			writer.write("{\n" + "    \"experiment\": " + id + ",\n" + "    \"parameters\": " + p.toJson() + ",\n"
					+ "    \"results\": [\n");
			int i = 0;
			final Iterator<Experiment.SimulationResult> it = results.iterator();
			while (it.hasNext()) {
				// The SimulationResult contains all information about a specific simulation,
				// the result object is the object created by the post processor, a String in
				// this case.
				Pair<String, List<BasicTask>> data;
				try {
					data = (Pair<String, List<BasicTask>>) it.next().getResultObject();
				} catch (ClassCastException e) {
					e.printStackTrace();
					return;
				}
				writer.write("        " + data.getLeft());
				writeTasksToFile(id, i, data.getRight());

				if (it.hasNext()) {
					writer.write(",\n");
				} else {
					writer.write("\n");
				}
				i++;
			}

			writer.write("    ]\n");
			writer.write("}");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeTasksToFile(String id, int i, Collection<BasicTask> col) {
		String filename = "task-statistics/experiment-" + id + "-" + i
				+ "tasks.csv";
		System.out.println("Writing results to file: " + filename);

		try {
			/*
			File file = new File(filename);
			if (!file.exists())
				file.createNewFile();
				*/
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename));
			writer.write(
					"EXP-ID,SingleTask,ID,isFinished,startTime,EndTime,pickupTime,completionTime,beginPosition+,endPosition,inOrderFor,agvID,Priority\n");

			for (BasicTask bt : col) {
				if (bt instanceof DeliveryTask) {
					@SuppressWarnings("unused")
					DeliveryTask st = (DeliveryTask) bt;
					writer.write(id + "," + "1" + "," + bt.ID + "," + bt.isFinished() + "," + bt.startTime + ","
							+ bt.getEndTime() + "," + bt.pickupTime + "," + bt.completionTime + "," + bt.beginPosition.x + "-" + bt.beginPosition.y
							+ "," + + bt.endPosition.x+"-"+bt.endPosition.y  + "," + null + "," + bt.agvID + "," + bt.getPriority() + "\n");
				} else {
					HoldingTask st = (HoldingTask) bt;
					writer.write(id + "," + "0" + "," + bt.ID + "," + bt.isFinished() + "," + bt.startTime + ","
							+ bt.getEndTime() + "," + bt.pickupTime + "," + bt.completionTime + "," + bt.beginPosition.x + "-" + bt.beginPosition.y
							+ "," + bt.endPosition.x+"-"+bt.endPosition.y + "," + st.inOrderFor.ID + "," + bt.agvID + "," + bt.getPriority()
							+ "\n");
				}
			}
			
			writer.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
