package mil.nga.bundler.ejb.exceptions;

import java.io.Serializable;

/**
 * Exception raised when there are problems obtaining references to 
 * requested/required EJBs.
 * 
 * @author L. Craig Carpenter
 */
public class EJBLookupException extends Exception implements Serializable {

    /**
     * The name of the EJB that resulted in the Exception.
     */
    private String ejbName;
    
    /** 
     * Default constructor requiring a message String.
     * @param msg Information identifying why the exception was raised.
     */
    public EJBLookupException(String msg) {
        super(msg);
    }
    
    /** 
     * Optional constructor allowing clients to supply a String identifying 
     * which EJB resulted in the Exception.
     * @param msg Information identifying why the exception was raised.
     * @param ejb Name of EJB resulting in the Exception.
     */
    public EJBLookupException(String msg, String ejb) {
        super(msg);
        setEJBName(ejb);
    }
    
    /**
     * Getter method for the EJB name causing the exception.
     * @return The EJB name causing the exception.
     */
    public String getEJBName() {
        return ejbName;
    }
    
    /**
     * Setter method for the EJB name causing the exception.
     * @param value The EJB name causing the exception.
     */
    public void setEJBName(String value) {
        ejbName = value;
    }
    
}
