package net.microfalx.jvm.model;

import lombok.Data;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.Serializable;
import java.util.*;

@Data
public class ThreadDump implements Serializable {

    private static final long serialVersionUID = -6503079159406114879L;

    private Collection<ThreadInformation> threads = new ArrayList<>();
    private Map<Long, ThreadInformation> threadsById;

    int daemonThread;
    int nonDaemonThread;

    public void addThread(ThreadInformation threadInformation) {
        this.threads.add(threadInformation);
    }

    /**
     * Returns information about a thread.
     *
     * @param id the thread identifier
     * @return the thread information, null if not null
     */
    public ThreadInformation getThread(long id) {
        if (threadsById == null) {
            Map<Long, ThreadInformation> threadsById = new HashMap<>();
            for (ThreadInformation thread : threads) {
                threadsById.put(thread.getId(), thread);
            }
            this.threadsById = threadsById;
        }
        return threadsById.get(id);
    }

    /**
     * Returns the thread states and their counts.
     *
     * @return a non-null instance
     */
    public Map<Thread.State, Number> getStates() {
        Map<Thread.State, MutableInt> states = new HashMap<>();
        for (ThreadInformation thread : threads) {
            MutableInt count = states.computeIfAbsent(thread.getState(), state -> new MutableInt(0));
            count.increment();
        }
        return Collections.unmodifiableMap(states);
    }


}
