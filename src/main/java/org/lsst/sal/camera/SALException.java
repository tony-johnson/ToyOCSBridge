package org.lsst.sal.camera;

/**
 * Generic exception thrown if a communication error occurs.
 * @author tonyj
 */
public class SALException extends Exception {
    SALException(String message, Throwable cause) {
        super(message,cause);
    }
}
