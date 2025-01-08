package net.microfalx.jvm;

import net.microfalx.jvm.model.Server;
import net.microfalx.jvm.model.VirtualMachine;
import net.microfalx.metrics.Batch;
import net.microfalx.metrics.Metric;
import net.microfalx.metrics.SeriesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;
import static net.microfalx.lang.ExceptionUtils.getRootCauseMessage;

/**
 * A singleton class which collects JVM metrics and stores them in the store.
 */
public class VirtualMachineMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualMachineMetrics.class);

    private static final VirtualMachineMetrics instance = new VirtualMachineMetrics();

    private ScheduledExecutorService executor;
    private volatile String name;
    private volatile boolean memory;
    private volatile boolean started;
    private volatile Duration interval = Duration.ofSeconds(5);
    private volatile Future<?> scrapeTask;
    private volatile SeriesStore seriesStore;
    private volatile VirtualMachineCollector collector;

    /**
     * Returns the global instance.
     *
     * @return a non-null instance
     */
    public static VirtualMachineMetrics get() {
        return instance;
    }

    protected VirtualMachineMetrics() {
    }

    /**
     * Returns whether the metrics are stored in memory.
     *
     * @return {@code true} in memory, {@code false} otherwise
     */
    public boolean isMemory() {
        return memory;
    }

    /**
     * Changes the metrics store to memory.
     *
     * @return self
     */
    public VirtualMachineMetrics useMemory() {
        checkIfStarted();
        this.memory = true;
        return this;
    }

    /**
     * Changes the metrics store to memory.
     *
     * @param name the name of the store
     * @return self
     */
    public VirtualMachineMetrics useDisk(String name) {
        requireNotEmpty(name);
        checkIfStarted();
        this.memory = false;
        this.name = name;
        return this;
    }

    /**
     * Returns the scrape interval.
     *
     * @return a non-null instance
     */
    public Duration getInterval() {
        return interval;
    }

    /**
     * Changes the scrape interval.
     *
     * @param interval the new interval
     * @return self
     */
    public VirtualMachineMetrics setInterval(Duration interval) {
        requireNonNull(interval);
        this.interval = interval;
        createScrapeTask();
        return this;
    }

    /**
     * Changes the executor service.
     *
     * @param executor the executor
     */
    public void setExecutor(ScheduledExecutorService executor) {
        requireNonNull(executor);
        this.executor = executor;
    }

    /**
     * Returns the store for JVM metrics.
     *
     * @return a non-null instance
     */
    public synchronized SeriesStore getStore() {
        if (seriesStore == null) initialize();
        return seriesStore;
    }

    /**
     * Starts data collection.
     */
    public synchronized void start() {
        checkIfStarted();
        if (executor == null) executor = Executors.newScheduledThreadPool(2);
        initialize();
        createScrapeTask();
        started = true;
    }

    /**
     * Stops the data collection.
     */
    public synchronized void stop() {
        started = false;
    }

    /**
     * Clear the metrics.
     */
    public synchronized void clear() {
        if (this.seriesStore != null) this.seriesStore.clear();
    }

    /**
     * Returns whether the metrics collection is started.
     *
     * @return {@code true} if started, {@code false} otherwise
     */
    public synchronized boolean isStarted() {
        return started;
    }

    /**
     * Scrapes for new metrics.
     */
    public void scrape() {
        if (seriesStore == null) initialize();
        VirtualMachine virtualMachine = collector.execute();
        collectMetrics(virtualMachine);
    }

    private void checkIfStarted() {
        if (started) throw new VirtualMachineException("Already started");
    }

    private synchronized void initialize() {
        if (seriesStore == null) {
            seriesStore = memory ? SeriesStore.memory() : SeriesStore.disk(name);
            collector = new VirtualMachineCollector(VirtualMachineMBeanServer.local());
        }
    }

    private void collectMetrics(VirtualMachine vm) {
        Batch batch = Batch.create(System.currentTimeMillis());
        collectMemory(vm, batch);
        collectServer(vm, batch);
        seriesStore.add(batch);
    }

    private void collectMemory(VirtualMachine vm, Batch batch) {
        batch.add(MEMORY_HEAP_MAX, vm.getHeapTotalMemory());
        batch.add(MEMORY_HEAP_USED, vm.getHeapUsedMemory());
        batch.add(MEMORY_NON_HEAP_MAX, vm.getNonHeapTotalMemory());
        batch.add(MEMORY_NON_HEAP_USED, vm.getNonHeapUsedMemory());
        batch.add(MEMORY_EDEN_USED, vm.getTenuredMemoryPool().getUsed());
        batch.add(MEMORY_EDEN_MAX, vm.getEdenMemoryPool().getMaximum());
        batch.add(MEMORY_TENURED_MAX, vm.getTenuredMemoryPool().getMaximum());
        batch.add(MEMORY_TENURED_USED, vm.getTenuredMemoryPool().getUsed());
    }

    private void collectServer(VirtualMachine vm, Batch batch) {
        Server server = vm.getServer();
        batch.add(CPU_TOTAL, server.getCpuTotal());
        batch.add(CPU_USER, server.getCpuUser());
        batch.add(CPU_SYSTEM, server.getCpuSystem());
        batch.add(CPU_IO_WAIT, server.getCpuIoWait());
        batch.add(CPU_NICE, server.getCpuNice());
    }

    private synchronized void createScrapeTask() {
        if (scrapeTask != null) scrapeTask.cancel(false);
        scrapeTask = executor.scheduleAtFixedRate(new CollectorWorker(), 0, interval.toMillis(), MILLISECONDS);
    }

    private static final String JVM_METRIC_PREFIX = "jvm.";
    private static final String SERVER_METRIC_PREFIX = "server.";

    public static final Metric MEMORY_HEAP_MAX = Metric.get(JVM_METRIC_PREFIX + "memory.heap.max").withGroup("Heap").withDisplayName("Maximum");
    public static final Metric MEMORY_HEAP_USED = Metric.get(JVM_METRIC_PREFIX + "memory.heap.used").withGroup("Heap").withDisplayName("Used");
    public static final Metric MEMORY_NON_HEAP_MAX = Metric.get(JVM_METRIC_PREFIX + "memory.non_heap.max").withGroup("NonHeap").withDisplayName("Maximum");
    public static final Metric MEMORY_NON_HEAP_USED = Metric.get(JVM_METRIC_PREFIX + "memory.non_heap.used").withGroup("NonHeap").withDisplayName("Used");
    public static final Metric MEMORY_EDEN_MAX = Metric.get(JVM_METRIC_PREFIX + "memory.eden.max").withGroup("Eden").withDisplayName("Maximum");
    public static final Metric MEMORY_EDEN_USED = Metric.get(JVM_METRIC_PREFIX + "memory.eden.used").withGroup("Eden").withDisplayName("Used");
    public static final Metric MEMORY_TENURED_MAX = Metric.get(JVM_METRIC_PREFIX + "memory.tenured.max").withGroup("Tenured").withDisplayName("Maximum");
    public static final Metric MEMORY_TENURED_USED = Metric.get(JVM_METRIC_PREFIX + "memory.tenured.used").withGroup("Tenured").withDisplayName("Used");

    public static final Metric CPU_TOTAL = Metric.get(SERVER_METRIC_PREFIX + "cpu.total").withGroup("CPU").withDisplayName("Total");
    public static final Metric CPU_USER = Metric.get(SERVER_METRIC_PREFIX + "cpu.user").withGroup("CPU").withDisplayName("User");
    public static final Metric CPU_SYSTEM = Metric.get(SERVER_METRIC_PREFIX + "cpu.system").withGroup("CPU").withDisplayName("System");
    public static final Metric CPU_IO_WAIT = Metric.get(SERVER_METRIC_PREFIX + "cpu.io_wait").withGroup("CPU").withDisplayName("I/O Wait");
    public static final Metric CPU_NICE = Metric.get(SERVER_METRIC_PREFIX + "cpu.nice").withGroup("CPU").withDisplayName("Nice");


    class CollectorWorker implements Runnable {

        private final VirtualMachineCollector collector = new VirtualMachineCollector(VirtualMachineMBeanServer.local());

        @Override
        public void run() {
            while (started) {
                try {
                    VirtualMachine virtualMachine = collector.execute();
                    collectMetrics(virtualMachine);
                    break;
                } catch (Exception e) {
                    if (e instanceof InterruptedException) break;
                    LOGGER.warn("Failed to collect VM metrics, root cause: {}", getRootCauseMessage(e));
                }
            }
        }
    }
}
