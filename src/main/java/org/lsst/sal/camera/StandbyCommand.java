package org.lsst.sal.camera;

import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class StandbyCommand extends CameraCommand {

    public StandbyCommand(int cmdId) {
        super(cmdId);
    }

    @Override
    void issueCommand(SAL_camera mgr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
