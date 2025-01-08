package net.microfalx.jvm.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class RuntimeInformation implements Serializable {

    private static final long serialVersionUID = 7951723934123040651L;

    private long committedVirtualMemorySize;
    private long freePhysicalMemorySize;
    private long freeSwapSpaceSize;
    private long processCpuTime;
    private long totalPhysicalMemorySize;
    private long totalSwapSpaceSize;
    private String osName;
    private String osVersion;
    private long startTime;
    private long uptime;

}
