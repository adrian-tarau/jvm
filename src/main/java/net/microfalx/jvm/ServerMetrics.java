package net.microfalx.jvm;

import net.microfalx.jvm.model.Server;
import net.microfalx.metrics.Batch;
import net.microfalx.metrics.Metric;

import java.util.DoubleSummaryStatistics;
import java.util.LongSummaryStatistics;

/**
 * A singleton class which collects JVM metrics and stores them in the store.
 */
public final class ServerMetrics extends AbstractMetrics<Server, ServerCollector> {

    private static final ServerMetrics instance = new ServerMetrics();
    private final ServerCollector collector = new ServerCollector();

    private final DoubleSummaryStatistics cpuStatistics = new DoubleSummaryStatistics();
    private final DoubleSummaryStatistics loadStatistics = new DoubleSummaryStatistics();
    private final LongSummaryStatistics memoryStatistics = new LongSummaryStatistics();

    private volatile Server last;

    /**
     * Returns the global instance.
     *
     * @return a non-null instance
     */
    public static ServerMetrics get() {
        return instance;
    }

    /**
     * Returns the last virtual machine collected.
     *
     * @return a non-null instance
     */
    public Server getLast() {
        if (last == null) last = Server.get();
        return last;
    }

    /**
     * Returns the average used CPU since process startup.
     *
     * @return the CPU, between 0 and 100
     */
    public float getAverageCpu() {
        return (float) cpuStatistics.getAverage();
    }

    /**
     * Returns the average system load since the process startup.
     *
     * @return the load
     */
    public double getAverageLoad() {
        return loadStatistics.getAverage();
    }

    /**
     * Returns the average memory usage.
     *
     * @return a positive integer
     */
    public long getAverageMemory() {
        return (long) memoryStatistics.getAverage();
    }

    @Override
    protected void collectMetrics(Batch batch) {
        Server server = collector.execute();
        collectMemory(server, batch);
        collectCpu(server, batch);
        updateStatistics(server);
        this.last = server;
    }

    static void collectMemory(Server server, Batch batch) {
        batch.add(MEMORY_MAX, server.getMemoryTotal());
        batch.add(MEMORY_USED, server.getMemoryUsed());
        batch.add(MEMORY_ACTUALLY_USED, server.getMemoryActuallyUsed());
    }

    static void collectCpu(Server server, Batch batch) {
        batch.add(CPU_TOTAL, server.getCpuTotal());
        batch.add(CPU_USER, server.getCpuUser());
        batch.add(CPU_SYSTEM, server.getCpuSystem());
        batch.add(CPU_IO_WAIT, server.getCpuIoWait());
        batch.add(CPU_NICE, server.getCpuNice());
    }

    private void updateStatistics(Server server) {
        cpuStatistics.accept(server.getCpuTotal());
        loadStatistics.accept(server.getLoad1());
        memoryStatistics.accept(server.getMemoryActuallyUsed());
    }

    private static final String METRIC_PREFIX = "server.";

    public static final Metric MEMORY_MAX = Metric.get(METRIC_PREFIX + "memory.max").withGroup("Server / Memory").withDisplayName("Maximum");
    public static final Metric MEMORY_USED = Metric.get(METRIC_PREFIX + "memory.used").withGroup("Server / Memory").withDisplayName("Used");
    public static final Metric MEMORY_ACTUALLY_USED = Metric.get(METRIC_PREFIX + "memory.actually.used").withGroup("Server / Memory").withDisplayName("Actually Used");

    public static final Metric CPU_TOTAL = Metric.get(METRIC_PREFIX + "cpu.total").withGroup("CPU").withDisplayName("Total");
    public static final Metric CPU_USER = Metric.get(METRIC_PREFIX + "cpu.user").withGroup("CPU").withDisplayName("User");
    public static final Metric CPU_SYSTEM = Metric.get(METRIC_PREFIX + "cpu.system").withGroup("CPU").withDisplayName("System");
    public static final Metric CPU_IO_WAIT = Metric.get(METRIC_PREFIX + "cpu.io_wait").withGroup("CPU").withDisplayName("I/O Wait");
    public static final Metric CPU_NICE = Metric.get(METRIC_PREFIX + "cpu.nice").withGroup("CPU").withDisplayName("Nice");

    public static final Metric LOAD_1 = Metric.get(METRIC_PREFIX + "load.1").withGroup("Load").withDisplayName("1 Minute");
    public static final Metric LOAD_5 = Metric.get(METRIC_PREFIX + "load.5").withGroup("Load").withDisplayName("5 Minutes");
    public static final Metric LOAD_15 = Metric.get(METRIC_PREFIX + "load.15").withGroup("Load").withDisplayName("15 Minutes");
}
