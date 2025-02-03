package net.microfalx.jvm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractMetricsTest {

    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() / 2;

    private ExecutorService executor;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicLong counter = new AtomicLong();
    private final AtomicLong total = new AtomicLong(0);

    @BeforeEach
    void startExecutor() {
        started.set(true);
        executor = Executors.newFixedThreadPool(MAX_THREADS);
    }

    @AfterEach
    void stopExecutor() {
        started.set(false);
        executor.shutdownNow();
    }

    protected final void startBusyThreads(int count) {
        for (int i = 0; i < count; i++) {
            executor.submit(new BusyThread());
        }
    }

    private class BusyThread implements Runnable {


        @Override
        public void run() {
            while (started.get()) {
                total.addAndGet(counter.incrementAndGet());
            }
        }
    }
}
