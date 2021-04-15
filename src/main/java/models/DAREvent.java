package models;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.event.Event;

import tasks.BasicTask;

public class DAREvent extends Event {

    /**
     * The {@link DeliveryTask} that dispatched this event.
     */
    public final BasicTask basicTask;

    /**
     * The time at which the event was dispatched.
     */
    public final long time;

    /**
     * The {@link Parcel} that was involved in the event, or <code>null</code> if
     * there was no {@link Parcel} involved in the event.
     */
    @Nullable
    public final Parcel parcel;

    /**
     * The {@link Vehicle} that was involved in the event, or <code>null</code> if
     * there was no {@link Vehicle} involved in the event.
     */
    @Nullable
    public final Vehicle vehicle;

    public DAREvent( DAREventType type, long t, @Nullable BasicTask task, @Nullable Parcel p, @Nullable Vehicle v) {
        super(type, task);
        basicTask = task;
        time = t;
        parcel = p;
        vehicle = v;
    }
}
