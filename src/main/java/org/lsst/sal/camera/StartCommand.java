/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lsst.sal.camera;

import org.lsst.sal.SAL_camera;

/**
 *
 * @author tonyj
 */
class StartCommand extends CameraCommand {

    public StartCommand(int cmdId, String configuration) {
        super(cmdId);
    }

    @Override
    void issueCommand(SAL_camera mgr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}