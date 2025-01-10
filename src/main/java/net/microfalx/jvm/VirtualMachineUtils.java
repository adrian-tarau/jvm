package net.microfalx.jvm;

import net.microfalx.metrics.Metrics;

public class VirtualMachineUtils {

    protected final static Metrics METRICS = Metrics.ROOT.withGroup("VM");

    /**
     * Calculate the usage time in percent based on uptime and actual usage.
     *
     * @param startupTime the uptime in milliseconds
     * @param usage       the usage in milliseconds
     * @return the usage as percentage
     */
    public static float getAverageUsage(long startupTime, long usage) {
        long uptime = System.currentTimeMillis() - startupTime;
        return uptime > 0 ? 100 * (float) usage / uptime : 0;
    }
}
