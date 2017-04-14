package org.lsst.sal.camera;

import java.time.Duration;
import java.time.Instant;
import org.lsst.sal.SAL_camera;

/**
 * An implementation of SALCamera which works by using the existing SAL
 * interface. This can be used as-is, but might be better generated by SAL.
 *
 * @author tonyj
 */
class SALCameraImplementation extends SALCamera {

    private final SAL_camera mgr;

    SALCameraImplementation() {
        mgr = new SAL_camera();
        mgr.createP
    }

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public CameraCommand getNextCommand(Duration timeout) throws SALException {
        Instant stop = Instant.now().plus(timeout);

        // Currently we have to poll for each command
        mgr.salProcessor("camera_command_setFilter");
        camera.command_setFilter setFilterCommand = new camera.command_setFilter();
        mgr.salProcessor("camera_command_takeImages");
        camera.command_takeImages takeImagesCommand = new camera.command_takeImages();
        mgr.salProcessor("camera_command_initImage");
        camera.command_initImage initImageCommand = new camera.command_initImage();
        mgr.salProcessor("camera_command_enable");
        camera.command_enable enableCommand = new camera.command_enable();
        mgr.salProcessor("camera_command_disable");
        camera.command_disable disableCommand = new camera.command_disable();
        mgr.salProcessor("camera_command_enterControl");
        camera.command_enterControl enterControlCommand = new camera.command_enterControl();
        mgr.salProcessor("camera_command_exitControl");
        camera.command_exitControl exitControlCommand = new camera.command_exitControl();
        mgr.salProcessor("camera_command_start");
        camera.command_start startCommand = new camera.command_start();
        mgr.salProcessor("camera_command_standby");
        camera.command_standby standbyCommand = new camera.command_standby();

        Instant now = Instant.now();
        while (!now.isAfter(stop)) {
            int cmdId = mgr.acceptCommand_setFilter(setFilterCommand);
            if (cmdId > 0) {
                return new SetFilterCommand(cmdId, setFilterCommand.name);
            }
            cmdId = mgr.acceptCommand_takeImages(takeImagesCommand);
            if (cmdId > 0) {
                return new TakeImagesCommand(cmdId, takeImagesCommand.expTime, takeImagesCommand.numImages, takeImagesCommand.shutter,
                        takeImagesCommand.science, takeImagesCommand.wfs, takeImagesCommand.guide, takeImagesCommand.imageSequenceName);
            }
            cmdId = mgr.acceptCommand_initImage(initImageCommand);
            if (cmdId > 0) {
                return new InitImageCommand(cmdId, initImageCommand.deltaT);
            }
            cmdId = mgr.acceptCommand_enable(enableCommand);
            if (cmdId > 0) {
                return new EnableCommand(cmdId);
            }
            cmdId = mgr.acceptCommand_disable(disableCommand);
            if (cmdId > 0) {
                return new DisableCommand(cmdId);
            }
            cmdId = mgr.acceptCommand_enterControl(enterControlCommand);
            if (cmdId > 0) {
                return new EnterControlCommand(cmdId);
            }
            cmdId = mgr.acceptCommand_exitControl(exitControlCommand);
            if (cmdId > 0) {
                return new ExitControlCommand(cmdId);
            }
            cmdId = mgr.acceptCommand_start(startCommand);
            if (cmdId > 0) {
                return new StartCommand(cmdId, startCommand.configuration);
            }
            cmdId = mgr.acceptCommand_standby(standbyCommand);
            if (cmdId > 0) {
                return new StandbyCommand(cmdId);
            }
            try {
                // FIXME: Would be great if we did not have to poll
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                throw new SALException("Unexpected interupt while polling for command", ex);
            }
        }
        return null; // Timeout
    }

    @Override
    public void issueCommand(CameraCommand command) throws SALException {
        command.issueCommand(mgr);
    }

}
