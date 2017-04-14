package org.lsst.sal.camera;

import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class SetFilterCommand extends CameraCommand {

    String filterName;
    public SetFilterCommand(int cmdId, String name) {
        super(cmdId);
        this.filterName = name;
    }

    @Override
    void issueCommand(SAL_camera mgr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
