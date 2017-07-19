package mil.nga.bundler.ejb;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.bundler.ejb.EJBClientUtilities;
import mil.nga.bundler.ejb.interfaces.JobMetricsCollectorI;

/**
 * Timer Bean implemented to collect the bundler per-job metrics.  This timer 
 * will fire every hour at 15 minutes after the hour.
 * 
 * @author L. Craig Carpenter
 */
@Stateless
public class JobMetricsCollectorTimer {

    /**
     * Set up the Log4j system for use throughout the class
     */        
    private static Logger LOGGER = LoggerFactory.getLogger(
            JobMetricsCollectorTimer.class);
    
    /**
     * Container-injected reference to the CleanupService object.
     */
    @EJB
    JobMetricsCollectorI metricsCollector;
    
    /**
     * String to use to output dates in String format for logging purposes.
     */
    private static final String DATE_STRING = "yyyy/MM/dd HH:mm:ss:SSS";
    
    /**
     * Default eclipse-generated constructor. 
     */
    public JobMetricsCollectorTimer() { }
	
    /**
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JobService EJB.
     */
    private JobMetricsCollectorI getJobMetricsCollector() {
        if (metricsCollector == null) {
            LOGGER.warn("Application container failed to inject the "
                    + "reference to [ JobMetricsCollectorI ].  Attempting to "
                    + "look it up via JNDI.");
            metricsCollector = EJBClientUtilities
                    .getInstance()
                    .getJobMetricsCollector();
        }
        return metricsCollector;
    }
    
    /**
     * Entry point called by the application container to collect job
     * metrics.  
     * 
     * Execute the timer once an hour at the 15-minute mark.
     * 
     * @param t Container injected Timer object.
     */
	@SuppressWarnings("unused")
    @Schedule(second="0", minute="15", hour="*", dayOfWeek="*",
    dayOfMonth="*", month="*", year="*", info="JobMetricsCollectorTimer")
    private void scheduledTimeout(final Timer t) {
	    
	    DateFormat df = new SimpleDateFormat(DATE_STRING);
        
	    LOGGER.info("JobMetricsCollectorTimer launched at [ "
                + df.format(new Date(System.currentTimeMillis()))
                + " ].");
	       
	    if (getJobMetricsCollector() != null) {
	        getJobMetricsCollector().collectMetrics();
	        LOGGER.info("JobMetricsCollectorTimer complete at [ "
	                    + df.format(new Date(System.currentTimeMillis()))
	                    + " ].");
	    }
	    else {
	        LOGGER.error("Unable to obtain a reference to the ");
	    } 
    }
}