package org.lsst.sal.camera;

import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class DisableCommand extends CameraCommand {

    public DisableCommand(int cmdId) {
        super(cmdId);
    }

    @Override
    void issueCommand(SAL_camera mgr) {
        camera.command_disable cmd = new camera.command_disable();
        mgr.issueCommand_disable(cmd);
    }
    
}
