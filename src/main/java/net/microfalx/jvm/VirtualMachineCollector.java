package net.microfalx.jvm;

import net.microfalx.jvm.model.Process;
import net.microfalx.jvm.model.*;
import net.microfalx.lang.ArgumentUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Collects information about a Java VM (process).
 */
public final class VirtualMachineCollector extends AbstractCollector<VirtualMachine> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualMachineCollector.class);

    private static final String OPERATING_SYSTEM_NAME = "java.lang:type=OperatingSystem";

    private final VirtualMachineMBeanServer machineMBeanServer;
    private final SystemInfo systemInfo = new SystemInfo();

    public VirtualMachineCollector(VirtualMachineMBeanServer machineMBeanServer) {
        ArgumentUtils.requireNonNull(machineMBeanServer);
        this.machineMBeanServer = machineMBeanServer;
    }

    public VirtualMachine execute() {
        VirtualMachine vm = new VirtualMachine();
        try (Timer ignored = VirtualMachineUtils.METRICS.startTimer("Collect VM")) {
            vm.setLocal(machineMBeanServer.isLocal());
            collectPid(vm);
            collectProcess(vm);
            collectMemoryStats(vm);
            collectGarbageCollection(vm);
            collectBufferPools(vm);
            collectRuntimeInformation(vm);
            collectThreadInformation(vm);
            if (!isMetadata()) collectThreadDumps(vm);
        }
        return vm;
    }

    public void collectBufferPools(VirtualMachine virtualMachine) {
        Collection<BufferPoolMXBean> bufferPoolMXBeans = machineMBeanServer.getPlatformMXBeans(BufferPoolMXBean.class);
        Collection<BufferPool> bufferPools = new ArrayList<>();
        for (BufferPoolMXBean bufferPoolMXBean : bufferPoolMXBeans) {
            BufferPool.Type type = guessBufferPoolType(bufferPoolMXBean);
            bufferPools.add(new BufferPool(type, (int) bufferPoolMXBean.getCount(), bufferPoolMXBean.getTotalCapacity(), bufferPoolMXBean.getMemoryUsed()));
        }
        virtualMachine.setBufferPools(bufferPools);
    }

    public void collectGarbageCollection(VirtualMachine virtualMachine) {
        Collection<GarbageCollection> stats = new ArrayList<>();
        Collection<GarbageCollectorMXBean> garbageCollectorMXBeans = machineMBeanServer.getPlatformMXBeans(GarbageCollectorMXBean.class);
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            GarbageCollection.Type type = guessGarbageCollectorType(garbageCollectorMXBean);
            stats.add(new GarbageCollection(type, (int) garbageCollectorMXBean.getCollectionCount(), (int) garbageCollectorMXBean.getCollectionTime()));
        }
        virtualMachine.setGarbageCollections(stats);
    }

    private void collectMemoryStats(VirtualMachine virtualMachine) {
        Collection<MemoryPoolMXBean> memoryPoolMXBeans = machineMBeanServer.getPlatformMXBeans(MemoryPoolMXBean.class);
        Collection<MemoryPool> memoryPools = new ArrayList<>();
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            MemoryUsage memoryUsage = memoryPoolMXBean.getUsage();
            MemoryPool.Type memoryType = guessMemoryType(memoryPoolMXBean);

            MemoryPool memoryPool = new MemoryPool(memoryType, memoryUsage.getMax(), memoryUsage.getCommitted(), memoryUsage.getUsed(), memoryUsage.getCommitted());
            memoryPools.add(memoryPool);
        }
        virtualMachine.setMemoryPools(memoryPools);
    }

    private void collectRuntimeInformation(VirtualMachine virtualMachine) {
        OperatingSystemMXBean operatingSystemMXBean = machineMBeanServer.getPlatformMXBean(OperatingSystemMXBean.class);
        RuntimeMXBean runtimeMXBean = machineMBeanServer.getPlatformMXBean(RuntimeMXBean.class);

        RuntimeInformation runtimeInformation = new RuntimeInformation();
        runtimeInformation.setOsName(operatingSystemMXBean.getName());
        runtimeInformation.setOsVersion(operatingSystemMXBean.getVersion());
        runtimeInformation.setStartTime(runtimeMXBean.getStartTime());
        runtimeInformation.setUptime(runtimeMXBean.getUptime());
        virtualMachine.setName(runtimeMXBean.getVmName() + " " + runtimeMXBean.getVmVersion());

        com.sun.management.OperatingSystemMXBean internalOperatingSystemMXBean = machineMBeanServer.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
        Process process = virtualMachine.getProcess();
        process.setCpuTotal((float) internalOperatingSystemMXBean.getProcessCpuLoad() * 100);

        try {
            runtimeInformation.setCommittedVirtualMemorySize(machineMBeanServer.getLongAttr(OPERATING_SYSTEM_NAME, "CommittedVirtualMemorySize", 0L));
            runtimeInformation.setFreePhysicalMemorySize(machineMBeanServer.getLongAttr(OPERATING_SYSTEM_NAME, "FreePhysicalMemorySize", 0L));
            runtimeInformation.setFreeSwapSpaceSize(machineMBeanServer.getLongAttr(OPERATING_SYSTEM_NAME, "FreeSwapSpaceSize", 0L));
            runtimeInformation.setTotalPhysicalMemorySize(machineMBeanServer.getLongAttr(OPERATING_SYSTEM_NAME, "TotalPhysicalMemorySize", 0L));
            runtimeInformation.setTotalSwapSpaceSize(machineMBeanServer.getLongAttr(OPERATING_SYSTEM_NAME, "TotalSwapSpaceSize", 0L));
            runtimeInformation.setProcessCpuTime(machineMBeanServer.getLongAttr(OPERATING_SYSTEM_NAME, "ProcessCpuTime", 0L));
        } catch (Exception e) {
            LOGGER.error("Faied to extract operating system stats", e);
        }
        virtualMachine.setRuntimeInformation(runtimeInformation);
    }

    private void collectThreadInformation(VirtualMachine virtualMachine) {
        ThreadMXBean threadMXBean = machineMBeanServer.getPlatformMXBean(ThreadMXBean.class);
        ThreadInformation threadInformation = new ThreadInformation();
        threadInformation.setDaemon(threadMXBean.getDaemonThreadCount());
        threadInformation.setNonDaemon(threadMXBean.getThreadCount() - threadMXBean.getDaemonThreadCount());
        virtualMachine.setThreadInformation(threadInformation);
    }

    private void collectThreadDumps(VirtualMachine virtualMachine) {
        if (!machineMBeanServer.isLocal()) return;
        ThreadDump threadDump = new ThreadDump();
        virtualMachine.setThreadDump(threadDump);
    }

    private void collectPid(VirtualMachine virtualMachine) {
        virtualMachine.setPid(-1);
        if (machineMBeanServer.isLocal()) {
            virtualMachine.setPid((int) ProcessHandle.current().pid());
        } else {
            String name = machineMBeanServer.getPlatformMXBean(RuntimeMXBean.class).getName();
            if (name.contains("@")) {
                String pid = StringUtils.split(name, "@")[0];
                try {
                    virtualMachine.setPid(Integer.parseInt(pid));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
    }

    private void collectProcess(VirtualMachine virtualMachine) {
        if (!machineMBeanServer.isLocal()) return;
        Process process = new Process();
        process.setPid((int) ProcessHandle.current().pid());
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        OSProcess osProcess = operatingSystem.getProcess(operatingSystem.getProcessId());
        if (osProcess != null) {
            process.setCpuSystemTime(osProcess.getKernelTime());
            process.setCpuUserTime(osProcess.getUserTime());
            process.setMemoryVirtual(osProcess.getVirtualSize());
            process.setMemoryResident(osProcess.getResidentSetSize());
            process.setFileDescriptors((int) osProcess.getOpenFiles());
            process.setThreads(osProcess.getThreadCount());
            process.setStartupTime(osProcess.getStartTime());
            process.setUptime(osProcess.getUpTime());
            process.setBytesRead(osProcess.getBytesRead());
            process.setBytesWritten(osProcess.getBytesWritten());
            process.setMinorFaults(osProcess.getMinorFaults());
            process.setMajorFaults(osProcess.getMajorFaults());
        }
        virtualMachine.setProcess(process);
    }

    private MemoryPool.Type guessMemoryType(MemoryPoolMXBean memoryPoolMXBean) {
        String name = memoryPoolMXBean.getName();
        if (edenMemoryPools.contains(name)) {
            return MemoryPool.Type.EDEN;
        } else if (tenureMemoryPools.contains(name)) {
            return MemoryPool.Type.TENURED;
        } else if (survivorPools.contains(name)) {
            return MemoryPool.Type.SURVIVOR;
        } else if (name.equalsIgnoreCase(CODE_MEMORY_POOL_NAME)) {
            return MemoryPool.Type.CODE_CACHE;
        } else if (name.startsWith(CODE_HEAP_POOL_NAME)) {
            return MemoryPool.Type.CODE_HEAP;
        } else if (name.equalsIgnoreCase(PERM_GEN_MEMORY_POOL_NAME)) {
            return MemoryPool.Type.PERM_GEN;
        } else if (name.equalsIgnoreCase(METASPACE_POOL_NAME)) {
            return MemoryPool.Type.METASPACE;
        } else if (name.equalsIgnoreCase(COMPRESSED_CODE_CLASS_POOL_NAME)) {
            return MemoryPool.Type.CLASS_SPACE;
        } else {
            return MemoryPool.Type.UNKNOWN;
        }
    }

    private GarbageCollection.Type guessGarbageCollectorType(GarbageCollectorMXBean garbageCollectorMXBean) {
        String name = garbageCollectorMXBean.getName();
        if (edenGCNames.contains(name)) {
            return GarbageCollection.Type.EDEN;
        } else if (tenuredGCNames.contains(name)) {
            return GarbageCollection.Type.TENURED;
        } else {
            return GarbageCollection.Type.UNKNOWN;
        }
    }

    private BufferPool.Type guessBufferPoolType(BufferPoolMXBean bufferPoolMXBean) {
        String name = bufferPoolMXBean.getName();
        if ("direct".equals(name)) {
            return BufferPool.Type.DIRECT;
        } else if ("mapped".equals(name)) {
            return BufferPool.Type.MAPPED;
        } else {
            return BufferPool.Type.UNKNOWN;
        }
    }

    private static final Set<String> edenGCNames = new HashSet<>();
    private static final Set<String> tenuredGCNames = new HashSet<>();
    private static final Set<String> edenMemoryPools = new HashSet<>();
    private static final Set<String> survivorPools = new HashSet<>();
    private static final Set<String> tenureMemoryPools = new HashSet<>();

    private static final String CODE_MEMORY_POOL_NAME = "Code Cache";
    private static final String CODE_HEAP_POOL_NAME = "CodeHeap";
    private static final String COMPRESSED_CODE_CLASS_POOL_NAME = "Compressed Class Space";
    private static final String PERM_GEN_MEMORY_POOL_NAME = "PS Perm Gen";
    private static final String METASPACE_POOL_NAME = "Metaspace";

    static {
        edenGCNames.add("Copy");
        edenGCNames.add("ParNew");
        edenGCNames.add("PS Scavenge");
        edenGCNames.add("G1 Young Generation");
        edenGCNames.add("ZGC Minor Pauses");
        edenGCNames.add("ZGC Minor Cycles");

        tenuredGCNames.add("MarkSweepCompact");
        tenuredGCNames.add("ConcurrentMarkSweep");
        tenuredGCNames.add("PS MarkSweep");
        tenuredGCNames.add("G1 Old Generation");
        tenuredGCNames.add("ZGC Major Cycles");
        tenuredGCNames.add("ZGC Major Pauses");

        survivorPools.add("PS Survivor Space");
        survivorPools.add("Par Survivor Space");
        survivorPools.add("G1 Survivor Space");

        edenMemoryPools.add("PS Eden Space");
        edenMemoryPools.add("Par Eden Space");
        edenMemoryPools.add("G1 Eden Space");
        edenMemoryPools.add("ZGC Young Generation");

        tenureMemoryPools.add("PS Old Gen");
        tenureMemoryPools.add("CMS Old Gen");
        tenureMemoryPools.add("G1 Old Gen");
        tenureMemoryPools.add("ZGC Old Generation");
    }
}
