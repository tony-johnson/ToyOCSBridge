package org.lsst.sal.camera;

import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class ExitControlCommand extends CameraCommand {

    public ExitControlCommand(int cmdId) {
        super(cmdId);
    }

    @Override
    void issueCommand(SAL_camera mgr) {
        camera.command_exitControl cmd = new camera.command_exitControl();
        mgr.issueCommand_exitControl(cmd);
    }

}
