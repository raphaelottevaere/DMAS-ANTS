package tasks;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;

public class CarPackage extends Parcel {

	public BasicTask task;
	public String carID;
	public int taskId;
	public long endTime;

	public CarPackage(ParcelDTO parcelDto, BasicTask task, long time, String carID) {
		super(parcelDto);
		this.task = task;
		this.taskId=task.ID;
		this.carID=carID;
		this.endTime = task.getEndTime();
	}

	@Override
	public String toString() {
		return "CarPackage( TaskID: " + taskId + ", CarId: " + carID +")";
	}
}
