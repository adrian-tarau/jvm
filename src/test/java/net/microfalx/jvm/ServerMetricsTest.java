package net.microfalx.jvm;

import net.microfalx.jvm.model.Server;
import net.microfalx.lang.ThreadUtils;
import net.microfalx.lang.annotation.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.Duration.ofSeconds;
import static net.microfalx.lang.FormatterUtils.formatPercent;
import static net.microfalx.lang.ThreadUtils.sleepSeconds;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerMetricsTest extends AbstractMetricsTest {

    private ServerMetrics metrics;

    @BeforeEach
    public void setup() {
        metrics = (ServerMetrics) new ServerMetrics().useMemory();
        metrics.clear();
    }

    @Test
    public void scrape() {
        metrics.scrape();
        assertFalse(metrics.getStore().getMetrics().isEmpty());
    }

    @Test
    public void start() {
        metrics.start();
        ThreadUtils.sleepSeconds(5);
        metrics.stop();
    }

    @Test
    public void memory() {
        scrapeInLoop();
        assertTrue(metrics.getStore().getAverage(ServerMetrics.MEMORY_MAX, ofSeconds(60)).orElse(0) > 0);
        assertTrue(metrics.getStore().getAverage(ServerMetrics.MEMORY_USED, ofSeconds(60)).orElse(0) > 0);
        assertTrue(metrics.getStore().getAverage(ServerMetrics.MEMORY_ACTUALLY_USED, ofSeconds(60)).orElse(0) > 0);
    }

    @Test
    public void cpu() {
        scrapeInLoop();
        assertTrue(metrics.getStore().getAverage(ServerMetrics.CPU_TOTAL, ofSeconds(60)).orElse(0) > 0);
        assertTrue(metrics.getStore().getAverage(ServerMetrics.CPU_USER, ofSeconds(60)).orElse(0) > 0);
        assertTrue(metrics.getStore().getAverage(ServerMetrics.CPU_SYSTEM, ofSeconds(60)).orElse(0) > 0);
    }

    @Test
    public void cpuUser() {
        startBusyThreads(4);
        scrapeInLoop();
        double avgCpu = metrics.getStore().getAverage(ServerMetrics.CPU_USER, ofSeconds(60)).orElse(0);
        assertTrue(avgCpu > 400);
    }

    @Ignore
    @Test
    public void realTimeCpu() {
        startBusyThreads(4);
        for (; ; ) {
            metrics.scrape();
            Server server = metrics.getLast();
            ThreadUtils.sleepSeconds(1);
            System.out.println("CPU: System " + formatPercent(server.getCpuSystem())
                               + ", User " + formatPercent(server.getCpuUser()));
        }
    }

    private void scrapeInLoop() {
        scrapeInLoop(5);
    }

    private void scrapeInLoop(int iterations) {
        for (int i = 0; i < iterations; i++) {
            metrics.scrape();
            sleepSeconds(1);
        }
    }

}