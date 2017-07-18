package mil.nga.bundler.ejb;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class JobMetricsCollector {

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
     * This method will determine which jobs need to have a metrics record 
     * created. 
     * 
     * @return A list of jobs that need a metrics record calculated.
     */
    private List<String> getJobs() {
        List<String> currentJobs = jobService.getJobIDs();
        List<String> currentMetrics = metricsService.getJobIDs();
        if ((currentJobs != null) && (!currentMetrics.isEmpty())) {
            if ((currentMetrics != null) && (!currentMetrics.isEmpty())) {
                currentJobs.removeAll(currentMetrics);
            }
            else {
                LOGGER.warn("Unable to select list of current metrics.  "
                        + "The return list is null.");
            }
        }
        else {
            LOGGER.warn("Unable to select list of current jobs.  "
                    + "The return list is null.");
        }
        return currentJobs;
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
        List<String> sourceList = getJobs();
        if ((sourceList != null) && (!sourceList.isEmpty())) {
            for (String jobID : sourceList) {
                Job job = jobService.getMaterializedJob(jobID);
                if ((job.getState() == JobStateType.COMPLETE) ||
                    (job.getState() == JobStateType.ERROR)) {
                    BundlerJobMetrics metrics = getJobMetrics(job);
                    metricsService.insert(metrics);
                }
            }
        }
        else {
            LOGGER.info("There are no jobs requiring metrics collection.");
        }
    }
}
