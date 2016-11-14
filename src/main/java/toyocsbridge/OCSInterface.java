package toyocsbridge;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lsst.sal.SAL_camera;

/**
 * Interface to the real OCS.
 *
 * @author tonyj
 */
public class OCSInterface {

    private final SAL_camera mgr;
    private boolean shutdown = false;
    private Thread runThread;
    private final ToyOCSBridge bridge;
    private static final Logger logger = Logger.getLogger(OCSInterface.class.getName());

    OCSInterface(ToyOCSBridge bridge) {
        this.bridge = bridge;
        mgr = new SAL_camera();
        ExtendedOCSCommandExecutor exec = new ExtendedOCSCommandExecutor(bridge.getCCS());
        bridge.setExecutor(exec);
        bridge.getCCS().addStateChangeListener((currentState, oldState) -> {
            // For now send a generic event
            String msg = String.format("State Changed %s: %s->%s", currentState.getClass().getSimpleName(), oldState, currentState);
            int priority = 1;
            mgr.logEvent(msg, priority);
        });
    }

    public static void main(String[] args) {
        ToyOCSBridge bridge = new ToyOCSBridge();
        OCSInterface ocsInterface = new OCSInterface(bridge);
        ToyOCSGUI gui = new ToyOCSGUI(bridge);
        gui.setVisible(true);

        Thread t = new Thread("OCSCommandReceiver") {

            @Override
            public void run() {
                try {
                    ocsInterface.run();
                } catch (Exception x) {
                    logger.log(Level.WARNING, "Failed to initialize OCS communication layer, "
                            + "check that OCS has been setup correctly. Reverting to standalone mode.", x);
                }
            }

        };
        t.start();

    }

    @SuppressWarnings("SleepWhileInLoop")
    void run() {
        try {
            runThread = Thread.currentThread();
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

            while (!shutdown) {
                int cmdId = mgr.acceptCommand_setFilter(setFilterCommand);
                if (cmdId > 0) {
                    bridge.setFilter(cmdId, setFilterCommand.name);
                }
                cmdId = mgr.acceptCommand_takeImages(takeImagesCommand);
                if (cmdId > 0) {
                    bridge.takeImages(cmdId, takeImagesCommand.expTime, takeImagesCommand.numImages, takeImagesCommand.shutter,
                            takeImagesCommand.science, takeImagesCommand.wfs, takeImagesCommand.guide, takeImagesCommand.imageSequenceName);
                }
                cmdId = mgr.acceptCommand_initImage(initImageCommand);
                if (cmdId > 0) {
                    bridge.initImage(cmdId, initImageCommand.deltaT);
                }
                cmdId = mgr.acceptCommand_enable(enableCommand);
                if (cmdId > 0) {
                    bridge.enable(cmdId);
                }
                cmdId = mgr.acceptCommand_disable(disableCommand);
                if (cmdId > 0) {
                    bridge.disable(cmdId);
                }
                cmdId = mgr.acceptCommand_enterControl(enterControlCommand);
                if (cmdId > 0) {
                    bridge.enterControl(cmdId);
                }
                cmdId = mgr.acceptCommand_exitControl(exitControlCommand);
                if (cmdId > 0) {
                    bridge.exitControl(cmdId);
                }
                cmdId = mgr.acceptCommand_start(startCommand);
                if (cmdId > 0) {
                    bridge.start(cmdId, startCommand.configuration);
                }
                cmdId = mgr.acceptCommand_standby(standbyCommand);
                if (cmdId > 0) {
                    bridge.standby(cmdId);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, "Unexpected interruption", ex);
                }
            }
            mgr.salShutdown();
        } finally {
            runThread = null;
        }
    }

    void shutdown() throws InterruptedException {
        shutdown = true;
        runThread.join();
    }

    private class ExtendedOCSCommandExecutor extends OCSCommandExecutor {

        public ExtendedOCSCommandExecutor(CCS ccs) {
            super(ccs);
        }

        @Override
        protected void reportComplete(OCSCommand command) {
            super.reportComplete(command);
            if (command.getCmdId() != 0 && runThread != null) {
                command.ackCommand(mgr, SAL_camera.SAL__CMD_COMPLETE, 0, "Done : OK");
            }

        }

        @Override
        protected void reportError(OCSCommand command, Exception ex) {
            super.reportError(command, ex);
            if (command.getCmdId() != 0 && runThread != null) {
                command.ackCommand(mgr, SAL_camera.SAL__CMD_FAILED, 0, "Error : " + ex.getMessage());
            }
        }

        @Override
        protected void acknowledgeCommand(OCSCommand command, Duration timeout) {
            super.acknowledgeCommand(command, timeout);
            if (command.getCmdId() != 0 && runThread != null) {
                command.ackCommand(mgr, SAL_camera.SAL__CMD_INPROGRESS, (int) timeout.getSeconds(), "Ack : OK");
            }
        }

        @Override
        protected void rejectCommand(OCSCommand command, String reason) {
            super.rejectCommand(command, reason);
            if (command.getCmdId() != 0 && runThread != null) {
                command.ackCommand(mgr, SAL_camera.SAL__CMD_NOACK, 0, "Ack : NO");
            }
        }
    }
}
