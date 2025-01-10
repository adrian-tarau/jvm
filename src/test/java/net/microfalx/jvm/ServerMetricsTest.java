package net.microfalx.jvm;

import net.microfalx.lang.ThreadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.Duration.ofSeconds;
import static net.microfalx.lang.ThreadUtils.sleepSeconds;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerMetricsTest {

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

    private void scrapeInLoop() {
        for (int i = 0; i < 3; i++) {
            metrics.scrape();
            sleepSeconds(1);
        }
    }

}