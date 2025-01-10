package net.microfalx.jvm;

import org.junit.jupiter.api.Test;

import static net.microfalx.jvm.VirtualMachineUtils.getAverageUsage;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VirtualMachineUtilsTest {

    private static final long startTime = System.currentTimeMillis();

    @Test
    void assertAverage() {
        assertEquals(5, getAverageUsage(startTime, 10), 0.01);
        assertEquals(20, getAverageUsage(startTime, 300), 0.01);
    }

}