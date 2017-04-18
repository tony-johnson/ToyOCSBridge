package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class ExitControlCommand extends CameraCommand {

    ExitControlCommand(int cmdId, SAL_camera mgr) {
        super(cmdId, mgr);
    }

    @Override
    CommandResponse issueCommand(SAL_camera mgr) {
        camera.command_exitControl cmd = new camera.command_exitControl();
        int cmdId = mgr.issueCommand_exitControl(cmd);
        return new CommandResponse(mgr, this,cmdId);
    }

    @Override
    public void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout) {
        mgr.waitForCompletion_exitControl(cmdId, (int) timeout.getSeconds());
    }

    @Override
    void acknowledgeCommand(int response, int timeout, String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
