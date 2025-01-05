package net.microfalx.jvm;

import net.microfalx.lang.*;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.time.temporal.Temporal;
import java.util.Collection;

public class VirtualMachineMBeanServer implements Releasable, Timestampable {

    private final InetSocketAddress address;
    private MBeanServerConnection connection;
    private JMXConnector connector;

    private final long created = System.currentTimeMillis();

    /**
     * Creates an MBean Server connected to the local virtual machine.
     *
     * @return a non-null instance
     */
    public static VirtualMachineMBeanServer local() {
        return new VirtualMachineMBeanServer(null);
    }

    /**
     * Creates an MBean Server connected to a remove virtual machine.
     *
     * @return a non-null instance
     */
    public static VirtualMachineMBeanServer remote(InetSocketAddress address) {
        ArgumentUtils.requireNonNull(address);
        return new VirtualMachineMBeanServer(address);
    }

    private VirtualMachineMBeanServer(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public Temporal getCreatedAt() {
        return TimeUtils.fromMillis(created);
    }

    /**
     * Returns the address of this MBean server.
     *
     * @return a non-null instance
     */
    public InetSocketAddress getAddress() {
        if (isLocal()) throw new UnsupportedOperationException("Address not available for local server");
        return address;
    }

    /**
     * Returns whether the MBean Server is available.
     *
     * @return {@code true} if available, {@code false} otherwise
     */
    public boolean isAvailable() {
        try {
            RuntimeMXBean platformMXBean = getPlatformMXBean(RuntimeMXBean.class);
            platformMXBean.getStartTime();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns whether the server is local.
     *
     * @return {@code true} if local, {@code false} otherwise
     */
    public boolean isLocal() {
        return address == null;
    }

    @Override
    public void release() {
        if (connector != null) IOUtils.closeQuietly(connector);
    }

    /**
     * Returns a platform MBean.
     *
     * @param clazz the MBean class
     * @param <T>   the MBean type
     * @return a non-null instance
     */
    public <T extends PlatformManagedObject> T getPlatformMXBean(Class<T> clazz) {
        if (isLocal()) {
            return ManagementFactory.getPlatformMXBean(clazz);
        } else {
            try {
                return ManagementFactory.getPlatformMXBean(getConnection(), clazz);
            } catch (IOException e) {
                throw new VirtualMachineNotAvailableException("MBean '" + ClassUtils.getName(clazz) + " could not be retrieved for virtual machine " + address);
            }
        }
    }

    /**
     * Returns a collection of platform MBeans.
     *
     * @param clazz the MBean class
     * @param <T>   the MBean type
     * @return a non-null instance
     */
    public <T extends PlatformManagedObject> Collection<T> getPlatformMXBeans(Class<T> clazz) {
        if (isLocal()) {
            return ManagementFactory.getPlatformMXBeans(clazz);
        } else {
            try {
                return ManagementFactory.getPlatformMXBeans(getConnection(), clazz);
            } catch (IOException e) {
                throw new VirtualMachineNotAvailableException("MBean '" + ClassUtils.getName(clazz) + " could not be retrieved for virtual machine " + address);
            }
        }
    }

    /**
     * Returns a long attribute.
     *
     * @param name         the object bean name
     * @param attrName     the attribute name
     * @param defaultValue the default value
     * @return the value.
     */
    public Long getLongAttr(String name, String attrName, Long defaultValue) {
        try {
            ObjectName oName = new ObjectName(name);
            Object value;
            if (isLocal()) {
                value = ManagementFactory.getPlatformMBeanServer().getAttribute(oName, attrName);
            } else {
                value = getConnection().getAttribute(oName, attrName);
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Returns an integer attribute.
     *
     * @param name         the object bean name
     * @param attrName     the attribute name
     * @param defaultValue the default value
     * @return the value.
     */
    public Integer getIntAttr(String name, String attrName, Integer defaultValue) {
        try {
            ObjectName oName = new ObjectName(name);
            Object value;
            if (isLocal()) {
                value = ManagementFactory.getPlatformMBeanServer().getAttribute(oName, attrName);
            } else {
                value = getConnection().getAttribute(oName, attrName);
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Returns an float attribute.
     *
     * @param name         the object bean name
     * @param attrName     the attribute name
     * @param defaultValue the default value
     * @return the value.
     */
    public Float getFloatAttr(String name, String attrName, Float defaultValue) {
        try {
            ObjectName oName = new ObjectName(name);
            Object value;
            if (isLocal()) {
                value = ManagementFactory.getPlatformMBeanServer().getAttribute(oName, attrName);
            } else {
                value = getConnection().getAttribute(oName, attrName);
            }
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Returns a String attribute.
     *
     * @param name         the object bean name
     * @param attrName     the attribute name
     * @param defaultValue the default value
     * @return the value.
     */
    public String getStringAttr(String name, String attrName, String defaultValue) {
        try {
            ObjectName oName = new ObjectName(name);
            Object value;
            if (isLocal()) {
                value = ManagementFactory.getPlatformMBeanServer().getAttribute(oName, attrName);
            } else {
                value = getConnection().getAttribute(oName, attrName);
            }
            if (value instanceof String) {
                return (String) value;
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Returns (creates if it does not exist) a connection to a remote MBean Server.
     *
     * @return a non-null instance
     */
    private MBeanServerConnection getConnection() {
        if (connection != null) return connection;
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + address.getHostName() + ":" + address.getPort() + "/jmxrmi");
            connector = JMXConnectorFactory.connect(url);
            connector.connect();
            connection = connector.getMBeanServerConnection();
            return connection;
        } catch (Exception e) {
            throw new VirtualMachineNotAvailableException("A virtual machine is not available at " + address);
        }
    }
}
