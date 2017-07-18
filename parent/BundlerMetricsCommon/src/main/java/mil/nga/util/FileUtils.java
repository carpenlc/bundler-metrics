package mil.nga.util;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Collection of methods that interact with the file system.
 *
 * @author L. Craig Carpenter
 */
public class FileUtils {

    /**
     * Delete method that will recursively delete the input file.  If the file
     * is a directory the method will recurse through all of the files in that 
     * directory deleting each one prior to attempting deletion of the input 
     * directory.
     * 
     * @param filename The file to delete.
     * @throws IOException Thrown if there are issues accessing, or deleting
     * the target file.
     */
    public static void delete(String filename) 
            throws IOException {
        if ((filename != null) && (!filename.isEmpty())) {
            delete(new File(filename));
        }
    }

    /**
     * Delete method that will recursively delete the input file.  If the file
     * is a directory the method will recurse through all of the files in that 
     * directory deleting each one prior to attempting deletion of the input 
     * directory.
     * 
     * @param file The file to delete.
     * @throws IOException Thrown if there are issues accessing, or deleting
     * the target file.
     */
    public static void delete(File file) throws IOException {
        String method = "delete() - ";
        if ((file != null) && (file.exists())) {
            if (file.isDirectory()) {
                if (file.list().length == 0) {
                    file.delete();
                }
                else {
                    String files[] = file.list();
                    for (String current : files) {
                        File fileToDelete = new File(file, current);
                        delete(fileToDelete);
                        if (file.list().length == 0) {
                            file.delete();
                        }
                    }
                }
            }
            else {
                file.delete();
            }
        }
        else {
            throw new IOException(method 
                    + "The input file is null or does not exist.");
        }
    }

    /**
     * Generate a random hex encoded string token of the specified length.
     * Since there are two hex characters per byte, the random hex string 
     * returned will be twice as long as the user-specified length.
     *  
     * @param length The number of random bytes to use
     * @return random hex string
     */
    public static synchronized String generateUniqueToken(int length) {

        byte         random[]        = new byte[length];
        Random       randomGenerator = new Random();
        StringBuffer buffer          = new StringBuffer();

        randomGenerator.nextBytes(random);

        for (int j = 0; j < random.length; j++)
        {
            byte b1 = (byte) ((random[j] & 0xf0) >> 4);
            byte b2 = (byte) (random[j] & 0x0f);
            if (b1 < 10)
                buffer.append((char) ('0' + b1));
            else
                buffer.append((char) ('A' + (b1 - 10)));
            if (b2 < 10)
                buffer.append((char) ('0' + b2));
            else
                buffer.append((char) ('A' + (b2 - 10)));
        }

        return (buffer.toString());
    }

    /**
     * Simple method to convert a time (in milliseconds) to a printable
     * String.
     * 
     * @param format The format to pass into the SimpleDateFormat class.
     * @param time The time in milliseconds from the epoch.
     * @return The date in String format.
     */
    public static String getTimeAsString(String format, long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(cal.getTime());
    }

    /**
     * The File.getLength() method returns file sizes in bytes.  This 
     * method will convert the size information to a long representation
     * in the units of MByte.  If the file is actually less than 1MByte, 1
     * will be returned.
     * 
     * @param bytes The size of the file in bytes.
     * @param si If true output calculation is made on bytes/1000, if false 
     * binary sizes are used (i.e. bytes/1024)
     * @return The size in human readable format
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

}

