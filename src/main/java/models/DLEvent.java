package models;

import com.github.rinde.rinsim.event.Event;

import deadlocks.DeadLocks;

public class DLEvent extends Event {

    /**
     * The {@link Deadlock} that dispatched this event.
     */
    public final DeadLocks d;

    /**
     * The time at which the event was dispatched.
     */
    public final long time;

    public DLEvent( DAREventType type, DeadLocks deadlock, long t) {
        super(type);
        d=deadlock;
        time = t;
    }
}
