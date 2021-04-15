package experiments;

import java.io.IOException;

import simulator.SimulationSettings;

public class Experiments {

	public static void main(String[] args) throws IOException {
		test();
	}
	

	private static void exectute(ExperimentParameters ep, int i) throws IOException {
			new standardExperiment(i, ep).run();
	}

	//Maybe also do experiments with decreased vehicle speed?
	private static void test() throws IOException {
		// Changed heuristics need to check with standard experiment
		/*
		ExperimentParameters check = new ExperimentParameters("simple-harbour.dot", "check");
		check.repeat = 5;
		check.threads = 5;
		// check.showGUI=true;
		exectute(check, 0);
		//exectute(check, 1);
		*/

		// Standard experiments
		
		 //ExperimentParameters exp1 = new ExperimentParameters("simple-harbour.dot",	 "1-CONS"); 
		 //exp1.cons1 = true; 
		 //exectute(exp1, 0);
		 //exectute(exp1, 1);
		 //exectute(exp1, 2);
		 //exectute(exp1, 3);
		 
		 
		 //ExperimentParameters exp2 = new ExperimentParameters("simple-harbour.dot", "1-MAX-FAST"); 
		//exp2.maxFast = 1; 
		 //exectute(exp2, 0);
		 //exectute(exp2, 1);
		 //exectute(exp2, 2);
		 //exectute(exp2, 3);
		 
		
		 //ExperimentParameters exp3 = new ExperimentParameters("simple-harbour.dot","2-MAX-FAST");
		 //exp3.maxFast = 2; 
		 //exectute(exp3, 0);
		 //exectute(exp3, 1);
		// exectute(exp3, 2);
		 //exectute(exp3, 3);
		  
		
		 //ExperimentParameters exp4 = new ExperimentParameters("simple-harbour.dot","3-MAX-FAST"); 
		 //exp4.maxFast = 3; 
		 //exectute(exp4, 0);
		 //exectute(exp4, 1);
		 //exectute(exp4, 2);
		 //exectute(exp4, 3);
		
		
		 // Standard with nonFastHoldIA 
		 //ExperimentParameters exp5 = new ExperimentParameters("simple-harbour.dot", "1-MAX-FAST-NOHOLDIA");
		 //exp5.active_holdIA = false; 
		 //exp5.maxFast = 1; 
		 //exectute(exp5, 0);
		 //exectute(exp5, 1);
		 //exectute(exp5, 2);
		 //exectute(exp5, 3);
		 
		
		 // Same as standard experiments with much larger ttl -> 10 mins instead of 1
		 //ExperimentParameters exp6 = new ExperimentParameters("simple-harbour.dot","1-CONS-LARGE-TTL");
		 //exp6.cons1 = true;
		 //exp6.ttl = 10 * 30 * 1000;
		 //exectute(exp6, 0);
		 //exectute(exp6, 1);
		 //exectute(exp6, 2);
		 //exectute(exp6, 3);
		 
		
		 //ExperimentParameters exp7 = new ExperimentParameters("simple-harbour.dot","1-MAX-FAST-LARGE-TTL"); 
		 //exp7.maxFast = 2; 
		 //exp7.ttl = 10 * 30 * 1000; 
		 //exectute(exp7, 0);
		 //exectute(exp7, 1);
		 //exectute(exp7, 2);
		 //exectute(exp7, 3);
		 
		 
		
		 //ExperimentParameters exp8 = new ExperimentParameters("simple-harbour.dot", "2-MAX-FAST-LARGE-TTL"); 
		 //exp8.ttl = 10 * 30 * 1000; 
		 //exp8.maxFast = 2;
		 //exectute(exp8, 0);
		 //exectute(exp8, 1);
		 //exectute(exp8, 2);
		 //exectute(exp8, 3);
		
		
		 //ExperimentParameters exp9 = new ExperimentParameters("simple-harbour.dot","3-MAX-FAST-LARGE-TTL"); 
		 //exp9.ttl = 10 * 30 * 1000; 
		 //exp9.maxFast = 3;
		 //exectute(exp9, 0);
		 //exectute(exp9, 1);
		 //exectute(exp9, 2);
		 //exectute(exp9, 3);		 
		 
		
		 // different heuristics we dont take into account if other AGVs already
		 //registered for pickups ExperimentParameters exp11 = new
		 //ExperimentParameters exp10 = new ExperimentParameters("simple-harbour.dot", "1-MAX-FAST+PICKUP-HEUR-FACTOR");
		 //exp10.pickupsizefactor = 1;
		 //exp10.taskCons = 1; 
		 //exectute(exp10, 0);
		 //exectute(exp10, 1);
		 //exectute(exp10, 2);
		 //exectute(exp10, 3);
		 
		 
		// We dont as quickly raise the heuristics for tasks near the end time
		//ExperimentParameters exp11 = new ExperimentParameters("simple-harbour.dot",	"1-MAX-FAST-LINEAR-TIME-HEUR-SCALING");
		//exp11.h1 = 1;
		//exp11.m30 = 2;
		//exp11.m15 = 3; 
		//exp11.ur = 4; 
		// exectute(exp11, 0);
		// exectute(exp11, 1);
		 //exectute(exp11, 2);
		 //exectute(exp11, 3);

		 
		 //ExperimentParameters exp12 = new ExperimentParameters("simple-harbour.dot","inf-MAX-FAST"); 
		 //exp12.maxFast = 12; 
		 //exectute(exp12, 0);
		 //exectute(exp12, 1);
		 //exectute(exp12, 2);
		 //exectute(exp12, 3);
		 
		// Maybe also do experiments with increased speed?
		// Standard experiments with reduced AGV Speed
		
		 //ExperimentParameters exp1 = new ExperimentParameters("simple-harbour.dot",	 "1-CONS-REDUCED-SPEED"); 
		 //exp1.cons1 = true; 
		 //exectute(exp1, 0);
		 //exectute(exp1, 1);
		// exectute(exp1, 2);
		 //exectute(exp1, 3);
		 
		 
		
		
		 //ExperimentParameters exp2 = new ExperimentParameters("simple-harbour.dot", "1-MAX-FAST-REDUCED-SPEED"); 
		 //exectute(exp2, 0);
		 //exectute(exp2, 1);
		 //exectute(exp2, 2);
		// exectute(exp2, 3);
		
		
		
		 //ExperimentParameters exp3 = new ExperimentParameters("simple-harbour.dot","2-MAX-FAST-REDUCED-SPEED");
		 //exp3.maxFast = 2; 
		 //exectute(exp3, 0);
		 //exectute(exp3, 1);
		 //exectute(exp3, 2);
		 //exectute(exp3, 3);
		
		  
		
		 //ExperimentParameters exp4 = new ExperimentParameters("simple-harbour.dot","3-MAX-FAST-REDUCED-SPEED"); 
		 //exp4.maxFast = 3; 
		 //exectute(exp4, 0);
		 //exectute(exp4, 1);
		 //exectute(exp4, 2);
		 //exectute(exp4, 3);
		 
		 //ExperimentParameters exp12 = new ExperimentParameters("simple-harbour.dot","inf-MAX-FAST-REDUCED-SPEED"); 
		 //exp12.maxFast = 12; 
		 //exectute(exp12, 0);
		 //exectute(exp12, 1);
		 //exectute(exp12, 2);
		 //exectute(exp12, 3);
		 
		 // TODO CHANGE AGV SPEED BACK
		 // Maybe do experiment with more paths that are facotered in 
		
		 
		 //ExperimentParameters exp9 = new ExperimentParameters("simple-harbour.dot","inf-MAX-FAST-LARGE-TTL"); 
		 //exp9.ttl = 10 * 30 * 1000; 
		 //exp9.maxFast = 12;
		 //exectute(exp9, 0);
		 //exectute(exp9, 1);
		 //exectute(exp9, 2);
		 //exectute(exp9, 3);
		
		 // different heuristics we dont take into account if other AGVs already
		 //registered for pickups
		 //ExperimentParameters exp10 = new ExperimentParameters("simple-harbour.dot", "1-MAX-FAST+0-HEUR-FACTOR");
	     //exp10.pickupsizefactor = 0;
		 //exp10.taskCons = 0; 
		 //exectute(exp10, 0);
		// exectute(exp10, 1);
		 //exectute(exp10, 2);
		 //exectute(exp10, 3);

		 
			// We dont as quickly raise the heuristics for tasks near the end time
			//ExperimentParameters exp11 = new ExperimentParameters("simple-harbour.dot",	"1-MAX-FAST-EXPON-TIME-HEUR-SCALING");
			//exp11.h1 = 1;
			//exp11.m30 = 4;
			//exp11.m15 = 16; 
			//exp11.ur = 64;
			//exectute(exp11, 0);
			 //exectute(exp11, 1);
			// exectute(exp11, 2);
			 //exectute(exp11, 3);
			 
			 // Standard with nonFastHoldIA 
		//TODO CHANGE BACK VALUE IN SIMULATIONSETTINGS
			// ExperimentParameters exp5 = new ExperimentParameters("simple-harbour.dot", "2-max-fast-more-ants-decision");
			 //exp5.maxFast = 2; 
			 //exp5.alternativePathsToExplore=6;
			 //exectute(exp5, 0);
			 //exectute(exp5, 1);
			 //exectute(exp5, 2);
			 //exectute(exp5, 3);
			 
			 // Standard with nonFastHoldIA 
		//TODO CHANGE BACK VALUE IN SIMULATIONSETTINGS
			 //ExperimentParameters exp20 = new ExperimentParameters("simple-harbour.dot", "1-max-fast-more-ants-decision");
			 //exp20.maxFast = 1; 
			 //exp20.alternativePathsToExplore=6;
			 //exectute(exp20, 0);
			 //exectute(exp20, 1);
			 //exectute(exp20, 2);
			 //exectute(exp20, 3);
			 
			 //TODO CHANGE RANGE FACTOR AND CHANGE BACK AFTER!!
			 ExperimentParameters exp21 = new ExperimentParameters("simple-harbour.dot", "1-max-fast-no-range-bound");
			 exp21.maxFast = 1; 
			 //exectute(exp21, 0);
			 //exectute(exp21, 1);
			 //exectute(exp21, 2);
			 exectute(exp21, 3);
			 
	}
}
