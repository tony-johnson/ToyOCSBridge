package toyocsbridge;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lsst.sal.SAL_camera;
import toyocsbridge.State.StateChangeListener;

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
        bridge.getCCS().addStateChangeListener(new StateChangeListener() {

            @Override
            public void stateChanged(State currentState, Enum oldState) {
                // For now send a generic event
                String msg = String.format("State Changed %s: %s->%s", currentState.getClass().getSimpleName(), oldState, currentState);
                int priority = 1;
                mgr.logEvent(msg, priority);
            }
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
                ocsInterface.run();
            }

        };
        t.start();

    }

    void run() {
        runThread = Thread.currentThread();
        // Currently we have to poll for each command
        mgr.salProcessor("camera_command_setFilter");
        camera.command_setFilter setFilterCommand = new camera.command_setFilter();
        mgr.salProcessor("camera_command_takeImages");
        camera.command_takeImages takeImagesCommand = new camera.command_takeImages();
        mgr.salProcessor("camera_command_initImage");
        camera.command_initImage initImageCommand = new camera.command_initImage();
        mgr.salProcessor("camera_command_initImage");
        
        while (!shutdown) {
            int cmdId = mgr.acceptCommand_setFilter(setFilterCommand);
            if (cmdId > 0) {
                bridge.setFilter(cmdId, setFilterCommand.name);
            }
            cmdId = mgr.acceptCommand_takeImages(takeImagesCommand);
            if (cmdId > 0) {
                bridge.takeImages(cmdId, takeImagesCommand.expTime, takeImagesCommand.numImages, takeImagesCommand.shutter);
            }
            cmdId = mgr.acceptCommand_initImage(initImageCommand);
            if (cmdId > 0) {
                bridge.initImage(cmdId, initImageCommand.deltaT);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "Unexpected interruption", ex);
            }
        }
        mgr.salShutdown();
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
            if (command.getCmdId() != 0) {
                mgr.ackCommand_takeImages(command.getCmdId(), SAL_camera.SAL__CMD_COMPLETE, 0, "Done : OK");
            }

        }

        @Override
        protected void reportError(OCSCommand command, Exception ex) {
            super.reportError(command, ex);
            if (command.getCmdId() != 0) {
                mgr.ackCommand_takeImages(command.getCmdId(), SAL_camera.SAL__CMD_FAILED, 0, "Error : " + ex.getMessage());
            }
        }

        @Override
        protected void acknowledgeCommand(OCSCommand command, Duration timeout) {
            super.acknowledgeCommand(command, timeout);
            if (command.getCmdId() != 0) {
                mgr.ackCommand_takeImages(command.getCmdId(), SAL_camera.SAL__CMD_INPROGRESS, (int) timeout.getSeconds(), "Ack : OK");
            }
        }

        @Override
        protected void rejectCommand(OCSCommand command, String reason) {
            super.rejectCommand(command, reason);
            if (command.getCmdId() != 0) {
                mgr.ackCommand_takeImages(command.getCmdId(), SAL_camera.SAL__CMD_NOACK, 0, "Ack : NO");
            }
        }
    }
}
