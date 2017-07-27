package mil.nga.bundler.ejb.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.sql.DataSource;

import mil.nga.bundler.model.FileEntry;
import mil.nga.bundler.types.JobStateType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods that interface with the FILE_ENTRY table 
 * that will allow the Job class to be materialized without the use of the 
 * complex joins that occur in JPA.  
 * 
 * This class is written assuming that the injected DataSource object is not
 * handling the transactions on behalf of the application (i.e. non-JTA).  If 
 * this bean is deployed to a container with JTA enabled, the insert and 
 * update functions will throw exceptions when attempting to manage the 
 * underlying transaction.
 * 
 * @author L. Craig Carpenter
 */
@Stateless
@LocalBean
public class JDBCFileService {
    
    /**
     * Table used to extract the target archive information.
     */
    private static final String TABLE_NAME = "FILE_ENTRY";
    
    /**
     * Set up the logging system for use throughout the class
     */        
    private static final Logger LOGGER = LoggerFactory.getLogger(
            JDBCFileService.class);
    
    /**
     * Container-injected datasource object.
     */
    @Resource(mappedName="java:jboss/datasources/JobTracker")
    DataSource datasource;
    
    /**
     * Default Eclipse-generated constructor. 
     */
    public JDBCFileService() { }
    
    /**
     * Delete all files that match the input job ID.
     * 
     * @param archiveID The archive ID requested. (must be greater than zero)
     * @param jobID The job ID requested (must not be null, or empty String)
     */
    public void deleteFiles(String jobID) {
        
        Connection        conn   = null;
        PreparedStatement stmt   = null;
        long             start  = System.currentTimeMillis();
        String            sql    = "delete from " + TABLE_NAME + " where "
                + "JOB_ID = ?";
        
        if (datasource != null) {
            if ((jobID != null) && (!jobID.isEmpty())) {
                
                try { 
                    
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
                catch (SQLException se) {
                    LOGGER.error("An unexpected SQLException was raised "
                            + "while attempting to delete [ "
                            + TABLE_NAME
                            + " ] records associated with job ID [ "
                            + jobID
                            + " ].  Error message [ "
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
                LOGGER.warn("The input job ID is null or empty.  Unable to "
                        + "delete the file list.");
            }
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[ "
                    + TABLE_NAME 
                    + " ] records deleted in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }    
    }
    
    /**
     * Delete all files that match the imput archive ID and job ID.
     * 
     * @param archiveID The archive ID requested. (must be greater than zero)
     * @param jobID The job ID requested (must not be null, or empty String)
     */
    public void deleteFiles(long archiveID, String jobID) {
        
        Connection        conn   = null;
        PreparedStatement stmt   = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "delete from " + TABLE_NAME + " where "
                + "ARCHIVE_ID = ? AND JOB_ID = ?";
        
        if (datasource != null) {
            if (archiveID >= 0) {
                if ((jobID != null) && (!jobID.isEmpty())) {
                    
                    try { 
                        
                        conn = datasource.getConnection();
                        
                        // Note: If the container Datasource has jta=true this will throw
                        // an exception.
                        conn.setAutoCommit(false);
                        
                        stmt = conn.prepareStatement(sql);
                        stmt.setLong(1, archiveID);
                        stmt.setString(2, jobID);
                        stmt.executeUpdate();
                        
                        // Note: If the container Datasource has jta=true this will throw
                        // an exception.
                        conn.commit();
                    
                    }
                    catch (SQLException se) {
                        LOGGER.error("An unexpected SQLException was raised "
                                + "while attempting to delete [ "
                                + TABLE_NAME
                                + " ] records associated with job ID [ "
                                + jobID
                                + " ] and archive ID [ "
                                + archiveID
                                + " ].  Error message [ "
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
                    LOGGER.warn("The input job ID is null or empty.  Unable to "
                            + "delete the file list.");
                }
            }
            else {
                LOGGER.error("The input archive ID is out-of-range.  The archive "
                        + "ID should be greater than, or equal to zero.  Value "
                        + "supplied [ "
                        + archiveID
                        + " ].  Unable to execute delete command.");
            }
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[ "
                    + TABLE_NAME
                    + " ] records delected in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }    
    }
    
    /**
     * Retrieve the list of files associated with the input archive ID and 
     * job ID.
     * 
     * @param archiveID The archive ID requested. (must be greater than zero)
     * @param jobID The job ID requested (must not be null, or empty String)
     * @return The requested list of files.
     */
    public List<FileEntry> getFiles(long archiveID, String jobID) {
        
        List<FileEntry>   files  = new ArrayList<FileEntry>();
        Connection        conn   = null;
        PreparedStatement stmt   = null;
        ResultSet         rs     = null;
        long             start  = System.currentTimeMillis();
        String            sql    = "select ID, ARCHIVE_ID, "
                + "ARCHIVE_ENTRY_PATH, FILE_STATE, JOB_ID, PATH, FILE_SIZE "
                + "from "
                + TABLE_NAME
                + " where ARCHIVE_ID = ? AND JOB_ID = ?";
        
        if (datasource != null) {
            if (archiveID >= 0) {
                if ((jobID != null) && (!jobID.isEmpty())) {
                    
                    try { 
                        
                        conn = datasource.getConnection();
                        stmt = conn.prepareStatement(sql);
                        stmt.setLong(1, archiveID);
                        stmt.setString(2, jobID);
                        rs   = stmt.executeQuery();
                        
                        while (rs.next()) {
                            
                            FileEntry file = new FileEntry();
                            file.setID(rs.getLong("ID"));
                            file.setArchiveID(rs.getLong("ARCHIVE_ID"));
                            file.setEntryPath(rs.getString("ARCHIVE_ENTRY_PATH"));
                            file.setFileState(
                                    JobStateType.valueOf(
                                            rs.getString("FILE_STATE")));
                            file.setJobID(rs.getString("JOB_ID"));
                            file.setFilePath(rs.getString("PATH"));
                            file.setSize(rs.getLong("FILE_SIZE"));
                            files.add(file);
                            
                        }
                    }
                    catch (SQLException se) {
                        LOGGER.error("An unexpected SQLException was raised while "
                                + "attempting to retrieve a list of [ "
                                + TABLE_NAME
                                + " ] objects from the target data source.  Error "
                                + "message [ "
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
                    LOGGER.warn("The input job ID is null or empty.  Unable to "
                            + "retrieve the file list.");
                }
            }
            else {
                LOGGER.error("The input archive ID is out-of-range.  The archive "
                        + "ID should be greater than, or equal to zero.  Value "
                        + "supplied [ "
                        + archiveID
                        + " ].");
            }
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[ "
                    + files.size() 
                    + " ] files selected in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
        
        return files;
    }
    
    /**
     * Retrieve a complete list of job IDs from the data store.  This method was 
     * added in an effort to clean-up orphaned FILE_ENTRY records.
     * 
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
     * Persist (update) the information associated with the input 
     * <code>FILE_ENTRY</code> object.
     * 
     * @param file <code>FILE_ENTRY</code> object containing updated state 
     * information.
     */
    public void updateFile(FileEntry file) {
        
        Connection        conn   = null;
        PreparedStatement stmt   = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "update " + TABLE_NAME + " set ARCHIVE_ID = ?, "
                + "ARCHIVE_ENTRY_PATH = ?, FILE_STATE = ?, JOB_ID = ?, "
                + "PATH = ? , FILE_SIZE = ? where ID = ?";
        
        if (datasource != null) {
            if (file != null) {
                    
                try { 
                    
                    conn = datasource.getConnection();
                    
                    // Note: If the container Datasource has jta=true this will throw
                    // an exception.
                    conn.setAutoCommit(false);
                    
                    stmt = conn.prepareStatement(sql);
                    stmt.setLong(   1, file.getArchiveID());
                    stmt.setString( 2, file.getEntryPath());
                    stmt.setString( 3, file.getFileState().getText());
                    stmt.setString( 4, file.getJobID());
                    stmt.setString( 5, file.getFilePath());
                    stmt.setLong(   6, file.getSize());
                    stmt.setLong(   7, file.getID());
                    stmt.executeUpdate();
                    
                    // Note: If the container Datasource has jta=true this will throw
                    // an exception.
                    conn.commit();
                    
                }
                catch (SQLException se) {
                    LOGGER.error("An unexpected SQLException was raised while "
                            + "attempting to update [ "
                            + TABLE_NAME 
                            + " ] object with ID [ "
                            + file.getID()
                            + " ].  Error message [ "
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
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Update of [ "
                    + TABLE_NAME 
                    + " ] record with ID [ "
                    + file.getID()
                    + " ] completed in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }

    }

    /**
     * Persist (update) the information associated with the input 
     * list of <code>FILE_ENTRY</code> objects.
     * 
     * @param files List of <code>FILE_ENTRY</code> objects containing 
     * updated state information.
     */
    public void updateFiles(List<FileEntry> files) {
        if ((files != null) && (files.size() > 0)) {
            for (FileEntry file : files) {
                updateFile(file);
            }
        }
        else {
            LOGGER.warn("The input file list is null or contains no objects.  "
                    + "insert operation not performed.");
        }
    }
    
    /**
     * Persist (insert) the information associated with the input 
     * <code>FILE_ENTRY</code> object.
     * 
     * @param file <code>FILE_ENTRY</code> object containing updated state 
     * information.
     */
    public void insertFile(FileEntry file) {
        
        Connection        conn   = null;
        PreparedStatement stmt   = null;
        long              start  = System.currentTimeMillis();
        String            sql    = "insert into "
                + TABLE_NAME 
                + " (ARCHIVE_ID, "
                + "ARCHIVE_ENTRY_PATH, FILE_STATE, JOB_ID, "
                + "PATH, FILE_SIZE) values (?, ?, ?, ?, ?, ?)";
        
        if (datasource != null) {
            if (file != null) {
                    
                try { 
                    
                    conn = datasource.getConnection();
                    
                    // Note: If the container Datasource has jta=true this will throw
                    // an exception.
                    conn.setAutoCommit(false);
                    
                    stmt = conn.prepareStatement(sql);
                    stmt.setLong(1, file.getArchiveID());
                    stmt.setString(2, file.getEntryPath());
                    stmt.setString(3, file.getFileState().getText());
                    stmt.setString(4, file.getJobID());
                    stmt.setString(5, file.getFilePath());
                    stmt.setLong(6,  file.getSize());
                    stmt.executeUpdate();
                    
                    // Note: If the container Datasource has jta=true this will throw
                    // an exception.
                    conn.commit();
                    
                }
                catch (SQLException se) {
                    LOGGER.error("An unexpected SQLException was raised while "
                            + "attempting to insert a new [ "
                            + TABLE_NAME 
                            + " ] object.  "
                            + "Error message [ "
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
        }
        else {
            LOGGER.warn("DataSource object not injected by the container.  "
                    + "An empty List will be returned to the caller.");
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Insert of [ "
                    + TABLE_NAME 
                    + " ] for job ID [ "
                    + file.getJobID()
                    + " ] completed in [ "
                    + (System.currentTimeMillis() - start) 
                    + " ] ms.");
        }
    }
    
    /**
     * Persist (insert) the information associated with the input 
     * list of <code>FILE_ENTRY</code> objects.
     * 
     * @param files List of <code>FILE_ENTRY</code> objects to persist.
     */
    public void insertFiles(List<FileEntry> files) {
        if ((files != null) && (files.size() > 0)) {
            for (FileEntry file : files) {
                insertFile(file);
            }
        }
        else {
            LOGGER.warn("The input file list is null or contains no objects.  "
                    + "insert operation not performed.");
        }
    }
}
