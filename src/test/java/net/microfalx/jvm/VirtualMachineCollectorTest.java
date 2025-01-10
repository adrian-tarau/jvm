package net.microfalx.jvm;

import net.microfalx.jvm.model.VirtualMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class VirtualMachineCollectorTest {

    private VirtualMachineCollector collector;

    @BeforeEach
    public void setup() {
        VirtualMachineMBeanServer server = VirtualMachineMBeanServer.local();
        collector = new VirtualMachineCollector(server);
    }

    @Test
    void collectJvm() {
        VirtualMachine vm = collector.execute();
        assertNotNull(vm);
    }

    @Test
    void collectProcess() {
        VirtualMachine vm = collector.execute();
        assertNotNull(vm);
    }

}