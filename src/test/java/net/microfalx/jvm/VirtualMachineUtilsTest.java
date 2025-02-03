package net.microfalx.jvm;

import net.microfalx.lang.ThreadUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static net.microfalx.jvm.VirtualMachineUtils.getUsageAtNow;

class VirtualMachineUtilsTest {

    @Test
    void underUsed() {
        long startTime = System.nanoTime();
        ThreadUtils.sleepMillis(100);
        Assertions.assertThat(getUsageAtNow(startTime, 10)).isBetween(5f, 15f);
    }

    @Test
    void overUsed() {
        long startTime = System.nanoTime();
        ThreadUtils.sleepMillis(100);
        Assertions.assertThat(getUsageAtNow(startTime, 200)).isBetween(150f, 250f);
    }

}