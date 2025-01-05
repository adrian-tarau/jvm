package net.microfalx.jvm.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class Process implements Serializable {

    @Serial
    private static final long serialVersionUID = -2581339792400294491L;

    private int pid;
    private float cpuTotal;
    private float cpuSystem;
    private float cpuUser;
    private float cpuIoWait;
    private double cpuTotalTime;
    private double cpuUserTime;
    private double cpuSystemTime;
    private double cpuIoWaitTime;
    private long memoryVirtual;
    private long memoryResident;
    private long memoryShared;
    private int threads;
    private int fileDescriptors;
    private long minorFaults;
    private long majorFaults;
    private long pageFaults;
    private String state;
    private long uptime;
    private float score;
    private String report;

}
