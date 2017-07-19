package mil.nga.bundler;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.bundler.ejb.EJBClientUtilities;
import mil.nga.bundler.ejb.interfaces.JobMetricsCollectorI;
import mil.nga.util.HostNameUtils;

/**
 * Simple JAX-RS endpoint providing an "isAlive" function for monitoring
 * purposes.  It also provides a method to use to manually start the cleanup
 * process.
 * 
 * @author L. Craig Carpenter
 */
@Path("")
public class BundlerMetrics {

    /**
     * Set up the Log4j system for use throughout the class
     */
    static final Logger LOGGER = LoggerFactory.getLogger(BundlerMetrics.class);
    
    /**
     * The name of the application
     */
    public static final String APPLICATION_NAME = "BundlerMetrics";
    
    /**
     * Container-injected EJB reference.
     */
    JobMetricsCollectorI service;
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * 
     * @return Reference to the JobMetricsCollector EJB.
     */
    private JobMetricsCollectorI getJobMetricsCollector() {
        if (service == null) {
            
            LOGGER.warn("Application container failed to inject the "
                    + "reference to [ JobMetricsCollectorI ].  Attempting to "
                    + "look it up via JNDI.");
            service = EJBClientUtilities
                    .getInstance()
                    .getJobMetricsCollector();
        }
        return service;
    }
    
    /**
     * Simple method used to determine whether or not the 
     * application is responding to requests.
     */
    @GET
    @Path("/isAlive")
    public Response isAlive(@Context HttpHeaders headers) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Application [ ");
        sb.append(APPLICATION_NAME);
        sb.append(" ] on host [ ");
        sb.append(HostNameUtils.getHostName());
        sb.append(" ] running in JVM [ ");
        sb.append(EJBClientUtilities.getInstance().getServerName());
        sb.append(" ].");
        
        return Response.status(Status.OK).entity(sb.toString()).build();
            
    }
    
    @GET
    @Path("/startMetricsCollection")
    public Response startCleanup() {
            if (getJobMetricsCollector() != null) {
                getJobMetricsCollector().collectMetrics();
            }
            else {
                Response.status(Status.NOT_FOUND).build();
            }
        return Response.status(Status.OK).entity("Done!").build();
    }
}

