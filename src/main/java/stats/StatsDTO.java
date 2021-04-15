package stats;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;

public class StatsDTO extends StatisticsDTO {

	private static final long serialVersionUID = 1L;
	public final long total_Idle_Time;
	public final long total_Charging_Time;
	public final int total_SingleTask_during_Simulation;
	public final int total_Temp_Moves_made;
	public final int stuck_Agv;
	public final int to_late_delivery;
	public final int total_Deadlocks_created;
	public final int dropped_Cars;
	public final int total_SingleTask_CAR_Moves;
	public final long total_DeadLock_Time;
	public final int better_Task;
	public final int better_Path;
	public final int total_Temp_Moves_Pickups;
	public final int total_Charging;
	public final int total_made_tasks;

	public StatsDTO(int singleTask, //
			int createdPackages, int pickedUpPackage, int totalTempPickups, double totalTravelTime, long totalIdleTime,
			long computationTime, long simulationTime, boolean simFinish, int totalvehicles, long totalChargingTime,
			long deliveryTardiness, Unit<Duration> timeUnit, Unit<Length> distanceUnit, Unit<Velocity> speedUnit,
			int totalTempMoves, int stuckAgv, int totalDeadLocks, long deadLockSolveTime, int droppedCars,
			int totalPickups, int totalMoves, int betterTask, int betterPath, int totalCharging, int toLateDelivered,
			int totalMadeTasks) {

		super(0, totalTravelTime, pickedUpPackage, totalMoves, createdPackages, pickedUpPackage, 0l, deliveryTardiness,
				computationTime, simulationTime, simFinish, 0, 0l, totalvehicles, 0, timeUnit, distanceUnit, speedUnit);

		this.total_Idle_Time = totalIdleTime;
		this.total_Charging_Time = totalChargingTime;
		this.total_SingleTask_during_Simulation = singleTask;
		this.total_Temp_Moves_made = totalTempMoves;
		this.stuck_Agv = stuckAgv;
		this.total_Deadlocks_created = totalDeadLocks;
		this.dropped_Cars = droppedCars;
		this.total_SingleTask_CAR_Moves = totalMoves;
		this.total_DeadLock_Time = deadLockSolveTime;
		this.better_Task = betterTask;
		this.better_Path = betterPath;
		this.total_Temp_Moves_Pickups = totalTempPickups;
		this.total_Charging = totalCharging;
		this.to_late_delivery = toLateDelivered;
		this.total_made_tasks = totalMadeTasks;

	}

	@Override
	public String toString() {
		return "{" + "\"totalTasks\": " + totalParcels + ", \"totalMoves\": " + this.total_SingleTask_CAR_Moves
				+ ", \"totalDeliveries\": " + this.totalDeliveries + ", \"totalSingleTask\": "
				+ this.total_SingleTask_during_Simulation + ", \"totalTempMove\": " + this.total_Temp_Moves_made
				+ ", \"totalstuckAGV\": " + this.stuck_Agv + ", \"totalDeadlocks\": " + this.total_Deadlocks_created
				+ ", \"totaldeadlockSolveTime\": " + this.total_DeadLock_Time + ", \"totalTravelTime\": "
				+ this.totalTravelTime + ", \"totalIdleTime\": " + this.total_Idle_Time + ", \"totalChargingTime\": "
				+ this.total_Charging_Time + ", \"totalCharging\": " + this.total_Charging + ", \"totalPickups\": "
				+ this.totalPickups + ", \"totalDeliveries\": " + this.totalDeliveries + ", \"total_made_tasks\": "
				+ this.total_made_tasks + ", \"totalParcels\": " + this.acceptedParcels + ", \"droppedParcels\": "
				+ this.dropped_Cars + ", \"totalVehicles\": " + this.totalVehicles + ", \"delivery to late\": "
				+ this.to_late_delivery + ", \"deliveryTardiness\": " + this.deliveryTardiness + ", \"timeUnit\": \""
				+ this.timeUnit + ", \"distanceUnit\": \"" + this.distanceUnit + ", \"speedUnit\": \"" + this.speedUnit
				+ ", \"simulationTime\": " + this.simulationTime + ", \"simulationTimeInMin\": "
				+ this.simulationTime / 1000.0 / 60.0 + ", \"BetterTask\": " + this.better_Task + ", \"BetterPath\": "
				+ this.better_Path + "}";
	}

}
