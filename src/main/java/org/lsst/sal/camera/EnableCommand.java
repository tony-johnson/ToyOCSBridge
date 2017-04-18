package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
public class EnableCommand extends CameraCommand {
    
    public EnableCommand() {
    }

    EnableCommand(int cmdId, SAL_camera mgr) {
        super(cmdId, mgr);
    }

    @Override
    CommandResponse issueCommand(SAL_camera mgr) {
        camera.command_enable cmd = new camera.command_enable();
        int cmdId = mgr.issueCommand_enable(cmd);
        return new CommandResponse(mgr, this,cmdId);
    }

    @Override
    public void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout) {
        mgr.waitForCompletion_enable(cmdId, (int) timeout.getSeconds());
    }

    @Override
    public void acknowledgeCommand(int response, int timeout, String message) {
        getManager().ackCommand_enable(getCmdId(), response, timeout, message);
    }
}
