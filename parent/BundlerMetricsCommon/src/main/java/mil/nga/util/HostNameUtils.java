package mil.nga.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Class containing static utility methods and variable definitions used in 
 * conjunction with retrieving a given servers host name.
 *
 * @author L. Craig Carpenter
 */
public class HostNameUtils {
    
    /**
     * Environment variable containing the host name on Linux/Unix systems.
     */
    public static final String LINUX_HOSTNAME_ENV = "HOSTNAME";

    /**
     * Environment variable containing the host name on Windows systems.
     */
    public static final String WINDOWS_HOSTNAME_ENV = "COMPUTERNAME";

    /**
     * String returned if we could not obtain the host name.
     */
    public static final String DEFAULT_HOSTNAME = "unavailable"; 

    /**
     * Get the host name of the current server.
     *  
     * InetAddress.getLocalHost().getHostName() does a DNS query for
     * the local IP address.  The returned value is the first PTR record.  The 
     * problem is that if you have multiple PTR records, the first one returned
     * need not be the same every time.  This turned out to be a problem on the 
     * classified networks in that nearly every time this method was called, it
     * received a different host name.  Method was restructured to first use 
     * the value of the HOSTNAME environment variable, and then if that doesn't
     * work, then use the DNS lookup results.
     * 
     * @return The host name.
     */
    public static String getHostName() {
        
        String host = null;
        
        // This environment variable is for linux/unix
        host = System.getenv(LINUX_HOSTNAME_ENV);
        if ((host == null) || (host.isEmpty())) { 

            // If we're running on Windows the following environment 
            // variable will be set
            host = System.getenv(WINDOWS_HOSTNAME_ENV);
            if ((host == null) || (host.isEmpty())) {

                // Finally, try the portable method.  Know that results may be
                // questionable.
                try {
                    host = InetAddress.getLocalHost().getHostName();
                } 
                catch (UnknownHostException uhe) { 
                    System.err.println("UknownHostException encountered while "
                        + "attempting to determine the host name.  Error "
                        + "message [ "
                        + uhe.getMessage()
                        + " ].");
                }
            }
        }

        // If it's still empty just set it to "unavailable"
        if ((host == null) || (host.isEmpty())) { 
            host = DEFAULT_HOSTNAME;
        }
        return host;
    }

}

