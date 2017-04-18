package org.lsst.sal.camera.test;

import java.time.Duration;
import org.lsst.sal.camera.CameraCommand;
import org.lsst.sal.camera.SALCamera;
import org.lsst.sal.camera.SALException;

/**
 *
 * @author tonyj
 */
public class Main {
    public static void main(String[] args) throws SALException, InterruptedException {
        SALCamera camera = SALCamera.instance();
        CameraCommand command = camera.getNextCommand(Duration.ofSeconds(60));
        System.out.println("Got "+command);
        command.acknowledgeCommand(Duration.ofSeconds(1));
        Thread.sleep(500);
        command.reportComplete();
    }
}
