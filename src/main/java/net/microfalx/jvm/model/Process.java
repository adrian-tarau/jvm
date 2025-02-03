package net.microfalx.jvm.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Process implements Serializable {

    private static final long serialVersionUID = -2581339792400294491L;

    private int pid;
    private float cpuTotal;
    private float cpuSystem;
    private float cpuUser;
    private float cpuIoWait;
    private long cpuUserTime;
    private long cpuSystemTime;
    private long cpuIoWaitTime;
    private long memoryVirtual;
    private long memoryResident;
    private long memoryShared;
    private int threads;
    private int fileDescriptors;
    private long minorFaults;
    private long majorFaults;
    private long pageFaults;
    private long bytesRead;
    private long bytesWritten;
    private String state;
    private long startupTime;
    private long uptime;
    long window;

    public float getCpuTotal() {
        return getCpuUser() + getCpuSystem() + getCpuIoWait();
    }

    public long getCpuTotalTime() {
        return getCpuUserTime() + getCpuSystemTime() + getCpuIoWaitTime();
    }

}
