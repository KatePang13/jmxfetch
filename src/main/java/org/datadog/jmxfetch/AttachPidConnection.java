package org.datadog.jmxfetch;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.management.remote.JMXServiceURL;
import com.sun.tools.attach.VirtualMachine;

@Slf4j
public class AttachPidConnection extends Connection {
    private static final String CONNECTOR_ADDRESS =
            "com.sun.management.jmxremote.localConnectorAddress";
    private String pid;

    /** AttachPidConnection constructor for specified connection parameters. */
    public AttachPidConnection(Map<String, Object> connectionParams) throws IOException {
        pid = connectionParams.get("pid").toString();
        this.env = new HashMap<String, Object>();
        this.address = getAddress(connectionParams);
        createConnection();
    }

    private JMXServiceURL getAddress(Map<String, Object> connectionParams)
            throws IOException {
        JMXServiceURL address;
        try {
            address = new JMXServiceURL(getJmxUrlForPid(pid));
        } catch (com.sun.tools.attach.AttachNotSupportedException e) {
            throw new IOException("Unable to attach to pid:  " + pid, e);
        }
        return address;
    }

    private String getJmxUrlForPid(String pid)
            throws com.sun.tools.attach.AttachNotSupportedException, IOException {
        log.info("VirtualMachine.attach start" );
        VirtualMachine vm = VirtualMachine.attach(pid);
        if( vm == null ) {
            log.info("VirtualMachine.attach: null " );
        }
        return getJmxUriFromVirtualMachine(vm);
    }

    private static String getJmxUriFromVirtualMachine(VirtualMachine vm) {
        log.info("local management agent in JVM with ID: " + vm);
        String connectorAddress = null;
        try {
            // Get the local JMX connector URI
            connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
        } catch (IOException e) {
            throw new IllegalStateException("IOException when fetching JMX URI from JVM with ID: " + vm.id(), e);
        }
        // If there is no local JMX connector URI, we need to launch the
        // JMX agent via this VirtualMachine attachment.
        if (connectorAddress == null) {
            log.info("Starting local management agent in JVM with ID: " + vm.id());
            try {
                vm.startLocalManagementAgent();
            } catch (IOException e) {
                throw new IllegalStateException("IOException when starting local JMX management agent in JVM with ID: " + vm.id(), e);
            }
            // Agent is started, get the connector address
            try {
                connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
            } catch (IOException e) {
                throw new IllegalStateException("IOException when fetching JMX URI from JVM with ID: " + vm.id(), e);
            }
        }
        log.info("connectorAddress:" + connectorAddress);
        return connectorAddress;
    }

    private void loadJmxAgent(com.sun.tools.attach.VirtualMachine vm) throws IOException {
        String agent =
                vm.getSystemProperties().getProperty("java.home")
                        + File.separator
                        + "lib"
                        + File.separator
                        + "management-agent.jar";
        try {
            vm.loadAgent(agent);
        } catch (Exception e) {
            log.warn("Error initializing JMX agent", e);
        }
    }
}
