package toyocsbridge;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lsst.sal.camera.CameraColdTelemetry;
import org.lsst.sal.camera.CameraCommand;
import org.lsst.sal.camera.DisableCommand;
import org.lsst.sal.camera.EnableCommand;
import org.lsst.sal.camera.EnterControlCommand;
import org.lsst.sal.camera.ExitControlCommand;
import org.lsst.sal.camera.GenericEvent;
import org.lsst.sal.camera.SALCamera;
import org.lsst.sal.camera.SALException;
import org.lsst.sal.camera.SetFilterCommand;
import org.lsst.sal.camera.SummaryStateEvent;
import org.lsst.sal.camera.SummaryStateEvent.LSE209State;
import org.lsst.sal.camera.TakeImagesCommand;
import org.lsst.sal.camera.InitImageCommand;
import org.lsst.sal.camera.StandbyCommand;
import org.lsst.sal.camera.StartCommand;

/**
 * Interface to the real OCS.
 *
 * @author tonyj
 */
public class OCSInterface {

    private final SALCamera mgr;
    private boolean shutdown = false;
    private Thread runThread;
    private final ToyOCSBridge bridge;
    private static final Logger logger = Logger.getLogger(OCSInterface.class.getName());

    OCSInterface(ToyOCSBridge bridge) {
        this.bridge = bridge;
        mgr = SALCamera.instance();

        ExtendedOCSCommandExecutor exec = new ExtendedOCSCommandExecutor(bridge.getCCS());
        bridge.setExecutor(exec);
        bridge.getCCS().addStateChangeListener((currentState, oldState) -> {
            String msg = String.format("State Changed %s: %s->%s", currentState.getClass().getSimpleName(), oldState, currentState);
            int priority = 1;
            try {
                if (currentState.getState() instanceof LSE209State) {
                    mgr.logEvent(new SummaryStateEvent(priority, (LSE209State) currentState.getState()));
                } else {
                    // For now send a generic event
                    mgr.logEvent(new GenericEvent(priority, msg));
                }
            } catch (SALException x) {
                logger.log(Level.SEVERE, "Unable to log message", x);
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
                try {
                    ocsInterface.run();
                } catch (Exception x) {
                    logger.log(Level.WARNING, "Failed to initialize OCS communication layer, "
                            + "check that OCS has been setup correctly. Reverting to standalone mode.", x);
                }
            }

        };
        t.start();

        Thread t2 = new Thread("TelemetryGenerator") {
            @Override
            public void run() {
                try {
                    while (true) {

                        Thread.sleep(10000);
                        CameraColdTelemetry cold = new CameraColdTelemetry();
                        fillRandom(cold.getCompressor_load());
                        fillRandom(cold.getCompressor_speed());
                        ocsInterface.mgr.sendTelemetry(cold);
                        logger.log(Level.INFO, "Telemetry sent");
                    }
                } catch (InterruptedException | SALException ex) {
                    logger.log(Level.SEVERE, "Error while sending telemetry", ex);
                } 
            }

            private void fillRandom(float[] compressor_load) {
                for (int i=0; i<compressor_load.length; i++) {
                    
                }
            }

        };
        t2.start();
    }

    @SuppressWarnings("SleepWhileInLoop")
    void run() {
        try {
            runThread = Thread.currentThread();
            // Currently we have to poll for each command

            while (!shutdown) {
                CameraCommand cmd = mgr.getNextCommand(Duration.ofMinutes(1));
                if (cmd == null) {
                    logger.info("Still waiting for a command");
                } else if (cmd instanceof SetFilterCommand) {
                    bridge.execute((SetFilterCommand) cmd);
                } else if (cmd instanceof TakeImagesCommand) {
                    bridge.execute((TakeImagesCommand) cmd);
                } else if (cmd instanceof InitImageCommand) {
                    bridge.execute((InitImageCommand) cmd);
                } else if (cmd instanceof EnableCommand) {
                    bridge.execute((EnableCommand) cmd);
                } else if (cmd instanceof DisableCommand) {
                    bridge.execute((DisableCommand) cmd);
                } else if (cmd instanceof ExitControlCommand) {
                    bridge.execute((ExitControlCommand) cmd);
                } else if (cmd instanceof EnterControlCommand) {
                    bridge.execute((EnterControlCommand) cmd);
                } else if (cmd instanceof StartCommand) {
                    bridge.execute((StartCommand) cmd);
                } else if (cmd instanceof StandbyCommand) {
                    bridge.execute((StandbyCommand) cmd);
                }
            }
            //mgr.salShutdown();
        } catch (SALException ex) {
            logger.log(Level.SEVERE, "Unexpected error while waiting for commands", ex);
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
        protected void reportComplete(OCSExecutor command) {
            super.reportComplete(command);
            command.getCommand().reportComplete();
        }

        @Override
        protected void reportError(OCSExecutor command, Exception ex) {
            super.reportError(command, ex);
            command.getCommand().reportError(ex);
        }

        @Override
        protected void acknowledgeCommand(OCSExecutor command, Duration timeout) {
            super.acknowledgeCommand(command, timeout);
            command.getCommand().acknowledgeCommand(timeout);
        }

        @Override
        protected void rejectCommand(OCSExecutor command, String reason) {
            super.rejectCommand(command, reason);
            command.getCommand().rejectCommand(reason);
        }
    }
}
