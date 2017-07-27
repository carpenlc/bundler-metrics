package mil.nga.bundler.ejb.interfaces;

import javax.ejb.Remote;

/**
 * Local interface implemented by the JobMetricsCollector session bean. 
 * 
 * @author L. Craig Carpenter
 */
@Remote
public interface JobMetricsCollectorI {
 
    /**
     * Public entry point starting the metrics collection process.
     */
    public void collectMetrics();
    
}
