package net.microfalx.jvm.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Os implements Serializable {

    private static final long serialVersionUID = -2581339792400294491L;

    private String name;
    private String version;
}
