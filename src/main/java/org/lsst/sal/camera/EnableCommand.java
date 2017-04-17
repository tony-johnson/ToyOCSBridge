package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class EnableCommand extends CameraCommand {

    public EnableCommand(int cmdId) {
        super(cmdId);
    }

    @Override
    CommandResponse issueCommand(SAL_camera mgr) {
        camera.command_enable cmd = new camera.command_enable();
        int cmdId = mgr.issueCommand_enable(cmd);
        return new CommandResponse(mgr, this,cmdId);
    }

    @Override
    void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout) {
        mgr.waitForCompletion_enable(cmdId, (int) timeout.getSeconds());
    }
}
