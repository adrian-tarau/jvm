package net.microfalx.jvm;

import net.microfalx.jvm.model.Process;
import net.microfalx.jvm.model.*;
import net.microfalx.lang.ArgumentUtils;
import net.microfalx.lang.JvmUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.hardware.CentralProcessor.TickType;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.lang.management.*;
import java.util.*;
import java.util.stream.Collectors;

import static net.microfalx.lang.StringUtils.toIdentifier;

/**
 * Collects information about a virtual machine.
 */
public class VirtualMachineCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualMachineCollector.class);

    private static final String OPERATING_SYSTEM_NAME = "java.lang:type=OperatingSystem";

    private final VirtualMachineMBeanServer machineMBeanServer;
    private final SystemInfo systemInfo = new SystemInfo();

    private static volatile long[][] prevTicks;
    private boolean metadata;

    public VirtualMachineCollector(VirtualMachineMBeanServer machineMBeanServer) {
        ArgumentUtils.requireNonNull(machineMBeanServer);
        this.machineMBeanServer = machineMBeanServer;
    }

    public VirtualMachine execute() {
        VirtualMachine vm = new VirtualMachine();
        try (Timer ignored = VirtualMachineUtils.METRICS.startTimer("Collect")) {
            vm.setLocal(machineMBeanServer.isLocal());
            collectPid(vm);
            collectProcess(vm);
            collectServer(vm);
            collectOs(vm);
            collectMemoryStats(vm);
            collectGarbageCollection(vm);
            collectBufferPools(vm);
            collectRuntimeInformation(vm);
            collectThreadInformation(vm);
            if (!metadata) collectThreadDumps(vm);
        }
        return vm;
    }

    public VirtualMachineCollector setMetadata(boolean metadata) {
        this.metadata = metadata;
        return this;
    }

    public Os getOs() {
        VirtualMachine vm = new VirtualMachine();
        collectOs(vm);
        return vm.getOs();
    }

    public Server getServer() {
        VirtualMachine vm = new VirtualMachine();
        collectServer(vm);
        return vm.getServer();
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
            virtualMachine.setPid(ProcessHandle.current().pid());
        } else {
            String name = machineMBeanServer.getPlatformMXBean(RuntimeMXBean.class).getName();
            if (name.contains("@")) {
                String pid = StringUtils.split(name, "@")[0];
                try {
                    virtualMachine.setPid(Long.parseLong(pid));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
    }

    private void collectOs(VirtualMachine virtualMachine) {
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        Os os = new Os();
        os.setName(operatingSystem.getFamily());
        os.setVersion(operatingSystem.getVersionInfo().toString());
        virtualMachine.setOs(os);
    }

    private void collectProcess(VirtualMachine virtualMachine) {
        if (!machineMBeanServer.isLocal()) return;
        Process process = new Process();
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        OSProcess osProcess = operatingSystem.getProcess(operatingSystem.getProcessId());
        if (osProcess != null) {
            process.setFileDescriptors((int) osProcess.getOpenFiles());
            process.setThreads(osProcess.getThreadCount());
            process.setThreads(osProcess.getThreadCount());
        }
        virtualMachine.setProcess(process);
    }

    private void collectServer(VirtualMachine virtualMachine) {
        if (!machineMBeanServer.isLocal()) return;
        Server server = new Server();
        server.setHostName(machineMBeanServer.isLocal() ? JvmUtils.getLocalHost().getCanonicalHostName() : machineMBeanServer.getAddress().getHostName());
        server.setId(toIdentifier(server.getHostName()));
        extractCpu(server);
        extractMemory(server);
        extractNetwork(server);
        extractDisk(server);
        extractMisc(server);
        virtualMachine.setServer(server);
    }

    private void extractMemory(Server server) {
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        server.setMemoryTotal(memory.getTotal());
        server.setMemoryUsed(memory.getTotal() - memory.getAvailable());
        server.setMemoryActuallyUsed(server.getMemoryUsed());
        VirtualMemory virtualMemory = memory.getVirtualMemory();
        server.setSwapTotal(virtualMemory.getSwapTotal());
        server.setSwapUsed(virtualMemory.getSwapUsed());
        server.setSwapPageIn(virtualMemory.getSwapPagesIn());
        server.setSwapPageOut(virtualMemory.getSwapPagesOut());
    }

    private void extractDisk(Server server) {
        long reads = 0;
        long readBytes = 0;
        long writeBytes = 0;
        long writes = 0;

        List<HWDiskStore> diskStores = systemInfo.getHardware().getDiskStores();
        for (HWDiskStore diskStore : diskStores) {
            reads += diskStore.getReads();
            readBytes += diskStore.getReadBytes();
            writes += diskStore.getWrites();
            writeBytes += diskStore.getWriteBytes();
        }
        server.setIoReads(reads);
        server.setIoReadBytes(readBytes);
        server.setIoWrites(writes);
        server.setIoWriteBytes(writeBytes);

        Collection<net.microfalx.jvm.model.FileSystem> fileSystemInformations = new ArrayList<>();
        FileSystem fileSystem = systemInfo.getOperatingSystem().getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        long diskTotal = 0;
        long diskUsed = 0;
        long totalNodes = 0;
        long freeNodes = 0;
        for (OSFileStore fileStore : fileStores) {
            diskTotal += fileStore.getTotalSpace();
            diskUsed += fileStore.getTotalSpace() - fileStore.getUsableSpace();
            totalNodes += fileStore.getTotalInodes();
            freeNodes += fileStore.getFreeInodes();
            fileSystemInformations.add(create(fileStore));
        }
        server.setFileSystems(fileSystemInformations.stream()
                .filter(net.microfalx.jvm.model.FileSystem::isDisk)
                .collect(Collectors.toList()));
        server.setDiskInodeCount(totalNodes);
        server.setDiskInodeCountUsed(totalNodes - freeNodes);
        server.setDiskTotal(diskTotal);
        server.setDiskUsed(diskUsed);
    }

    private net.microfalx.jvm.model.FileSystem create(OSFileStore fileStore) {
        net.microfalx.jvm.model.FileSystem disk = new net.microfalx.jvm.model.FileSystem();
        disk.setId(fileStore.getUUID());
        disk.setName(fileStore.getName());
        disk.setDescription(fileStore.getDescription());
        disk.setMount(fileStore.getMount());
        disk.setType(net.microfalx.jvm.model.FileSystem.Type.fromString(fileStore.getType()));
        disk.setTotalSpace(fileStore.getTotalSpace());
        disk.setFreeSpace(fileStore.getFreeSpace());
        disk.setUsableSpace(fileStore.getUsableSpace());
        disk.setTotalInodes(fileStore.getTotalInodes());
        disk.setFreeInodes(fileStore.getFreeInodes());
        return disk;
    }

    private void extractNetwork(Server server) {
        long readBytes = 0;
        long writeBytes = 0;
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        for (NetworkIF networkIF : networkIFs) {
            readBytes += networkIF.getBytesRecv();
            writeBytes += networkIF.getBytesSent();
        }
        server.setNetworkReadBytes(readBytes);
        server.setNetworkWriteBytes(writeBytes);
    }

    private void extractMisc(Server server) {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        server.setContextSwitches(processor.getContextSwitches());
        server.setInterrupts(processor.getInterrupts());
        server.setUptime((int) systemInfo.getOperatingSystem().getSystemUptime());
    }

    private void extractCpu(Server server) {
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        server.setCores(processor.getPhysicalProcessorCount());
        server.setThreads(processor.getLogicalProcessorCount());
        server.setContainerThreads(-1);
        server.setLoad((float) processor.getSystemLoadAverage(1)[0]);
        if (metadata) return;
        if (prevTicks != null) {
            double[] load = processor.getProcessorCpuLoadBetweenTicks(prevTicks);
            server.setCpuSystem(getTick(TickType.SYSTEM, load));
            server.setCpuUser(getTick(TickType.USER, load));
            server.setCpuNice(getTick(TickType.NICE, load));
            server.setCpuIoWait(getTick(TickType.IOWAIT, load));
            server.setCpuIdle(getTick(TickType.IDLE, load));
            server.setCpuIrq(getTick(TickType.IRQ, load));
            server.setCpuSoftIrq(getTick(TickType.SOFTIRQ, load));
            server.setCpuStolen(getTick(TickType.STEAL, load));
            server.setCpuTotal(server.getCpuUser() + server.getCpuNice() + server.getCpuSystem() + server.getCpuIdle()
                    + server.getCpuIoWait() + server.getCpuIrq() + server.getCpuSoftIrq() + server.getCpuStolen());
        }
        prevTicks = processor.getProcessorCpuLoadTicks();
    }

    private float getTick(TickType type, double[] load) {
        return (float) load[type.getIndex()];
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
