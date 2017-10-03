package mil.nga.bundler;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import mil.nga.bundler.model.Job;
import mil.nga.bundler.ejb.EJBClientUtilities;
import mil.nga.bundler.ejb.exceptions.EJBLookupException;
import mil.nga.bundler.ejb.interfaces.JobMetricsCollectorI;
import mil.nga.bundler.ejb.jdbc.JDBCJobService;
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
    @EJB
    JobMetricsCollectorI service;
    
    /**
     * Container-injected EJB reference
     */
    @EJB
    JDBCJobService jobService;
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * 
     * @return Reference to the JobMetricsCollector EJB.
     */
    private JobMetricsCollectorI getJobMetricsCollector() 
            throws EJBLookupException {
        if (service == null) {
            LOGGER.warn("Application container failed to inject the "
                    + "reference to [ "
                    + JobMetricsCollectorI.class.getName()
                    + " ].  Attempting to "
                    + "look it up via JNDI.");
            service = EJBClientUtilities
                    .getInstance()
                    .getJobMetricsCollector();
        }
        return service;
    }
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * 
     * @return Reference to the JDBCJobService EJB.
     */
    private JDBCJobService getJobService() 
            throws EJBLookupException {
        if (jobService == null) {
            LOGGER.warn("Application container failed to inject the "
                    + "reference to [ "
                    + JDBCJobService.class.getName()
                    + " ].  Attempting to "
                    + "look it up via JNDI.");
            jobService = EJBClientUtilities
                    .getInstance()
                    .getJDBCJobService();
        }
        return jobService;
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
    
    /**
     * Simple method allowing clients to manually start the metrics 
     * collection process from a browser.
     */
    @GET
    @Path("/startMetricsCollection")
    public Response startCleanup() {
        try {
            getJobMetricsCollector().collectMetrics();
        }
        catch (EJBLookupException ele) {
            LOGGER.error("Unexpected EJBLookupException raised while "
                    + "attempting to look up EJB [ "
                    + ele.getEJBName()
                    + " ].");
            Response.status(Status.NOT_FOUND).build();
        }
        return Response.status(Status.OK).entity("Done!").build();
    }
    
    /**
     * 
     */
    @GET
    @Path("/getJobDetails")
    @Produces("application/xml")
    public Response getJobDetails(@QueryParam("job_id") String jobID) {
        
        StringBuilder sb  = new StringBuilder();
        
        try {
            if ((jobID != null) && (!jobID.isEmpty())) {
                
                LOGGER.info("Retrieving job associated with job_id [ "
                        + jobID
                        + " ].");
                
                Job job = this.getJobService().getMaterializedJob(jobID);
                if (job != null) {
                    
                    try {
                        XmlMapper mapper = new XmlMapper();
                    
                        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
                        sb.append(mapper.writeValueAsString(job));
                    }
                    catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
                        sb.append("JsonProcessingException raised.  Error => [ "
                                + jpe.getMessage()
                                + " ].");
                    }
                }
                else {
                    sb.append("Unable to find job matching job_id [ "
                            + jobID
                            + " ].");
                }
            }
            else {
                sb.append("Input job_id is null or undefined.");
            }
        }
        catch (EJBLookupException ele) {
            LOGGER.error("Unexpected EJBLookupException raised while "
                    + "attempting to look up EJB [ "
                    + ele.getEJBName()
                    + " ].");
            Response.status(Status.NOT_FOUND).build();
        }
        return Response.status(Status.OK).entity(sb.toString()).build();
    }
}

