package mil.nga.bundler.ejb.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.sql.DataSource;

import mil.nga.bundler.ejb.EJBClientUtilities;
import mil.nga.bundler.ejb.exceptions.EJBLookupException;
import mil.nga.bundler.model.Job;
import mil.nga.bundler.types.ArchiveType;
import mil.nga.bundler.types.JobStateType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session Bean implementation class JDBCJobService
 * 
 * This class re-implements the methods in the JobService EJB class as 
 * simple JDBC calls.  This class was created in order to solve some 
 * performance problems when using Hibernate in conjunction with the 
 * metrics data.
 */
@Stateless
@LocalBean
public class JDBCJobService {

    /**
     * Table used to extract the target archive information.
     */
    private static final String TABLE_NAME = "JOBS";
    
    /**
     * Set up the logging system for use throughout the class
     */        
    private static final Logger LOGGER = LoggerFactory.getLogger(
            JDBCJobService.class);
    
    /**
     * Container-injected datasource object.
     */
    @Resource(mappedName="java:jboss/datasources/JobTracker")
    DataSource datasource;
    
    /**
     * Container-injected reference to the JDBCArchiveService EJB.
     */
    @EJB
    JDBCArchiveService jdbcArchiveService;
    
    /**
     * Eclipse-generated default constructor. 
     */
    public JDBCJobService() { }

    /**
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JobService EJB.
     */
    private JDBCArchiveService getJDBCArchiveService() 
            throws EJBLookupException {
        
        if (jdbcArchiveService == null) {
            
            LOGGER.warn("Application container failed to inject the "
                    + "reference to the JDBCArchiveService EJB.  Attempting "
                    + "to look it up via JNDI.");
            jdbcArchiveService = EJBClientUtilities
                    .getInstance()
                    .getJDBCArchiveService();
            
            // If it's still null, throw an exception to prevent NPE later.
            if (jdbcArchiveService == null) {
                throw new EJBLookupException(
                        "Unable to obtain a reference to [ "
                        + JDBCArchiveService.class.getName()
                        + " ].",
                        JDBCArchiveService.class.getName());
            }
        }
        return jdbcArchiveService;
    }
    
    /**
     * Delete all information associated with the input jobID from the back-end
     * data store.
     * 
     * @param jobID The target job ID to delete.
     */
    public void delete(String jobID) {
        
        Connection        conn   = null;
        List<String>      jobIDs = new ArrayList<String>();
        PreparedStatement stmt   = null;
        ResultSet         rs     = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "delete from " + TABLE_NAME  
                + " where JOB_ID = ?";
            
        if (datasource != null) {
            if ((jobID != null) && (!jobID.isEmpty())) {
                
                try {
                    
                    // First, delete the associated archive and file objects
                    getJDBCArchiveService().deepDeleteArchive(jobID);
                
                    conn = datasource.getConnection();
                    
                    // Note: If the container Datasource has jta=true this will throw
                    // an exception.
                    conn.setAutoCommit(false);
                    
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, jobID);
                    stmt.executeUpdate();
                    
                    // Note: If the container Datasource has jta=true this will throw
                    // an exception.
                    conn.commit();
                    
                }
                catch (EJBLookupException ele) {
                    LOGGER.error("Unable to obtain a reference to [ "
                            + ele.getEJBName()
                            + " ].  Delete of job [  "
                            + jobID
                            + " ] will not be performed.");
                }
                catch (SQLException se) {
                    LOGGER.error("An unexpected SQLException was raised while "
                            + "attempting to insert delete a [ "
                            + TABLE_NAME 
                            + " ] object "
                            + "from the data store.  Error message [ "
                            + se.getMessage() 
                            + " ].");
                }
                finally {
                    try { 
                        if (stmt != null) { stmt.close(); } 
                    } catch (Exception e) {}
                    try { 
                        if (conn != null) { conn.close(); } 
                    } catch (Exception e) {}
                }
            }
            else {
                LOGGER.warn("Input JOB_ID field is null or empty.  "
                        + "Delete operation will not be performed.");
            }
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Delete of [ "
                    + TABLE_NAME
                    + " ] record for job ID [ "
                    + jobID
                    + " ] completed in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
    }
    
    /**
     * Delete all information associated with the input Job from the back-end
     * data store.
     * 
     * @param job The target job object to delete.
     */
    public void delete(Job job) {
        if (job != null) {
            delete(job.getJobID());
        }
        else {
            LOGGER.error("The input Job object is null.  Delete operation "
                    + "not be performed.");
        }
    }
    
    /**
     * Retrieve a complete list of job IDs from the data store.
     * @return A list of job IDs.
     */
    public List<String> getJobIDs() {
        
        Connection        conn   = null;
        List<String>      jobIDs = new ArrayList<String>();
        PreparedStatement stmt   = null;
        ResultSet         rs     = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "select JOB_ID from " + TABLE_NAME;
        
        if (datasource != null) {
            
            try {
                conn = datasource.getConnection();
                stmt = conn.prepareStatement(sql);
                rs   = stmt.executeQuery();
                while (rs.next()) {
                    jobIDs.add(rs.getString("JOB_ID"));
                }
            }
            catch (SQLException se) {
                LOGGER.error("An unexpected SQLException was raised while "
                        + "attempting to retrieve a list of job IDs from the "
                        + "target data source.  Error message [ "
                        + se.getMessage() 
                        + " ].");
            }
            finally {
                try { 
                    if (rs != null) { rs.close(); } 
                } catch (Exception e) {}
                try { 
                    if (stmt != null) { stmt.close(); } 
                } catch (Exception e) {}
                try { 
                    if (conn != null) { conn.close(); } 
                } catch (Exception e) {}
            }
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[ "
                    + jobIDs.size() 
                    + " ] job IDs selected in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
        
        return jobIDs;
    }
    
    /**
     * Retrieve a list of job IDs that were started prior to the input time.
     * This method was added to facilitate cleanup of data from the back-end
     * data store.
     * 
     * @return A list of job IDs.
     */
    public List<String> getJobIDs(long time) {
        
        Connection        conn   = null;
        List<String>      jobIDs = new ArrayList<String>();
        PreparedStatement stmt   = null;
        ResultSet         rs     = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "select JOB_ID from " + TABLE_NAME
                + " where START_TIME < ?";
        
        if (datasource != null) {
            
            try {
                conn = datasource.getConnection();
                stmt = conn.prepareStatement(sql);
                stmt.setLong(1, time);
                rs   = stmt.executeQuery();
                while (rs.next()) {
                    jobIDs.add(rs.getString("JOB_ID"));
                }
            }
            catch (SQLException se) {
                LOGGER.error("An unexpected SQLException was raised while "
                        + "attempting to retrieve a list of job IDs from the "
                        + "target data source.  Error message [ "
                        + se.getMessage() 
                        + " ].");
            }
            finally {
                try { 
                    if (rs != null) { rs.close(); } 
                } catch (Exception e) {}
                try { 
                    if (stmt != null) { stmt.close(); } 
                } catch (Exception e) {}
                try { 
                    if (conn != null) { conn.close(); } 
                } catch (Exception e) {}
            }
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[ "
                    + jobIDs.size() 
                    + " ] job IDs selected in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
        
        return jobIDs;
    }
    
    /**
     * This method will return a list of all 
     * <code>mil.nga.bundler.model.Job</code> objects currently persisted in 
     * the back-end data store.  The Job objects returned will not be fully 
     * loaded.  They will not contain the individual archives nor the file 
     * lists.
     * 
     * @return A list of Job objects.
     */
    public List<Job> getJobs() {
        
        Connection        conn   = null;
        List<Job>         jobs   = new ArrayList<Job>();
        PreparedStatement stmt   = null;
        ResultSet         rs     = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "select JOB_ID, ARCHIVE_SIZE, "
                + "ARCHIVE_TYPE, END_TIME, NUM_ARCHIVES, "
                + "NUM_ARCHIVES_COMPLETE, NUM_FILES, NUM_FILES_COMPLETE, "
                + "START_TIME, JOB_STATE, TOTAL_SIZE, TOTAL_SIZE_COMPLETE, "
                + "USER_NAME from "
                + TABLE_NAME
                + " order by START_TIME desc";
        
        if (datasource != null) {
            try {
                conn = datasource.getConnection();
                stmt = conn.prepareStatement(sql);
                rs   = stmt.executeQuery();
                
                while (rs.next()) {
                    
                    Job job = new Job();
                    job.setJobID(rs.getString("JOB_ID"));
                    job.setArchiveSize(rs.getLong("ARCHIVE_SIZE"));
                    job.setArchiveType(ArchiveType.valueOf(
                            rs.getString("ARCHIVE_TYPE")));
                    job.setEndTime(rs.getLong("END_TIME"));
                    job.setNumArchives(rs.getInt("NUM_ARCHIVES"));
                    job.setNumArchivesComplete(
                            rs.getInt("NUM_ARCHIVES_COMPLETE"));
                    job.setNumFiles(rs.getLong("NUM_FILES"));
                    job.setNumFilesComplete(rs.getLong("NUM_FILES_COMPLETE"));
                    job.setStartTime(rs.getLong("START_TIME"));
                    job.setState(JobStateType.valueOf(
                            rs.getString("JOB_STATE")));
                    job.setTotalSize(rs.getLong("TOTAL_SIZE"));
                    job.setTotalSizeComplete(rs.getLong("TOTAL_SIZE_COMPLETE"));
                    job.setUserName(rs.getString("USER_NAME"));
                    jobs.add(job);
                }
            }
            catch (SQLException se) {
                LOGGER.error("An unexpected SQLException was raised while "
                        + "attempting to retrieve a list of job IDs from the "
                        + "target data source.  Error message [ "
                        + se.getMessage() 
                        + " ].");
            }
            finally {
                try { rs.close(); } catch (Exception e) {}
                try { stmt.close(); } catch (Exception e) {}
                try { conn.close(); } catch (Exception e) {}
            }
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[ "
                    + jobs.size() 
                    + " ] jobs selected in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
        
        return jobs;
    }
    
    /**
     * Load the fully materialized Job from the data store.  The returned
     * Job object will contain fully populated child Archive and 
     * child FileEntry lists. 
     * 
     * @param jobID The ID of the job to retrieve.
     * @return The fully materialized job.
     */
    public Job getMaterializedJob(String jobID) {
        
        Connection        conn   = null;
        Job               job    = new Job();
        PreparedStatement stmt   = null;
        ResultSet         rs     = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "select JOB_ID, ARCHIVE_SIZE, "
                + "ARCHIVE_TYPE, END_TIME, NUM_ARCHIVES, "
                + "NUM_ARCHIVES_COMPLETE, NUM_FILES, NUM_FILES_COMPLETE, "
                + "START_TIME, JOB_STATE, TOTAL_SIZE, TOTAL_SIZE_COMPLETE, "
                + "USER_NAME from "
                + TABLE_NAME 
                + " where JOB_ID = ?";

        
        if (datasource != null) {
            if ((jobID != null) && (!jobID.isEmpty())) {
                try {
                    
                    conn = datasource.getConnection();
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, jobID);
                    rs   = stmt.executeQuery();
                    
                    if (rs.next()) {
                        
                        job.setJobID(rs.getString("JOB_ID"));
                        job.setArchiveSize(rs.getLong("ARCHIVE_SIZE"));
                        job.setArchiveType(ArchiveType.valueOf(
                                rs.getString("ARCHIVE_TYPE")));
                        job.setEndTime(rs.getLong("END_TIME"));
                        job.setNumArchives(rs.getInt("NUM_ARCHIVES"));
                        job.setNumArchivesComplete(
                                rs.getInt("NUM_ARCHIVES_COMPLETE"));
                        job.setNumFiles(rs.getLong("NUM_FILES"));
                        job.setNumFilesComplete(rs.getLong("NUM_FILES_COMPLETE"));
                        job.setStartTime(rs.getLong("START_TIME"));
                        job.setState(JobStateType.valueOf(
                                rs.getString("JOB_STATE")));
                        job.setTotalSize(rs.getLong("TOTAL_SIZE"));
                        job.setTotalSizeComplete(rs.getLong("TOTAL_SIZE_COMPLETE"));
                        job.setUserName(rs.getString("USER_NAME"));
                        
                        // Get the child archive data
                        job.setArchives(
                                getJDBCArchiveService().
                                    getMaterializedArchives(
                                        job.getJobID()));
                    }
                }
                catch (EJBLookupException ele) {
                    LOGGER.error("Unable to obtain a reference to [ "
                            + ele.getEJBName()
                            + " ].  Delete of job [  "
                            + jobID
                            + " ] will not be performed.");
                }
                catch (SQLException se) {
                    LOGGER.error("An unexpected SQLException was raised while "
                            + "attempting to retrieve a list of job IDs from the "
                            + "target data source.  Error message [ "
                            + se.getMessage() 
                            + " ].");
                }
                finally {
                    try { 
                        if (rs != null) { rs.close(); } 
                    } catch (Exception e) {}
                    try { 
                        if (stmt != null) { stmt.close(); } 
                    } catch (Exception e) {}
                    try { 
                        if (conn != null) { conn.close(); } 
                    } catch (Exception e) {}
                }
            }
            else {
                // jobid is null
            }
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty Job will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Materialized job with job ID [ "
                    + jobID 
                    + " ] selected in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
        return job;
    }
    
    /**
     * This method will return a list of all 
     * <code>mil.nga.bundler.model.Job</code> objects currently persisted in 
     * the back-end data store That fall between the input start and end time.
     * The Job objects returned will not be fully materialized.  
     * They will not contain the individual archives nor the file 
     * lists.
     * 
     * @param startTime The "from" parameter 
     * @param endTime The "to" parameter
     * @return All of the jobs with a start time that fall in between the two 
     * input parameters.
     */
    public List<Job> getJobsByDate(long startTime, long endTime) {
        
        Connection        conn   = null;
        List<Job>         jobs   = new ArrayList<Job>();
        PreparedStatement stmt   = null;
        ResultSet         rs     = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "select JOB_ID, ARCHIVE_SIZE, "
                + "ARCHIVE_TYPE, END_TIME, NUM_ARCHIVES, "
                + "NUM_ARCHIVES_COMPLETE, NUM_FILES, NUM_FILES_COMPLETE, "
                + "START_TIME, JOB_STATE, TOTAL_SIZE, TOTAL_SIZE_COMPLETE, "
                + "USER_NAME from " + TABLE_NAME + " where START_TIME > ? "
                + "and START_TIME < ? order by START_TIME desc";
        
        
        // Ensure the startTime is earlier than the endTime before submitting
        // the query to the database.
        if (startTime > endTime) {
                LOGGER.warn("The caller supplied a start time that falls "
                        + "after the end time.  Swapping start and end "
                        + "times.");
                long temp = startTime;
                startTime = endTime;
                endTime = temp;
        }
        else if (startTime == endTime) {
            LOGGER.warn("The caller supplied the same time for both start "
                    + "and end time.  This method will likely yield a null "
                    + "job list.");
        }
        
        if (datasource != null) {
            try {
                
                conn = datasource.getConnection();
                stmt = conn.prepareStatement(sql);
                stmt.setLong(1, startTime);
                stmt.setLong(2, endTime);
                rs   = stmt.executeQuery();
                
                while (rs.next()) {
                    
                    Job job = new Job();
                    job.setJobID(rs.getString("JOB_ID"));
                    job.setArchiveSize(rs.getLong("ARCHIVE_SIZE"));
                    job.setArchiveType(ArchiveType.valueOf(
                            rs.getString("ARCHIVE_TYPE")));
                    job.setEndTime(rs.getLong("END_TIME"));
                    job.setNumArchives(rs.getInt("NUM_ARCHIVES"));
                    job.setNumArchivesComplete(
                            rs.getInt("NUM_ARCHIVES_COMPLETE"));
                    job.setNumFiles(rs.getLong("NUM_FILES"));
                    job.setNumFilesComplete(rs.getLong("NUM_FILES_COMPLETE"));
                    job.setStartTime(rs.getLong("START_TIME"));
                    job.setState(JobStateType.valueOf(
                            rs.getString("JOB_STATE")));
                    job.setTotalSize(rs.getLong("TOTAL_SIZE"));
                    job.setTotalSizeComplete(rs.getLong("TOTAL_SIZE_COMPLETE"));
                    job.setUserName(rs.getString("USER_NAME"));
                    jobs.add(job);
                }
            }
            catch (SQLException se) {
                LOGGER.error("An unexpected SQLException was raised while "
                        + "attempting to retrieve a list of job IDs from the "
                        + "target data source.  Error message [ "
                        + se.getMessage() 
                        + " ].");
            }
            finally {
                try { 
                    if (rs != null) { rs.close(); } 
                } catch (Exception e) {}
                try { 
                    if (stmt != null) { stmt.close(); } 
                } catch (Exception e) {}
                try { 
                    if (conn != null) { conn.close(); } 
                } catch (Exception e) {}
            }
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[ "
                    + jobs.size() 
                    + " ] job IDs selected in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
        
        return jobs;
    }
}
