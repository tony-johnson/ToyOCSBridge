package org.lsst.sal.camera;

import java.time.Duration;
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
    CommandResponse issueCommand(SAL_camera mgr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void waitForResponse(SAL_camera mgr, int cmdId, Duration timeout) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    } 
}
