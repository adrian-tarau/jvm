package net.microfalx.jvm;

/**
 * An exception raised when a remote virtual machine is not available.
 */
public class VirtualMachineNotAvailableException extends VirtualMachineException {

    public VirtualMachineNotAvailableException(String message) {
        super(message);
    }
}
