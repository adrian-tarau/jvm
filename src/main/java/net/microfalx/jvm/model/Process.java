package net.microfalx.jvm.model;

import lombok.Data;
import net.microfalx.jvm.VirtualMachineUtils;

import java.io.Serializable;

@Data
public class Process implements Serializable {

    private static final long serialVersionUID = -2581339792400294491L;

    private int pid;
    private float cpuTotal = -1;
    private float cpuSystem = -1;
    private float cpuUser = -1;
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

    public float getCpuTotal() {
        return getCpuUser() + getCpuSystem() + getCpuIoWait();
    }

    public long getCpuTotalTime() {
        return getCpuUserTime() + getCpuSystemTime() + getCpuIoWaitTime();
    }

    public float getCpuUser() {
        if (cpuUser >= 0) {
            return cpuUser;
        } else {
            return VirtualMachineUtils.getAverageUsage(uptime, cpuUserTime);
        }
    }

    public float getCpuSystem() {
        if (cpuSystem >= 0) {
            return cpuSystem;
        } else {
            return VirtualMachineUtils.getAverageUsage(uptime, cpuSystemTime);
        }
    }

    public float getCpuIoWait() {
        if (cpuIoWait >= 0) {
            return cpuIoWait;
        } else {
            return VirtualMachineUtils.getAverageUsage(uptime, cpuIoWaitTime);
        }
    }
}
