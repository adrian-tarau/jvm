package net.microfalx.jvm.model;

import lombok.Data;
import net.microfalx.lang.Identifiable;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ThreadInformation implements Identifiable<Long>, Serializable {

    @Serial
    private static final long serialVersionUID = 4174499425268985048L;

    private long id;
    private int daemon;
    private int nonDaemon;

    private long blockedTime;
    private long blockedCount;
    private long waitedTime;
    private long waitedCount;

    private int[] activeEvents;
    private long[] eventCounts;
    private long[] eventTimes;

    private int inNativeCount;
    private int suspendedCount;

    private int[] stateCount;
    private Thread.State state;

    public Long getId() {
        return id;
    }
}
