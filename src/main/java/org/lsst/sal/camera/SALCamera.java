package org.lsst.sal.camera;

import java.time.Duration;

/**
 * A simplified Java interface to SAL. This is designed as a thought experiment for what
 * the SAL Java interface for the camera could look like,
 * @author tonyj
 */
public abstract class SALCamera {
    private static SALCamera theCamera;
    
    /**
     * Protected constructor to force singleton access via instance(). Not sure if 
     * this is required, is there any problem with creating multiple instances of this class?
     */
    protected SALCamera() {
        
    }
    
    public static synchronized SALCamera instance() {
        if (theCamera == null) theCamera = new SALCameraImplementation();
        return theCamera;
    }
    
    /**
     * Fetch the next command, or timeout if specified duration is exceeded.
     * @param timeout The maximum time to wait before timing out
     * @return The next command received, or <code>null</code> if the wait times-out.
     * @throws org.lsst.sal.camera.SALException If a communication or similar error occurs
     */
    public abstract CameraCommand getNextCommand(Duration timeout) throws SALException;

    public abstract CommandResponse issueCommand(CameraCommand command) throws SALException;
    
}
