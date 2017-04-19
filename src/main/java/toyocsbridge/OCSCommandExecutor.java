package toyocsbridge;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lsst.sal.camera.CameraCommand;

/**
 *
 * @author tonyj
 */
public class OCSCommandExecutor {

    private static final Logger logger = Logger.getLogger(ToyOCSBridge.class.getName());

    enum CommandState {

        IDLE, BUSY
    };
    private final State commandState;

    OCSCommandExecutor(CCS ccs) {
        commandState = new State(ccs, CommandState.IDLE);
    }

    void executeCommand(OCSExecutor command) {
        if (!commandState.isInState(CommandState.IDLE)) {
            rejectCommand(command, "Command state not idle");
        } else {
            try {
                Duration timeout = command.testPreconditions();
                commandState.setState(CommandState.BUSY);
                if (!timeout.isZero()) {
                    acknowledgeCommand(command, timeout);
                }
                command.execute();
                reportComplete(command);
            } catch (PreconditionsNotMet ex) {
                rejectCommand(command, ex.getMessage());
            } catch (Exception ex) {
                reportError(command, ex);
            } finally {
                commandState.setState(CommandState.IDLE);
            }
        }
    }

    void executeCommand(CCSCommand command) {
        // CCS commands do not report their execution to the OCS
        try {
            command.testPreconditions();
            command.execute();
        } catch (PreconditionsNotMet ex) {
            logger.log(Level.INFO, "Reject command: {0} because {1}", new Object[]{command, ex.getMessage()});
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Command failed: " + command, ex);
        }
    }

    protected void rejectCommand(OCSExecutor command, String reason) {
        logger.log(Level.INFO, "Reject command: {0} because {1}", new Object[]{command, reason});
    }

    protected void acknowledgeCommand(OCSExecutor command, Duration timeout) {
        logger.log(Level.INFO, "Acknowledge command: {0} timeout {1}", new Object[]{command, timeout});
    }

    protected void reportError(OCSExecutor command, Exception ex) {
        logger.log(Level.WARNING, "Command failed: " + command, ex);
    }

    protected void reportComplete(OCSExecutor command) {
        logger.log(Level.INFO, "Command complete: {0}", command);
    }

    /**
     * A base class for all OCS command executors
     *
     * @author tonyj
     */
    public static abstract class OCSExecutor {

        private final CameraCommand cmd;

        OCSExecutor(CameraCommand command) {
            this.cmd = command;
        }

        /**
         * Check preconditions, and estimate the command duration.
         *
         * @throws PreconditionsNotMet If the preconditions are not met
         * @return The estimated duration of the command (can be ZERO)
         */
        abstract Duration testPreconditions() throws PreconditionsNotMet;

        /**
         * Actually perform the command
         */
        abstract void execute() throws Exception;

        public int getCmdId() {
            return cmd.getCmdId();
        }
        
        public CameraCommand getCommand() {
            return cmd;
        }

        @Override
        public String toString() {
            return cmd.toString();
        }

    }

    /**
     * A base class for all CCS commands
     *
     * @author tonyj
     */
    public static abstract class CCSCommand {

        CCSCommand() {
        }

        /**
         * Check preconditions, and estimate the command duration.
         *
         * @throws PreconditionsNotMet If the preconditions are not met
         */
        abstract void testPreconditions() throws PreconditionsNotMet;

        /**
         * Actually perform the command
         */
        abstract void execute() throws Exception;
    }

    static class PreconditionsNotMet extends Exception {

        private static final long serialVersionUID = 1L;

        public PreconditionsNotMet(String reason) {
            super(reason);
        }
    }
}
