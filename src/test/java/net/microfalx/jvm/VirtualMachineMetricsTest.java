package net.microfalx.jvm;

import net.microfalx.lang.ThreadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.Duration.ofSeconds;
import static net.microfalx.lang.ThreadUtils.sleepSeconds;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualMachineMetricsTest {

    private VirtualMachineMetrics metrics;

    @BeforeEach
    public void setup() {
        metrics = (VirtualMachineMetrics) new VirtualMachineMetrics().useMemory();
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
        assertTrue(metrics.getStore().getAverage(VirtualMachineMetrics.MEMORY_HEAP_USED, ofSeconds(60)).orElse(0) > 0);
        assertTrue(metrics.getStore().getAverage(VirtualMachineMetrics.MEMORY_NON_HEAP_USED, ofSeconds(60)).orElse(0) > 0);
        assertTrue(metrics.getStore().getAverage(VirtualMachineMetrics.MEMORY_EDEN_USED, ofSeconds(60)).orElse(0) > 0);
        assertTrue(metrics.getStore().getAverage(VirtualMachineMetrics.MEMORY_TENURED_USED, ofSeconds(60)).orElse(0) > 0);
    }

    @Test
    public void cpu() {
        scrapeInLoop();
        assertTrue(metrics.getStore().getAverage(VirtualMachineMetrics.CPU_TOTAL, ofSeconds(60)).orElse(0) > 0);
        assertTrue(metrics.getStore().getAverage(VirtualMachineMetrics.CPU_USER, ofSeconds(60)).orElse(0) > 0);
        assertTrue(metrics.getStore().getAverage(VirtualMachineMetrics.CPU_SYSTEM, ofSeconds(60)).orElse(0) > 0);
    }

    private void scrapeInLoop() {
        for (int i = 0; i < 3; i++) {
            metrics.scrape();
            sleepSeconds(1);
        }
        sleepSeconds(1);
    }

}