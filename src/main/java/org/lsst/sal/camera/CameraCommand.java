package org.lsst.sal.camera;

import org.lsst.sal.SAL_camera;

/**
 * Super class for all camera commands,
 * @author tonyj
 */
public abstract class CameraCommand extends SALCommand {
    
    public CameraCommand(int cmdId) {
        super(cmdId);
    }

    abstract void issueCommand(SAL_camera mgr);
    
}
