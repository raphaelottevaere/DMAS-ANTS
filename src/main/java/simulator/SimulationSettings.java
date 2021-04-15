package simulator;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

public class SimulationSettings {
	public static boolean withStatsPanel = false;
	
	//Basic simulator settings
	public static final Unit<Length> DISTANCE_UNIT =  SI.METER;
	public static final Unit<Velocity> SPEED_UNIT = SI.METRES_PER_SECOND;
	public static final long TICK_LENGTH = 250L;
	public static final int NODE_DISTANCE = 30;	//20
	
	public static final boolean REALTIME = false;
	public static final double BATTERY_LOAD_PERCENTAGE = 20;
	public static final long SearchTime = 1 * 30 * 1000 / TICK_LENGTH ; //Currently just set to a minute
	
	//Simulation Settings (Working)
	public static final int CHARGING_PLACE_AGV = 1;
	public static final int AGV_AMMOUNT = 20;
	public static final int ALTERNATIVE_PATHS_EXPLORATION =3; //For disciplined flood 
	public static final int EXPLORATION_TASK_REFRESH_TIME = 5;	//Number of nodes moved
	public static final int EXPLORATION_PATH_REFRESH_TIME = 3;	//number of nodes moved
	public static final int INTENTION_REFRESH_TIME = 5*1000/250; //elke 5 seconde
	public static final int NEEDED_ANTS_FOR_DECISIONS=5;
	public static final double INTENTIONS_CHANGE_FACTOR=1.01;
	public static int SIM_SPEED_UP=10;
	public static int INTENTION_RESERVATION_LIFETIME= 4 * INTENTION_REFRESH_TIME;
	public static long TIME_BETWEEN_RESERVATIONS = 10000; // this was added and set for 10 seconds, to reserve space between the AGVS while driving and shrinking the problem of AGVS delyaing exachother to infinity
	public static final int MAX_PATH_FAILS = 3;
	public static final double PENALTY_PATH_DISCOVERY = 5;
	public static final int WAITING_ON_DEPENDANT_ANTS = 10 ; //ammount of ticks to wait
	//Time to load = 1 min
	// Experiment Change
	public static final long TIME_TO_LOAD =30*1000; 
	
	//Simulation Settings (GUI)
	public static final int WIDTH = 1920;
	public static final int HEIGHT = 1080;
	public static final boolean INFRASTRUCTURE_VERBOSE = false;
	public static final boolean AGVVERBOSE = false;
	public static final boolean ASYNCVIEW = false;
	public static final boolean GUI = true;

	//AGV Settings
	// Experiment Change
	public static final double Extra_time_per_HOP = 4; //2 for fast, 4 for normal, 6 for slow
	public static final double AGV_SPEED = 20;  //40 for fast, 20 for normal, 10 for slow
	public static final int AGVMaxCharge = 20 * 1000 * 10; //is prob realistic, the last times ten is for the scaling of the simulation
	public static final double Battery_Percentage_Needed_To_Leave_Charging = 0.90;
	public static final int AGV_Length = 10;
	public static final long TASK_DISTANCE_TO_CONSIDER_START = 20; //20 
	public static final int TASK_CONSIDERING_FACTOR = 2;
	public static final int TASK_AMMOUNT_TO_CONSIDER = 5;
	//HOW much extra battery needs to be reserved for a path to be valid (in case of 1.2 -> atleast 20% extra range is needed for it to be valid) to the chargingStation
	public static final double BATTERY_OVERKILL_PERCENTAGE = 1.5;
	//The impact loading has on a battery (measured in lost driving distance) in meters
	public static final double BATTERY_LOAD_IMPACT = 1000;
	public static final int Minimum_Wait_Time = 30;
	public static final int AGVStartCharge = (int) (AGVMaxCharge*BATTERY_LOAD_PERCENTAGE/100);
	
	//TASKS Settings
	// A StoragePlace usually holds max of 12 Cars
	public static final double NEW_TASK_PROB =0.001; //standard set on 0.0015
	public static final int STORAGE_STD = 6; // This means 75% full
	public static final int STORAGE_MEAN = 6;
	
	//CHARGING Setting
	//We assume 20KM range and 4m/s charging time
	// change to realistic
	public static final double CHARGE_AMMOUNT = 5 ;
	// the amount of time needed between 2 chargingReservations in between a AGV can add another chargingReservation
	 // If this is equal to 1 as factor the AGV must abide by the already made ChargingReservations and cannot insert himself inbetween
	public static final long CHARGINGTIME_THRESHHOLD = (long) (AGVMaxCharge/CHARGE_AMMOUNT * 0.75);  
	public static final long MAX_CHARGETIME = (long) (AGVMaxCharge/CHARGE_AMMOUNT);
	public static final long RETRY_DEADLOCKS_SOLVER = 10*1000;
	public static final long RestWait = 30 * 1000;
	public static final boolean DeadlockPreventionOn = true;
	public static final int SEARCH_HORIZON = 20;
	public static final boolean ONLY_1_TASKCONSUMER = false;
	public static final boolean HOLD_IA_ACTIVE = true;
	public static final boolean IMPORT_1_COMSUMER = false;
	public static final int MAX_FAST_COALITION =1;
	public static final int MinSingleTasks = 15; //15
	public static final int MaxTasks =20; //20
	
	//HEUR
	public static final int HEUR_1_TASK_AMMOUNT = 2;
	public static final int HEUR_TASKCONS_SIZE_FACTOR = 2;
	public static final int HEUR_PICKUPTASK_SIZE_FACTOR = 4;
	public static final int HEUR_HOUR_REMAINING_TIME = 2;
	public static final int HEUR_30MIN_REMAINING_TIME = 4;
	public static final int HEUR_15MIN_REMAINING_TIME = 8;
	public static final int HEUR_URGENT_TIME = 16;
	public static int HEUR_PREFER_BATCH_JOBS = 1;
	
	//Experiment + scenario settings
	public static final long SIMULATION_LENGTH = 1000*60*60*8;
	public static final int THREADS = 5;
	public static final int CHARGINGSTATION_RECHARGE=1;
	public static int REPEAT = 5;
	public static final boolean SHOWGUIExperiments = false;
	public static final int NUMBER_SCENARIOS = 1;
	public static final int SimulationSpeed = 10;
}
