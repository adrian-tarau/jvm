package net.microfalx.jvm;

import net.microfalx.lang.TimeUtils;
import net.microfalx.metrics.Metrics;

public class VirtualMachineUtils {

    protected final static Metrics METRICS = Metrics.ROOT.withGroup("VM");

    /**
     * Calculate the usage time in percent based on uptime and actual usage.
     *
     * @param prevTime the previous time reference in nanos
     * @param usage    the usage in milliseconds
     * @return the usage as percentage
     */
    public static float getUsageAtNow(long prevTime, long usage) {
        return getUsage(System.nanoTime() - prevTime, usage);
    }

    /**
     * Calculate the usage time in percent based on uptime and actual usage.
     *
     * @param duration the duration of the time interval in nanos
     * @param usage    the usage in milliseconds
     * @return the usage as percentage
     */
    public static float getUsage(long duration, long usage) {
        usage *= TimeUtils.NANOSECONDS_IN_MILLISECONDS;
        return duration > 0 ? (float) (100 * (double) usage / duration) : 0;
    }
}
