package mil.nga.bundler.ejb.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.bundler.model.BundlerJobMetrics;
import mil.nga.bundler.types.ArchiveType;
import mil.nga.bundler.types.JobStateType;

/**
 * Session bean providing methods for interfacing with the table containing
 * the bundler job metrics information.
 * 
 * This class is written assuming that the injected DataSource object is not
 * handling the transactions on behalf of the application (i.e. non-JTA).  If 
 * this bean is deployed to a container with JTA enabled, the insert and 
 * update functions will throw exceptions when attempting to manage the 
 * underlying transaction.
 */
@Stateless
@LocalBean
public class JDBCJobMetricsService {

    /**
     * The target table name.
     */
    public static final String TABLE_NAME = "BUNDLER_JOB_METRICS";
    
    /**
     * Set up the logging system for use throughout the class
     */        
    private static final Logger LOGGER = LoggerFactory.getLogger(
            JDBCJobMetricsService.class);
    
    /**
     * Container-injected datasource object.  
     */
    @Resource(mappedName="java:jboss/datasources/JobTracker")
    DataSource datasource;
    
    /**
     * Default constructor. 
     */
    public JDBCJobMetricsService() { }

    /**
     * Retrieve a complete list of job IDs from the data store.
     * 
     * @return A list of job IDs.
     */
    public List<String> getJobIDs() {
        
        Connection        conn   = null;
        List<String>      jobIDs = new ArrayList<String>();
        PreparedStatement stmt   = null;
        ResultSet         rs      = null;
        
        long start = System.currentTimeMillis();
        String sql = "select JOB_ID from " + TABLE_NAME;
        
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
                        + "attempting to retrieve a list of job IDs from "
                        + "table [ "
                        + TABLE_NAME
                        + " ].  Error message [ "
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
                    + " ] job IDs selected from table [ "
                    + TABLE_NAME
                    + " ] in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
        return jobIDs;
    }
    /**
     * This method will return a list of all 
     * <code>mil.nga.bundler.model.BundlerJobMetrics</code> objects currently 
     * persisted in the back-end data store That fall between the input start 
     * and end time.
     * 
     * @param startTime The "from" parameter 
     * @param endTime The "to" parameter
     * @return All of the job metrics records that with a start time that fall 
     * in between the two input parameters.
     */
    public BundlerJobMetrics getJobMetrics(String jobID) {
        
        Connection        conn    = null;
        BundlerJobMetrics metrics = null;
        PreparedStatement stmt    = null;
        ResultSet         rs      = null;
        long              start   = System.currentTimeMillis();
        String             sql    = "select ARCHIVE_SIZE, ARCHIVE_TYPE, "
                + "ELAPSED_TIME, JOB_ID, JOB_STATE, NUM_ARCHIVES, "
                + "NUM_ARCHIVES_COMPLETE, NUM_FILES, NUM_FILES_COMPLETE, "
                + "START_TIME, TOTAL_COMPRESSED_SIZE, TOTAL_SIZE, USER_NAME "
                + " from " 
                + TABLE_NAME
                + "where JOB_ID =  ? ";
        
        if (datasource != null) {
            try {
                
                conn = datasource.getConnection();
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, jobID);
                rs   = stmt.executeQuery();
                
                if (rs.next()) {
                    
                    metrics = new BundlerJobMetrics.BundlerJobMetricsBuilder()
                            .archiveSize(rs.getLong("ARCHIVE_SIZE"))
                            .archiveType(ArchiveType.valueOf(
                                    rs.getString("ARCHIVE_TYPE")))
                            .elapsedTime(rs.getLong("ELAPSED_TIME"))
                            .jobID(rs.getString("JOB_ID"))
                            .jobState(JobStateType.valueOf(
                                    rs.getString("JOB_STATE")))
                            .numArchives(rs.getInt("NUM_ARCHIVES"))
                            .numArchivesComplete(rs.getInt("NUM_ARCHIVES_COMPLETE"))
                            .numFiles(rs.getLong("NUM_FILES"))
                            .numFilesComplete(rs.getLong("NUM_FILES_COMPLETE"))
                            .startTime(rs.getLong("START_TIME"))
                            .totalSize(rs.getLong("TOTAL_SIZE"))
                            .totalCompressedSize(rs.getLong("TOTAL_COMPRESSED_SIZE"))
                            .userName(rs.getString("USER_NAME"))
                            .build();
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
            LOGGER.debug("job ID [ "
                    + jobID
                    + " ] selected in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
        
        return metrics;
    }
    
    /**
     * This method will return a list of all 
     * <code>mil.nga.bundler.model.BundlerJobMetrics</code> objects currently 
     * persisted in the back-end data store That fall between the input start 
     * and end time.
     * 
     * @param startTime The "from" parameter 
     * @param endTime The "to" parameter
     * @return All of the job metrics records that with a start time that fall 
     * in between the two input parameters.
     */
    public List<BundlerJobMetrics> getJobMetricsByDate(
            long startTime, 
            long endTime) {
        
        Connection              conn    = null;
        List<BundlerJobMetrics> metrics = new ArrayList<BundlerJobMetrics>();
        PreparedStatement       stmt    = null;
        ResultSet               rs      = null;
        long                    start   = System.currentTimeMillis();
        String                  sql    = "select ARCHIVE_SIZE, ARCHIVE_TYPE, "
                + "ELAPSED_TIME, JOB_ID, JOB_STATE, NUM_ARCHIVES, "
                + "NUM_ARCHIVES_COMPLETE, NUM_FILES, NUM_FILES_COMPLETE, "
                + "START_TIME, TOTAL_COMPRESSED_SIZE, TOTAL_SIZE, USER_NAME "
                + " from " 
                + TABLE_NAME
                + "where START_TIME > ? "
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
                    
                    BundlerJobMetrics job = 
                            new BundlerJobMetrics.BundlerJobMetricsBuilder()
                            .archiveSize(rs.getLong("ARCHIVE_SIZE"))
                            .archiveType(ArchiveType.valueOf(
                                    rs.getString("ARCHIVE_TYPE")))
                            .elapsedTime(rs.getLong("ELAPSED_TIME"))
                            .jobID(rs.getString("JOB_ID"))
                            .jobState(JobStateType.valueOf(
                                    rs.getString("JOB_STATE")))
                            .numArchives(rs.getInt("NUM_ARCHIVES"))
                            .numArchivesComplete(rs.getInt("NUM_ARCHIVES_COMPLETE"))
                            .numFiles(rs.getLong("NUM_FILES"))
                            .numFilesComplete(rs.getLong("NUM_FILES_COMPLETE"))
                            .startTime(rs.getLong("START_TIME"))
                            .totalSize(rs.getLong("TOTAL_SIZE"))
                            .totalCompressedSize(rs.getLong("TOTAL_COMPRESSED_SIZE"))
                            .userName(rs.getString("USER_NAME"))
                            .build();
                    metrics.add(job);
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
                    + metrics.size() 
                    + " ] job IDs selected in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
        
        return metrics;
    }
    
    /**
     * Insert a single job metrics record into the target data source.
     * 
     * @param metrics Single job metrics record.
     */
    public void insert(BundlerJobMetrics metrics) {
        Connection        conn   = null;
        PreparedStatement stmt   = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "insert into "
                + TABLE_NAME 
                + " (ARCHIVE_SIZE, ARCHIVE_TYPE, "
                + "ELAPSED_TIME, JOB_ID, JOB_STATE, NUM_ARCHIVES, "
                + "NUM_ARCHIVES_COMPLETE, NUM_FILES, NUM_FILES_COMPLETE, "
                + "START_TIME, TOTAL_COMPRESSED_SIZE, TOTAL_SIZE, USER_NAME) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        
        if (datasource != null) {
            if (metrics != null) {
                
                try { 
                    
                    conn = datasource.getConnection();
                    
                    // Note: If the container Datasource has jta=true this will throw
                    // an exception.
                    conn.setAutoCommit(false);
                    
                    stmt = conn.prepareStatement(sql);
                    stmt.setLong(   1,  metrics.getArchiveSize());
                    stmt.setString( 2,  metrics.getArchiveType().getText());
                    stmt.setLong(   3,  metrics.getElapsedTime());
                    stmt.setString( 4,  metrics.getJobID());
                    stmt.setString( 5,  metrics.getJobState().getText());
                    stmt.setInt(    6,  metrics.getNumArchives());
                    stmt.setInt(    7,  metrics.getNumArchivesComplete());
                    stmt.setLong(   8,  metrics.getNumFiles());
                    stmt.setLong(   9,  metrics.getNumFilesComplete());
                    stmt.setLong(   10, metrics.getStartTime());
                    stmt.setLong(   11, metrics.getTotalCompressedSize());
                    stmt.setLong(   12, metrics.getTotalSize());
                    stmt.setString( 13, metrics.getUserName());
                    stmt.executeUpdate();
                    
                    // Note: If the container Datasource has jta=true this will throw
                    // an exception.
                    conn.commit();
                    
                }
                catch (SQLException se) {
                    LOGGER.error("An unexpected SQLException was raised while "
                            + "attempting to insert a new [ "
                            + TABLE_NAME 
                            + " ] object "
                            + "into the data store.  Error message [ "
                            + se.getMessage() 
                            + " ].");
                	LOGGER.info("Error occurred attempting to insert the "
                			+ "following metrics record => [ "
                			+ metrics.toString()
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
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Insert of [ "
                    + TABLE_NAME
                    + " ] record for job ID [ "
                    + metrics.getJobID()
                    + " ] completed in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
    }
}
