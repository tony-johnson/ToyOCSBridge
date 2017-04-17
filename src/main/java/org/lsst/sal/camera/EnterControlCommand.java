package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class EnterControlCommand extends CameraCommand {

    public EnterControlCommand(int cmdId) {
        super(cmdId);
    }
    
    @Override
    CommandResponse issueCommand(SAL_camera mgr) {
        camera.command_enterControl cmd = new camera.command_enterControl();
        int cmdId = mgr.issueCommand_enterControl(cmd);
        return new CommandResponse(mgr, this,cmdId);
    }    

    @Override
    void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout) {
        mgr.waitForCompletion_enterControl(cmdId, (int) timeout.getSeconds());
    }
}
