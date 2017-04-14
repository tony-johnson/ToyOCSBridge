package org.lsst.sal.camera;

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
    void issueCommand(SAL_camera mgr) {
        camera.command_enterControl cmd = new camera.command_enterControl();
        mgr.issueCommand_enterControl(cmd);
    }    
}
