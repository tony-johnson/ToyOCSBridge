package org.lsst.sal.camera.test;

import java.time.Duration;
import org.lsst.sal.camera.CommandResponse;
import org.lsst.sal.camera.EnableCommand;
import org.lsst.sal.camera.SALCamera;
import org.lsst.sal.camera.SALException;

/**
 *
 * @author tonyj
 */
public class Main2 {
    public static void main(String[] args) throws SALException {
       SALCamera camera = SALCamera.instance();
       CommandResponse response = camera.issueCommand(new EnableCommand());
       response.waitForResponse(Duration.ofSeconds(10));
    }
}
