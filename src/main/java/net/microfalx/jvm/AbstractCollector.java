package net.microfalx.jvm;

/**
 * Base class for all collectors.
 *
 * @param <M> the collected metrics
 */
public abstract class AbstractCollector<M> {

    private boolean metadata;

    public boolean isMetadata() {
        return metadata;
    }

    public AbstractCollector<M> setMetadata(boolean metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Executes the collection.
     *
     * @return a non-null instance
     */
    public abstract M execute();
}
