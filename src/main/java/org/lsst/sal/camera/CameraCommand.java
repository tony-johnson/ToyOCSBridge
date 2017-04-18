package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 * Super class for all camera commands, 
 *
 * @author tonyj
 */
public abstract class CameraCommand {

    private SAL_camera mgr;    
    private final int cmdId;

    public CameraCommand() {
        cmdId = 0;
    }

    CameraCommand(int cmdId, SAL_camera mgr) {
        this.cmdId = cmdId;
        this.mgr = mgr;
    }

    abstract CommandResponse issueCommand(SAL_camera mgr);

    public abstract void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout);

    public void acknowledgeCommand(Duration timeout) {
        acknowledgeCommand(SAL_camera.SAL__CMD_INPROGRESS, (int) timeout.getSeconds(), "Ack : OK");
    }

    public void reportComplete() {
        acknowledgeCommand(SAL_camera.SAL__CMD_COMPLETE, 0, "Done : OK");
    }

    public void reportError(Exception ex) {
        acknowledgeCommand(SAL_camera.SAL__CMD_FAILED, 0, "Error : " + ex.getMessage());
    }

    public void rejectCommand(String reason) {
        // TODO: Can we append the reason to the response?
        acknowledgeCommand(SAL_camera.SAL__CMD_FAILED, 0, "Ack : NO");
    }

    SAL_camera getManager() {
        return mgr;
    }

    public int getCmdId() {
        return cmdId;
    }

    abstract void acknowledgeCommand(int response, int timeout, String message);
}
