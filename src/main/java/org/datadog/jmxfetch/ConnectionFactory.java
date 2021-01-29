package org.datadog.jmxfetch;

import static org.datadog.jmxfetch.Instance.isDirectInstance;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

/** Singleton used to create connections to the MBeanServer. */
@Slf4j
public class ConnectionFactory {
    public static final String PROCESS_NAME_REGEX = "process_name_regex";
    public static final String PID = "pid";

    /** Factory method to create connections, both remote and local to the target JVM. */
    public static Connection createConnection(Map<String, Object> connectionParams)
            throws IOException {
        // This is used by dd-java-agent to enable directly connecting to the mbean server.
        // This works since jmxfetch is being run as a library inside the process being monitored.
        if (isDirectInstance(connectionParams)) {
            log.info("Connecting to JMX directly on the JVM");
            return new JvmDirectConnection();
        }

        if (connectionParams.get(PROCESS_NAME_REGEX) != null) {
            try {
                Class.forName("com.sun.tools.attach.AttachNotSupportedException");
            } catch (ClassNotFoundException e) {
                throw new IOException(
                        "Unable to find tools.jar."
                                + " Are you using a JDK and did you set the path to tools.jar ?");
            }
            log.info("Connecting using Attach API");
            return new AttachApiConnection(connectionParams);
        }

        if(connectionParams.get(PID) != null) {
            try {
                Class.forName("com.sun.tools.attach.AttachNotSupportedException");
            } catch (ClassNotFoundException e) {
                throw new IOException(
                        "Unable to find tools.jar."
                                + " Are you using a JDK and did you set the path to tools.jar ?");
            }
            log.info("Connecting using Attach PID");
            return new AttachPidConnection(connectionParams);
        }

        log.info("Connecting using JMX Remote");
        return new RemoteConnection(connectionParams);
    }
}
