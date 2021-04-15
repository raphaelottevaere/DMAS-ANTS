package reservations;

import agents.AGVAgent;
import tasks.BasicTask;

public class TaskReservation extends Reservation{
    public BasicTask task;
    public long evaporationTimestamp;

    public TaskReservation(AGVAgent agv, BasicTask task, long evaporationTimestamp) {
		super(agv);
        this.task=task;
        this.evaporationTimestamp = evaporationTimestamp;
    }
    
    public TaskReservation(AGVAgent agv) {
		super(agv);
        this.task=null;
    }

    public TaskReservation copy(long evaporationTimestamp) {
        return new TaskReservation(agv, task, evaporationTimestamp);
    }

    @Override
    public String toString() {
        return "DeliveryTaskReservation{robotID: " + agv.ID + "}";
    }
}
