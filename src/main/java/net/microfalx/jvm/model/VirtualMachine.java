package net.microfalx.jvm.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Data
public class VirtualMachine implements Serializable {

    @Serial
    private static final long serialVersionUID = 2876115727942855023L;

    private boolean local;

    private long pid;

    private long heapTotalMemory;
    private long heapUsedMemory;

    private long nonHeapTotalMemory;
    private long nonHeapUsedMemory;

    private Collection<MemoryPool> memoryPools = Collections.emptyList();
    private Collection<BufferPool> bufferPools = Collections.emptyList();
    private Collection<GarbageCollection> garbageCollections = Collections.emptyList();
    private RuntimeInformation runtimeInformation;
    private ThreadInformation threadInformation;
    private Process process;
    private Server server;

    private int gcRequests;
    private int gcSkipped;
    private int gcExecuted;

    private ThreadDump threadDump;

    public float getHeapUsedMemoryPercent() {
        return heapTotalMemory == 0 ? 0 : 100 * ((float) heapUsedMemory / (float) heapTotalMemory);
    }

    public MemoryPool getMemoryPool(MemoryPool.Type type) {
        for (MemoryPool memoryPool : memoryPools) {
            if (memoryPool.getType() == type) return memoryPool;
        }
        throw new IllegalArgumentException("Unknown memory pool type: " + type);
    }

    public float getNonHeapUsedMemoryPercent() {
        return nonHeapTotalMemory == 0 ? 0 : 100 * ((float) nonHeapUsedMemory / (float) nonHeapTotalMemory);
    }


    public MemoryPool getEdenMemoryPool() {
        return getMemoryPool(MemoryPool.Type.EDEN);
    }

    public MemoryPool getTenuredMemoryPool() {
        return getMemoryPool(MemoryPool.Type.TENURED);
    }

    public MemoryPool getSurvivorMemoryPool() {
        return getMemoryPool(MemoryPool.Type.SURVIVOR);
    }

    public void setMemoryPools(Collection<MemoryPool> memoryPools) {
        this.memoryPools = memoryPools;
        heapTotalMemory = 0;
        heapUsedMemory = 0;
        nonHeapTotalMemory = 0;
        nonHeapUsedMemory = 0;
        for (MemoryPool memoryPool : memoryPools) {
            if (memoryPool.getType().isHeap()) {
                heapTotalMemory += memoryPool.getMaximum();
                heapUsedMemory += memoryPool.getUsed();
            } else {
                nonHeapTotalMemory += memoryPool.getMaximum();
                nonHeapUsedMemory += memoryPool.getUsed();
            }
        }
    }

    public static MemoryPool getAverageMemoryStats(MemoryPool.Type type, Collection<VirtualMachine> virtualMachines) {
        Collection<MemoryPool> memoryStats = getMemoryStats(type, virtualMachines);
        int count = memoryStats.size();
        if (count == 0) return new MemoryPool(type, 0, 0, 0, 0);
        long maximum = Long.MIN_VALUE;
        long capacity = 0;
        long used = 0;
        long committed = 0;
        for (MemoryPool memoryStat : memoryStats) {
            maximum = Math.max(memoryStat.getMaximum(), maximum);
            capacity += memoryStat.getCapacity();
            used += memoryStat.getUsed();
            committed += memoryStat.getCommitted();
        }
        return new MemoryPool(type, maximum, capacity / count, used / count, committed / count);
    }

    public static MemoryPool getPeakMemoryStats(MemoryPool.Type type, Collection<VirtualMachine> virtualMachines) {
        Collection<MemoryPool> memoryStats = getMemoryStats(type, virtualMachines);
        int count = memoryStats.size();
        if (count == 0) return new MemoryPool(type, 0, 0, 0, 0);
        long maximum = Long.MIN_VALUE;
        long capacity = Long.MIN_VALUE;
        long used = Long.MIN_VALUE;
        long committed = Long.MIN_VALUE;
        for (MemoryPool memoryStat : memoryStats) {
            maximum = Math.max(memoryStat.getMaximum(), maximum);
            capacity = Math.max(memoryStat.getCapacity(), capacity);
            used = Math.max(memoryStat.getUsed(), used);
            committed = Math.max(memoryStat.getCommitted(), committed);
        }
        return new MemoryPool(type, maximum, capacity, used, committed);
    }

    private static Collection<MemoryPool> getMemoryStats(MemoryPool.Type type, Collection<VirtualMachine> virtualMachines) {
        Collection<MemoryPool> memoryPools = new ArrayList<>();
        for (VirtualMachine virtualMachine : virtualMachines) {
            memoryPools.add(virtualMachine.getMemoryPool(type));
        }
        return memoryPools;
    }

}
