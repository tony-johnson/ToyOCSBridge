package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 * The result of sending a command
 * @author tonyj
 */
public class CommandResponse {

    private final CameraCommand command;
    private final int cmdId;
    private final SAL_camera mgr;

    CommandResponse(SAL_camera mgr, CameraCommand command, int cmdId) {
        this.mgr = mgr;
        this.command = command;
        this.cmdId = cmdId;
    }
    
    public void waitForResponse(Duration timeout) {
        command.waitForResponse(mgr,cmdId,timeout);
    }
}
