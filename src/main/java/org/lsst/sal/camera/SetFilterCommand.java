package org.lsst.sal.camera;

import java.time.Duration;
import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class SetFilterCommand extends CameraCommand {

    String filterName;
    public SetFilterCommand(int cmdId, SAL_camera mgr, String name) {
        super(cmdId, mgr);
        this.filterName = name;
    }

    @Override
    CommandResponse issueCommand(SAL_camera mgr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void acknowledgeCommand(int response, int timeout, String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    } 
}
