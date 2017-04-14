package org.lsst.sal.camera;

import java.time.Duration;

/**
 *
 * @author tonyj
 */
public class Main {
    public static void main(String[] args) throws SALException {
        SALCamera camera = SALCamera.instance();
        CameraCommand command = camera.getNextCommand(Duration.ofSeconds(60));
        System.out.println("Got "+command);
    }
}
