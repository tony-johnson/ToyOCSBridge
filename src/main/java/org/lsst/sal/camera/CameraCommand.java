package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 * Super class for all camera commands,
 * @author tonyj
 */
public abstract class CameraCommand extends SALCommand {
    
    public CameraCommand(int cmdId) {
        super(cmdId);
    }

    abstract CommandResponse issueCommand(SAL_camera mgr);    

    abstract void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout);
}
