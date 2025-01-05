package net.microfalx.jvm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class GarbageCollection implements Serializable {

    @Serial
    private static final long serialVersionUID = -3522275180586328029L;

    private Type type;
    private long duration;
    private int count;

    @Getter
    @ToString
    @AllArgsConstructor
    public enum Type {

        EDEN(true, "Eden"),
        TENURED(false, "Tenured"),
        UNKNOWN(false, "Unknown");

        private final boolean young;
        private final String label;
    }
}
