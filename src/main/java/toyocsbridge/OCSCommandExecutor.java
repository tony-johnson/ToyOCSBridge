package toyocsbridge;

import java.util.logging.Level;
import java.util.logging.Logger;

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

    void executeOCSCommand(OCSCommand command) {
        if (!commandState.isInState(CommandState.IDLE)) {
            rejectCommand(command, "Command state not idle");
        } else {
            try {
                command.testPreconditions();
                commandState.setState(CommandState.BUSY);
                acknowledgeCommand(command);
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

    protected void rejectCommand(OCSCommand command, String reason) {
        logger.log(Level.INFO, "Reject command: {0} because {1}", new Object[]{command.getClass().getSimpleName(), reason});
    }

    protected void acknowledgeCommand(OCSCommand command) {
        logger.log(Level.INFO, "Acknowledge command: {0}", command.getClass().getSimpleName());
    }

    protected void reportError(OCSCommand command, Exception ex) {
        logger.log(Level.WARNING, "Command failed: " + command.getClass().getSimpleName(), ex);
    }

    protected void reportComplete(OCSCommand command) {
        logger.log(Level.INFO, "Command complete: {0}", command.getClass().getSimpleName());
    }

    /**
     * A base class for all OCS commands
     *
     * @author tonyj
     */
    public static abstract class OCSCommand {
        private final int cmdId;

        OCSCommand(int cmdId) {
            this.cmdId = cmdId;
        }
        /**
         * Must return true for the command to be accepted.
         *
         * @return
         */
        abstract void testPreconditions() throws PreconditionsNotMet;

        /**
         * Actually perform the command
         */
        abstract void execute() throws Exception;

        public int getCmdId() {
            return cmdId;
        }
    }

    static class PreconditionsNotMet extends Exception {

        private static final long serialVersionUID = 1L;

        public PreconditionsNotMet(String reason) {
            super(reason);
        }
    }
}
