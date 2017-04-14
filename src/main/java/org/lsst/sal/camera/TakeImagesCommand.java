package org.lsst.sal.camera;

import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class TakeImagesCommand extends CameraCommand {

    public TakeImagesCommand(int cmdId, double expTime, int numImages, boolean shutter, boolean science, boolean wfs, boolean guide, String imageSequenceName) {
        super(cmdId);
    }

    @Override
    void issueCommand(SAL_camera mgr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
