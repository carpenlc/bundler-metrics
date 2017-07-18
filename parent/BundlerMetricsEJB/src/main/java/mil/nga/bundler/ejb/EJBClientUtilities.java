package mil.nga.bundler.ejb;

import java.lang.management.ManagementFactory;
import java.util.Properties;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class used by the Web tier to look up EJB references within
 * the container.  This class is specific to the JBoss/Wildfly application
 * containers.  This was initially developed because JBoss EAP 6.x does not 
 * support EJB injection into the Web tier.
 * 
 * @author L. Craig Carpenter
 */
public class EJBClientUtilities {

    /**
     * Set up the LogBack system for use throughout the class
     */        
    static final Logger LOGGER = LoggerFactory.getLogger(
            EJBClientUtilities.class);
    
    /**
     * Handle to the container JNDI Context.
     */
    private static Context initialContext;
    
    /**
     * The specific JNDI interface to look up.
     */
    private static final String PKG_INTERFACES = "org.jboss.ejb.client.naming";
    
    /**
     * The server MBean name used for obtaining information about the running 
     * server. 
     */
    private static final String SERVER_MBEAN_OBJECT_NAME = 
        "jboss.as:management-root=server";

    /**
     * MBean attribute that contains the JVM server name.
     */
    private static final String SERVER_NAME_ATTRIBUTE = "name";
    
    /**
     * The name of the EAR file in which the EJBs are packaged.
     */
    private static final String EAR_APPLICATION_NAME = "BundlerMetrics";
    
    /**
     * The name of the module (i.e. JAR) containing the EJBs
     */
    private static final String EJB_MODULE_NAME = "BundlerMetricsEJB";

    /**
     * Construct the JBoss appropriate JNDI lookup name for the input Class
     * object.
     * 
     * @param clazz EJB class reference we want to look up.
     * @return The JBoss appropriate JNDI lookup name.
     */
    private String getJNDIName(Class<?> clazz) {
        
        String appName = EAR_APPLICATION_NAME;
        String moduleName = EJB_MODULE_NAME;
        // String distinctName = "";
        String beanName = clazz.getSimpleName();
        String interfaceName = clazz.getName();        
        
        // The following lookup is when using a local/remote interface
        // view.
        // String name = "ejb:" 
        //        + appName + "/" 
        //        + moduleName + "/" 
        //        + distinctName + "/" 
        //        + beanName + "!" + interfaceName;
        
        // When using a no-interface view for the beans, the following is the
        // lookup.
        String name = "java:global/" 
                + appName + "/"
                + moduleName + "/"
                + beanName + "!" + interfaceName;
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Looking up [ "
                    + name
                    + " ].");
        }
        return name;
    }

    /**
     * Return the raw reference to the target EJB.
     * 
     * @param clazz The Class reference to look up.
     * @return The superclass (Object) reference to the target EJB. 
     */
    private Object getEJB(Class<?> clazz) {
        
        Object ejb  = null;
        String name = getJNDIName(clazz);
        
        try {
            Context ctx = getInitialContext();
            if (ctx != null) {
                ejb =  ctx.lookup(name);
            }
            else {
                LOGGER.error("Unable to look up the InitialContext.  See "
                        + "previous errors for more information.");
            }
        }
        catch (NamingException ne) {
            LOGGER.error("Unexpected NamingException attempting to "
                    + "look up EJB [ "
                    + name
                    + " ].  Error encountered [ "
                    + ne.getMessage()
                    + " ].");
        }
        return ejb;
    }

    /**
     * Simple method used to get the initial context used by nearly all
     * of the methods in this class.
     * 
     * @return Reference to the InitialContext.
     * @throws NamingException Thrown if there are problems encountered while
     * obtaining the InitialContext.
     */
    private Context getInitialContext() throws NamingException {
        
        if (initialContext == null) {
            Properties properties = new Properties();
            properties.put(Context.URL_PKG_PREFIXES, PKG_INTERFACES);
            properties.put("jboss.naming.client.ejb.context", true);
            initialContext = new InitialContext(properties);
        }

        return initialContext;
    }

    /**
     * Accessor method for the singleton instance of the ClientUtility class.
     * 
     * @return The singleton instance of the ClientUtility class.
     */
    public static EJBClientUtilities getInstance() {
        return EJBClientUtilitiesHolder.getSingleton();
    } 

    /**
     * Utility method used to look up the JobMetricsCollector interface.  
     * This method is only called by the web tier.
     * 
     * @return The JobMetricsCollector interface, or null if we couldn't 
     * look it up.
     */
    public JobMetricsCollector getJobMetricsCollector() {
        
        JobMetricsCollector service = null;
        Object           ejb     = getEJB(JobMetricsCollector.class);
        
        if (ejb != null) {
            if (ejb instanceof mil.nga.bundler.ejb.JobMetricsCollector) {
                service = (JobMetricsCollector)ejb;
            }
            else {
                LOGGER.error("Unable to look up EJB [ "
                        + getJNDIName(JobMetricsCollector.class)
                        + " ] returned reference was the wrong type.  "
                        + "Type returned [ "
                        + ejb.getClass().getCanonicalName()
                        + " ].");
            }
        }
        else {
            LOGGER.error("Unable to look up EJB [ "
                    + getJNDIName(JobMetricsCollector.class)
                    + " ] returned reference was null.");
        }
        return service;
    }    

    /**
     * Method using the JMX MBean interface to retrieve the name of the current
     * JVM (i.e. server name).
     * 
     * @return The name of the container server instance.
     */
    public String getServerName() {
        
        String serverName = "";
        
        try {
            
            ObjectName serverMBeanName = new ObjectName(
                    SERVER_MBEAN_OBJECT_NAME);
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            serverName = (String)server.getAttribute(
                    serverMBeanName, 
                    SERVER_NAME_ATTRIBUTE);
        
        }
        catch (AttributeNotFoundException anfe) {
            LOGGER.error("Unexpected AttributeNotFoundException while "
                    + "attempting to obtain the server name from the "
                    + "application container.  Error message => "
                    + anfe.getMessage());
        }
        catch (MBeanException mbe) {
            LOGGER.error("Unexpected MBeanException while "
                    + "attempting to obtain the server name from the "
                    + "application container.  Error message => "
                    + mbe.getMessage());
        }
        catch (MalformedObjectNameException mone) {
            LOGGER.error("Unexpected MalformedObjectNameException while "
                    + "attempting to obtain the server name from the "
                    + "application container.  Error message => "
                    + mone.getMessage());
        }
        catch (InstanceNotFoundException infe) {
            LOGGER.error("Unexpected AttributeNotFoundException while "
                    + "attempting to obtain the server name from the "
                    + "application container.  Error message => "
                    + infe.getMessage());
        }
        catch (ReflectionException re) {
            LOGGER.error("Unexpected ReflectionException while "
                    + "attempting to obtain the server name from the "
                    + "application container.  Error message => "
                    + re.getMessage());
        }
        return serverName;
    }

    /**
     * Static inner class used to construct the Singleton object.  This class
     * exploits the fact that classes are not loaded until they are referenced
     * therefore enforcing thread safety without the performance hit imposed
     * by the <code>synchronized</code> keyword.
     * 
     * @author L. Craig Carpenter
     */
    public static class EJBClientUtilitiesHolder {
        
        /**
         * Reference to the Singleton instance of the ClientUtility
         */
        private static EJBClientUtilities _instance = new EJBClientUtilities();
    
        /**
         * Accessor method for the singleton instance of the ClientUtility.
         *
         * @return The Singleton instance of the client utility.
         */
        public static EJBClientUtilities getSingleton() {
            return _instance;
        }
        
    }
}
