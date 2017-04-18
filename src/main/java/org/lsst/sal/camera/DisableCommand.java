package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class DisableCommand extends CameraCommand {

    DisableCommand(int cmdId, SAL_camera mgr) {
        super(cmdId, mgr);
    }

    @Override
    CommandResponse issueCommand(SAL_camera mgr) {
        camera.command_disable cmd = new camera.command_disable();
        int cmdId = mgr.issueCommand_disable(cmd);
        return new CommandResponse(mgr, this,cmdId);
    }

    @Override
    public void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout) {
        mgr.waitForCompletion_enable(cmdId, (int) timeout.getSeconds());
    }  

    @Override
    void acknowledgeCommand(int response, int timeout, String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
