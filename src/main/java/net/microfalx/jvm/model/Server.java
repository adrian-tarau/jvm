package net.microfalx.jvm.model;

import lombok.Data;
import net.microfalx.jvm.ServerCollector;
import net.microfalx.lang.Identifiable;
import net.microfalx.lang.StringUtils;
import net.microfalx.lang.TimeUtils;
import net.microfalx.lang.Timestampable;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.util.Collection;

@Data
public class Server implements Identifiable<String>, Serializable, Timestampable {

    private static final long serialVersionUID = -8672559294889358525L;

    private String id = StringUtils.NA_STRING;
    private String hostName = "localhost";
    private long timestamp = System.currentTimeMillis();

    private Os os;

    private int cores;
    private int threads;
    private float containerThreads;
    private float cpuTotal;
    private float cpuSystem;
    private float cpuUser;
    private float cpuIoWait;
    private float cpuIdle;
    private float cpuNice;
    private float cpuIrq;
    private float cpuSoftIrq;
    private float cpuStolen;
    private float load1;
    private float load5;
    private float load15;
    private long contextSwitches;
    private long interrupts;
    private long memoryTotal;
    private long memoryUsed;
    private float memoryUsedPct;
    private long memoryActuallyUsed;
    private long memoryContainerTotal;
    private long memoryContainerUsed;
    private float memoryContainerUsedPct;
    private long memoryContainerActuallyUsed;
    private long swapTotal;
    private long swapUsed;
    private float swapUsedPct;
    private long swapContainerTotal;
    private long swapContainerUsed;
    private long swapPageIn;
    private long swapPageOut;
    private long diskTotal;
    private long diskUsed;
    private float diskUsedPct;
    private long diskInodeCount;
    private long diskInodeCountUsed;
    private long ioReads;
    private long ioWrites;
    private long ioReadBytes;
    private long ioWriteBytes;
    private long networkReadBytes;
    private long networkWriteBytes;
    private int processTotal;
    private int processRunning;
    private int processStopped;
    private int processIdle;
    private int processZombie;
    private int processSleeping;
    private int processThreads;
    private long uptime;

    private Collection<FileSystem> fileSystems;

    /**
     * Returns information about the current server.
     *
     * @return a non-null instance
     */
    public static Server get() {
        return get(false);
    }

    /**
     * Returns information about the current server.
     *
     * @param metadata {@code true} to collect only metadata, {@code metadata} to collect full stats
     * @return a non-null instance
     */
    public static Server get(boolean metadata) {
        ServerCollector collector = (ServerCollector) new ServerCollector()
                .setMetadata(metadata);
        return collector.execute();
    }

    @Override
    public Temporal getCreatedAt() {
        return TimeUtils.fromMillis(timestamp);
    }
}
