package net.microfalx.jvm;

import net.microfalx.jvm.model.Process;
import net.microfalx.jvm.model.VirtualMachine;
import net.microfalx.metrics.Batch;
import net.microfalx.metrics.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.DoubleSummaryStatistics;
import java.util.LongSummaryStatistics;

/**
 * A singleton class which collects JVM metrics and stores them in the store.
 */
public final class VirtualMachineMetrics extends AbstractMetrics<VirtualMachine, VirtualMachineCollector> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualMachineMetrics.class);

    private static final VirtualMachineMetrics instance = new VirtualMachineMetrics();
    private final VirtualMachineCollector collector = new VirtualMachineCollector(VirtualMachineMBeanServer.local());

    private volatile VirtualMachine last = new VirtualMachine();
    private final DoubleSummaryStatistics cpuStatistics = new DoubleSummaryStatistics();
    private final LongSummaryStatistics heapStatistics = new LongSummaryStatistics();
    private final LongSummaryStatistics nonHeapStatistics = new LongSummaryStatistics();

    /**
     * Returns the global instance.
     *
     * @return a non-null instance
     */
    public static VirtualMachineMetrics get() {
        return instance;
    }

    /**
     * Returns the average used CPU since startup.
     *
     * @return the CPU, between 0 and 100
     */
    public float getAverageCpu() {
        return (float) cpuStatistics.getAverage();
    }

    /**
     * Returns the maximum memory size (HEAP and NON-HEAP).
     *
     * @return a positive integer
     */
    public long getMemoryMaximum() {
        return getHeapMemoryMaximum() + getNonHeapMemoryMaximum();
    }

    /**
     * Returns the average memory usage.
     *
     * @return the value in bytes
     */
    public long getMemoryAverage() {
        return getHeapMemoryAverage() + getNonHeapMemoryAverage();
    }

    /**
     * Returns the maximum size of HEAP memory.
     *
     * @return the value in bytes
     */
    public long getHeapMemoryMaximum() {
        return last.getHeapTotalMemory();
    }

    /**
     * Returns the average HEAP usage.
     *
     * @return the value in bytes
     */
    public long getHeapMemoryAverage() {
        return (long) heapStatistics.getAverage();
    }

    /**
     * Returns the maximum size of NON_HEAP memory.
     *
     * @return the value in bytes
     */
    public long getNonHeapMemoryMaximum() {
        return last.getNonHeapTotalMemory();
    }

    /**
     * Returns the average NON_HEAP usage.
     *
     * @return the value in bytes
     */
    public long getNonHeapMemoryAverage() {
        return (long) nonHeapStatistics.getAverage();
    }

    /**
     * Returns the last virtual machine collected.
     *
     * @return a non-null instance
     */
    public VirtualMachine getLast() {
        if (last == null) last = VirtualMachine.get();
        return last;
    }

    @Override
    protected void collectMetrics(Batch batch) {
        VirtualMachine virtualMachine = collector.execute();
        collectMemory(virtualMachine, batch);
        collectCpu(virtualMachine, batch);
        updateStatistics(virtualMachine);
        this.last = virtualMachine;
    }

    private static void collectMemory(VirtualMachine vm, Batch batch) {
        batch.add(MEMORY_HEAP_MAX, vm.getHeapTotalMemory());
        batch.add(MEMORY_HEAP_USED, vm.getHeapUsedMemory());
        batch.add(MEMORY_NON_HEAP_MAX, vm.getNonHeapTotalMemory());
        batch.add(MEMORY_NON_HEAP_USED, vm.getNonHeapUsedMemory());
        batch.add(MEMORY_EDEN_USED, vm.getTenuredMemoryPool().getUsed());
        batch.add(MEMORY_EDEN_MAX, vm.getEdenMemoryPool().getMaximum());
        batch.add(MEMORY_TENURED_MAX, vm.getTenuredMemoryPool().getMaximum());
        batch.add(MEMORY_TENURED_USED, vm.getTenuredMemoryPool().getUsed());
    }

    private static void collectCpu(VirtualMachine vm, Batch batch) {
        Process process = vm.getProcess();
        batch.add(CPU_TOTAL, process.getCpuTotal());
        batch.add(CPU_USER, process.getCpuUser());
        batch.add(CPU_SYSTEM, process.getCpuSystem());
        batch.add(CPU_IO_WAIT, process.getCpuIoWait());
    }

    private void updateStatistics(VirtualMachine vm) {
        Process process = vm.getProcess();
        cpuStatistics.accept(process.getCpuTotal());
        heapStatistics.accept(vm.getHeapUsedMemory());
        nonHeapStatistics.accept(vm.getNonHeapUsedMemory());
    }

    private static final String METRIC_PREFIX = "jvm.";

    public static final Metric MEMORY_HEAP_MAX = Metric.get(METRIC_PREFIX + "memory.heap.max").withGroup("Heap").withDisplayName("Maximum");
    public static final Metric MEMORY_HEAP_USED = Metric.get(METRIC_PREFIX + "memory.heap.used").withGroup("Heap").withDisplayName("Used");
    public static final Metric MEMORY_NON_HEAP_MAX = Metric.get(METRIC_PREFIX + "memory.non_heap.max").withGroup("NonHeap").withDisplayName("Maximum");
    public static final Metric MEMORY_NON_HEAP_USED = Metric.get(METRIC_PREFIX + "memory.non_heap.used").withGroup("NonHeap").withDisplayName("Used");
    public static final Metric MEMORY_EDEN_MAX = Metric.get(METRIC_PREFIX + "memory.eden.max").withGroup("Eden").withDisplayName("Maximum");
    public static final Metric MEMORY_EDEN_USED = Metric.get(METRIC_PREFIX + "memory.eden.used").withGroup("Eden").withDisplayName("Used");
    public static final Metric MEMORY_TENURED_MAX = Metric.get(METRIC_PREFIX + "memory.tenured.max").withGroup("Tenured").withDisplayName("Maximum");
    public static final Metric MEMORY_TENURED_USED = Metric.get(METRIC_PREFIX + "memory.tenured.used").withGroup("Tenured").withDisplayName("Used");

    public static final Metric CPU_TOTAL = Metric.get(METRIC_PREFIX + "cpu.total").withGroup("CPU").withDisplayName("Total");
    public static final Metric CPU_USER = Metric.get(METRIC_PREFIX + "cpu.user").withGroup("CPU").withDisplayName("User");
    public static final Metric CPU_SYSTEM = Metric.get(METRIC_PREFIX + "cpu.system").withGroup("CPU").withDisplayName("System");
    public static final Metric CPU_IO_WAIT = Metric.get(METRIC_PREFIX + "cpu.io_wait").withGroup("CPU").withDisplayName("I/O Wait");

}
