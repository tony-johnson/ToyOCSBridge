package org.lsst.sal.camera;

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
    void issueCommand(SAL_camera mgr) {
        camera.command_enable cmd = new camera.command_enable();
        mgr.issueCommand_enable(cmd);
    }
}
