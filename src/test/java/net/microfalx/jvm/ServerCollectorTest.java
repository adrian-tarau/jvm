package net.microfalx.jvm;

import net.microfalx.jvm.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServerCollectorTest {

    private ServerCollector collector;

    @BeforeEach
    public void setup() {
        collector = new ServerCollector();
    }

    @Test
    void collectMetadata() {
        collector.setMetadata(true);
        collectAndAssert();
    }

    @Test
    void collectAll() {
        collectAndAssert();
    }

    private void collectAndAssert() {
        Server server = collector.execute();
        for (int i = 0; i < 2; i++) {
            server = collector.execute();
        }
        assertNotNull(server);
    }

}