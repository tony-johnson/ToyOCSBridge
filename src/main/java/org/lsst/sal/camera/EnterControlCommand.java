package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 * 
 * @author tonyj
 */
class EnterControlCommand extends CameraCommand {

    EnterControlCommand(int cmdId, SAL_camera mgr) {
        super(cmdId, mgr);
    }
    
    @Override
    CommandResponse issueCommand(SAL_camera mgr) {
        camera.command_enterControl cmd = new camera.command_enterControl();
        int cmdId = mgr.issueCommand_enterControl(cmd);
        return new CommandResponse(mgr, this,cmdId);
    }    

    @Override
    public void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout) {
        mgr.waitForCompletion_enterControl(cmdId, (int) timeout.getSeconds());
    }

    @Override
    void acknowledgeCommand(int response, int timeout, String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
