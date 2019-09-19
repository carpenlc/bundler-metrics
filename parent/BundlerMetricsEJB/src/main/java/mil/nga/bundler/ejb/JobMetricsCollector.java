package mil.nga.bundler.ejb;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.bundler.ejb.exceptions.EJBLookupException;
import mil.nga.bundler.ejb.interfaces.JobMetricsCollectorI;
import mil.nga.bundler.ejb.jdbc.JDBCJobMetricsService;
import mil.nga.bundler.ejb.jdbc.JDBCJobService;
import mil.nga.bundler.model.Archive;
import mil.nga.bundler.model.BundlerJobMetrics;
import mil.nga.bundler.model.Job;
import mil.nga.bundler.types.JobStateType;

/**
 * Session Bean implementation class JobMetricsCollector
 */
@Stateless
@LocalBean
public class JobMetricsCollector implements JobMetricsCollectorI {

    /**
     * Set up the logging system for use throughout the class
     */        
    private static final Logger LOGGER = LoggerFactory.getLogger(
            JobMetricsCollector.class);
    
    /**
     * Container-injected reference to the JDBCJobMetricsService session bean.
     */
    @EJB
    JDBCJobMetricsService metricsService;
    
    /**
     * Container-injected reference to the JDBCJobService session bean.
     */
    @EJB
    JDBCJobService jobService;
    
    /**
     * Eclipse-generated default constructor. 
     */
    public JobMetricsCollector() { }

    /**
     * Calculate the compression percentage.
     * 
     * @param totalSize The total size of the job.
     * @param compressedSize The compressed size of the job.
     * @return The amount of compression achieved.
     */
    private double getCompressionPercentage(
            long totalSize, 
            long compressedSize) {
        double ratio = 0.0;
        if (totalSize > 0) {
            if (compressedSize > 0) {
                ratio = (double)(totalSize - compressedSize) /
                        (double)totalSize;
            }
        }
        return ratio;
    }
    
    /**
     * Calculate the amount of time in milliseconds that the target Job 
     * object required to complete.
     * 
     * @param job The target Job.
     * @return The elapsed time (in milliseconds)
     */
    private long getElapsedTime(Job job) {
        long elapsedTime = 0;
        if (job != null) {
            elapsedTime = job.getEndTime() - job.getStartTime();
        }
        return elapsedTime;
    }
    
    /**
     * Build the metrics for a target job.
     * 
     * @param job The target job.
     * @return Metrics collected for the target job.
     */
    private BundlerJobMetrics getJobMetrics(Job job) {
        return new BundlerJobMetrics.BundlerJobMetricsBuilder()
                .archiveSize(job.getArchiveSize())
                .archiveType(job.getArchiveType())
                .compressionPercentage(getCompressionPercentage(
                        job.getTotalSize(), 
                        getTotalCompressedSize(job)))
                .elapsedTime(getElapsedTime(job))
                .jobID(job.getJobID())
                .jobState(job.getState())
                .numArchives(job.getNumArchives())
                .numArchivesComplete(job.getNumArchivesComplete())
                .numFiles(job.getNumFiles())
                .numFilesComplete(job.getNumFilesComplete())
                .startTime(job.getStartTime())
                .totalCompressedSize(getTotalCompressedSize(job))
                .totalSize(job.getTotalSize())
                .userName(job.getUserName())
                .build();
    }
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JobMetricsCollectorI interface, null if the 
     * interface could not be looked up.
     */
    private JDBCJobService getJDBCJobService() 
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
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JobMetricsCollectorI interface, null if the 
     * interface could not be looked up.
     */
    private JDBCJobMetricsService getJDBCJobMetricsService() 
            throws EJBLookupException {
        
        if (metricsService == null) {
            LOGGER.warn("Application container failed to inject the "
                    + "reference to [ "
                    + JDBCJobMetricsService.class.getName()
                    + " ].  Attempting to "
                    + "look it up via JNDI.");
            
            metricsService = EJBClientUtilities
                    .getInstance()
                    .getJDBCJobMetricsService();
        }
        return metricsService;
    }
    
    /**
     * This method will determine which jobs need to have a metrics record 
     * created. 
     * 
     * @return A list of jobs that need a metrics record calculated.
     */
    private List<String> getJobs() throws EJBLookupException {
        
    	// The number of jobs in the current JOBS table is always going 
    	// to be (far) fewer than the total number of jobs in the metrics
    	// table.  Rather than select all jobs from the metrics and use
    	// the collections class to get the disjoint set, just see if 
    	// the job ID exists in the target
    	if (LOGGER.isDebugEnabled()) {
    		LOGGER.debug("Determining jobs that need a metrics record created.");
    	}
        List<String> currentJobs = getJDBCJobService().getJobIDs();
        List<String> jobs        = new ArrayList<String>();
        
        if ((currentJobs != null) && (!currentJobs.isEmpty())) {
        	for (String jobID : currentJobs) {
	        	if (!getJDBCJobMetricsService().jobIDExists(jobID)) {
	        		jobs.add(jobID);
	        	}
        	}
        }
        else {
            LOGGER.warn("Unable to select list of current jobs.  "
                    + "The return list is null.");
        }
        return jobs;
    }
    
    /**
     * Calculate the total compressed size of the job.  This is calculated 
     * using the summation of the compressed size for each individual archive.
     * 
     * @param job The target job.
     * @return The summation of the compressed sizes for each individual 
     * archive.
     */
    private long getTotalCompressedSize(Job job) {
        long compressedSize = 0L;
        if (job != null) {
            if ((job.getArchives() != null) && 
                    (job.getArchives().size() > 0)) {
                for (Archive archive : job.getArchives()) {
                    compressedSize += archive.getSize();
                }
            }
        }
        return compressedSize;
    }
    
    /**
     * Public entry point starting the metrics collection process.
     */
    public void collectMetrics() {
        
        int  counter   = 0;
        long startTime = System.currentTimeMillis();
        LOGGER.info("Starting bundler metrics collection...");
        try {
            
            List<String> sourceList = getJobs();
            if ((sourceList != null) && (!sourceList.isEmpty())) {
                
                LOGGER.info("Processing [ "
                        + sourceList.size()
                        + " ] jobs.");
                
                for (String jobID : sourceList) {
                    
                	LOGGER.info("Processing job ID [ " 
                    		+ jobID
                    		+ " ].");
                    
                    Job job = jobService.getMaterializedJob(jobID);
                    
                    LOGGER.info("Job retrieved in [ "
                    		+ (System.currentTimeMillis() - startTime)
                    		+ " ].");
                    if (job != null) {
                        
                    	LOGGER.info("Job state [ "
                    			+ job.getState().toString()
                    			+ " ].");
                        // Ensure that the job is in a "completed" state.
                        if ((job.getState() == JobStateType.COMPLETE) ||
                            (job.getState() == JobStateType.ERROR) || 
                            (job.getState() == JobStateType.INVALID_REQUEST)) {
                            
                            BundlerJobMetrics metrics = getJobMetrics(job);
                            //if (LOGGER.isDebugEnabled()) {
                                LOGGER.info("Inserting metrics record => [ " 
                                		+ metrics.toString()
                                		);
                            //}
                            
                            metricsService.insert(metrics);
                            counter++;
                            
                        }
                    }
                    else {
                        LOGGER.warn("Unable to find a job matching job ID [ "
                                + jobID
                                + " ].  Job object returned was null.");
                    }
                }
            }
            else {
                LOGGER.info("There are no jobs requiring metrics collection.");
            }
        }
        catch (EJBLookupException ele) {
            LOGGER.error("Unable to obtain a reference to [ "
                    + ele.getEJBName()
                    + " ].  Metrics collection operation will not be "
                    + "performed.");
        }
        
        LOGGER.info("Metrics collection completed in [ "
                + (System.currentTimeMillis() - startTime)
                + " ] ms and processed [ "
                + counter
                + " ] jobs.");
    }
}
