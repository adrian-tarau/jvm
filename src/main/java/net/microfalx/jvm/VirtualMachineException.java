package net.microfalx.jvm;

/**
 * Base class for all exceptions related to JVM.
 */
public class VirtualMachineException extends RuntimeException {

    public VirtualMachineException(String message) {
        super(message);
    }

    public VirtualMachineException(String message, Throwable cause) {
        super(message, cause);
    }
}
