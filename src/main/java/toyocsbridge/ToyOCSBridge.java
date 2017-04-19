package toyocsbridge;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.lsst.sal.camera.DisableCommand;
import org.lsst.sal.camera.EnableCommand;
import org.lsst.sal.camera.EnterControlCommand;
import org.lsst.sal.camera.ExitControlCommand;
import org.lsst.sal.camera.InitGuidersCommand;
import org.lsst.sal.camera.InitImageCommand;
import org.lsst.sal.camera.SetFilterCommand;
import org.lsst.sal.camera.StandbyCommand;
import org.lsst.sal.camera.StartCommand;
import org.lsst.sal.camera.SummaryStateEvent.LSE209State;
import org.lsst.sal.camera.TakeImagesCommand;
import toyocsbridge.OCSCommandExecutor.CCSCommand;
import toyocsbridge.OCSCommandExecutor.OCSExecutor;
import toyocsbridge.OCSCommandExecutor.PreconditionsNotMet;

/**
 * This is a toy for experimenting with the OCS event behaviour. It is not real
 * CCS code and is not intended to become real CCS code.
 *
 * @author tonyj
 */
public class ToyOCSBridge {

    enum TakeImageReadinessState {

        READY, NOT_READY, GETTING_READY
    };

    // Note: order of declaration determines order of status boxes in GUI.
    private final CCS ccs = new CCS();
    private final State lse209State = new State(ccs, LSE209State.OFFLINE_PUBLISH_ONLY);
    private OCSCommandExecutor ocs = new OCSCommandExecutor(ccs);
    private final State takeImageReadinessState = new State(ccs, TakeImageReadinessState.NOT_READY);
    private final Shutter shutter = new Shutter(ccs);
    private final Rafts rafts = new Rafts(ccs);
    private final Filter fcs = new Filter(ccs);
    private ScheduledFuture<?> startImageTimeout;

    public ToyOCSBridge() {
        // We are ready to take an image only if the rafts have been cleared, and the shutter
        // has been prepared.
        ccs.addStateChangeListener((state, oldState) -> {
            AggregateStatus as = ccs.getAggregateStatus();
            if (as.hasState(Rafts.RaftsState.QUIESCENT, Shutter.ShutterReadinessState.READY)) {
                takeImageReadinessState.setState(TakeImageReadinessState.READY);
            } else if (!as.hasState(TakeImageReadinessState.GETTING_READY)) {
                takeImageReadinessState.setState(TakeImageReadinessState.NOT_READY);
            }
        });
    }

    /**
     * Allow a user to provide an alternative implementation of the
     * OCSCommandExecutor. Used to override the default OCSCommandExecutor with
     * one that actually sends acknowledgments back to OCS.
     *
     * @param ocs
     */
    void setExecutor(OCSCommandExecutor ocs) {
        this.ocs = ocs;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ToyOCSBridge ocs = new ToyOCSBridge();
        ToyOCSGUI gui = new ToyOCSGUI(ocs);
        gui.setVisible(true);
    }

    void execute(InitImageCommand command) {
        OCSExecutor initImage = new InitImageExecutor(command);
        ocs.executeCommand(initImage);
    }

    void execute(SetFilterCommand command) {
        OCSExecutor setFilter = new SetFilterExecutor(command);
        ocs.executeCommand(setFilter);
    }

    void execute(TakeImagesCommand command) {
        OCSExecutor takeImages = new TakeImagesExecutor(command);
        ocs.executeCommand(takeImages);
    }

    void execute(InitGuidersCommand command) {
        OCSExecutor initGuiders = new InitGuidersExecutor(command);
        ocs.executeCommand(initGuiders);
    }

//    void clear(int nClears) {
//        OCSExecutor clear = new Clear(nClears);
//        ocs.executeCommand(clear);
//    }
//
//    void startImage(int cmdId, String visitName, boolean openShutter, boolean science, boolean wavefront, boolean guider, double timeout) {
//        OCSExecutor startImage = new StartImage(cmdId, visitName, openShutter, science, wavefront, guider, timeout);
//        ocs.executeCommand(startImage);
//    }
//
//    void endImage(int cmdId) {
//        OCSExecutor endImage = new EndImage(cmdId);
//        ocs.executeCommand(endImage);
//    }
//
//    void discardRows(int cmdId, int nRows) {
//        OCSExecutor discardRows = new DiscardRows(cmdId, nRows);
//        ocs.executeCommand(discardRows);
//    }

    void execute(EnterControlCommand command) {
        OCSExecutor takeControl = new EnterControlExecutor(command);
        ocs.executeCommand(takeControl);
    }

    void execute(ExitControlCommand command) {
        OCSExecutor exit = new ExitExecutor(command);
        ocs.executeCommand(exit);
    }

    void execute(StartCommand command) {
        OCSExecutor start = new StartExecutor(command);
        ocs.executeCommand(start);
    }

    void execute(StandbyCommand command) {
        OCSExecutor standby = new StandbyExecutor(command);
        ocs.executeCommand(standby);
    }

    void execute(EnableCommand command) {
        OCSExecutor enable = new EnableExecutor(command);
        ocs.executeCommand(enable);
    }

    void execute(DisableCommand command) {
        OCSExecutor disable = new DisableExecutor(command);
        ocs.executeCommand(disable);
    }

    void setAvailable() {
        CCSCommand setAvailable = new SetAvailableCommand();
        ocs.executeCommand(setAvailable);
    }

    void revokeAvailable() {
        CCSCommand revokeAvailable = new RevokeAvailableCommand();
        ocs.executeCommand(revokeAvailable);
    }

    void simulateFault() {
        CCSCommand simulateFault = new SimulateFaultCommand();
        ocs.executeCommand(simulateFault);
    }

    void clearFault() {
        CCSCommand clearFault = new ClearFaultCommand();
        ocs.executeCommand(clearFault);
    }

    public Filter getFCS() {
        return fcs;
    }

    CCS getCCS() {
        return ccs;
    }

    class InitImageExecutor extends OCSExecutor {

        private final InitImageCommand command;

        public InitImageExecutor(InitImageCommand command) {
            super(command);
            this.command = command;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (command.getDeltaT() <= 0 || command.getDeltaT() > 15) {
                throw new PreconditionsNotMet("Invalid deltaT: " + command.getDeltaT());
            }
            if (startImageTimeout != null && !startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("Exposure in progress");
            }
            return Duration.ZERO;
        }

        @Override
        void execute() {
            Duration takeImagesExpected = Duration.ofMillis((long) (command.getDeltaT() * 1000));
            takeImageReadinessState.setState(TakeImageReadinessState.GETTING_READY);
            ccs.schedule(takeImagesExpected.minus(Rafts.CLEAR_TIME), () -> {
                rafts.clear(1);
            });
            ccs.schedule(takeImagesExpected.minus(Shutter.PREP_TIME), () -> {
                shutter.prepare();
            });
        }

    }

    class TakeImagesExecutor extends OCSExecutor {

        private final TakeImagesCommand command;

        public TakeImagesExecutor(TakeImagesCommand command) {
            super(command);
            this.command = command;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (command.getNumImages() <= 0 || command.getNumImages() > 10 || command.getExpTime() < 1 || command.getExpTime() > 30) {
                throw new PreconditionsNotMet("Invalid argument");
            }
            if (startImageTimeout != null && !startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("Exposure in progress");
            }
            // Worse case estimate
            return Duration.ofMillis((long) (command.getExpTime() * 1000)).plus(Shutter.MOVE_TIME).plus(Rafts.READOUT_TIME).multipliedBy(command.getNumImages());
        }

        @Override
        void execute() throws InterruptedException, ExecutionException, TimeoutException {
            Duration exposeTime = Duration.ofMillis((long) (command.getExpTime() * 1000));
            for (int i = 0; i < command.getNumImages(); i++) {
                Future waitUntilReady = ccs.waitForStatus(TakeImageReadinessState.READY);
                // FIXME: It is not necessarily necessary tp always do a clear _AND_ prepare
                if (takeImageReadinessState.isInState(TakeImageReadinessState.NOT_READY)) {
                    rafts.clear(1);
                    shutter.prepare();
                }

                waitUntilReady.get(1, TimeUnit.SECONDS);
                if (command.isShutter()) {
                    shutter.expose(exposeTime);
                    rafts.expose(exposeTime.plus(Shutter.MOVE_TIME));
                    // For the last exposure we only wait until the readout starts
                    // For other exposures we must wait until readout is complete
                    Future waitUntilDone = ccs.waitForStatus(i + 1 < command.getNumImages() ? Rafts.RaftsState.QUIESCENT : Rafts.RaftsState.READING_OUT);
                    waitUntilDone.get(exposeTime.plus(Shutter.MOVE_TIME).plus(Rafts.READOUT_TIME).plusSeconds(1).toMillis(), TimeUnit.MILLISECONDS);
                } else {
                    rafts.expose(exposeTime);
                    Future waitUntilDone = ccs.waitForStatus(i + 1 < command.getNumImages() ? Rafts.RaftsState.QUIESCENT : Rafts.RaftsState.READING_OUT);
                    waitUntilDone.get(exposeTime.plus(Shutter.MOVE_TIME).plus(Rafts.READOUT_TIME).plusSeconds(1).toMillis(), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    class SetFilterExecutor extends OCSExecutor {

        private final SetFilterCommand command;

        public SetFilterExecutor(SetFilterCommand command) {
            super(command);
            this.command = command;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (startImageTimeout != null && !startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("Exposure in progress");
            }
            if (!fcs.filterIsAvailable(command.getFilterName())) {
                throw new PreconditionsNotMet("Invalid filter: " + command.getFilterName());
            }
            // Worse case
            return Filter.ROTATION_TIME_PER_DEGREE.multipliedBy(360).plus(Filter.LOAD_TIME).plus(Filter.UNLOAD_TIME);
        }

        @Override
        void execute() throws Exception {
            fcs.setFilter(command.getFilterName());
        }
    }

    private class InitGuidersExecutor extends OCSExecutor {

        private final InitGuidersCommand command;

        public InitGuidersExecutor(InitGuidersCommand command) {
            super(command);
            this.command = command;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            return Duration.ZERO;
        }

        @Override
        void execute() {
            // FIXME: Currently this does not actually do anything
        }
    }

//    private class Clear extends OCSExecutor {
//
//        public Clear(int cmdId, int nClears) {
//            super(cmdId);
//            this.nClears = nClears;
//        }
//
//        @Override
//        Duration testPreconditions() throws PreconditionsNotMet {
//            if (!lse209State.isInState(LSE209State.ENABLED)) {
//                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
//            }
//            if (nClears <= 0 || nClears > 15) {
//                throw new PreconditionsNotMet("Invalid nClears: " + nClears);
//            }
//            return Rafts.CLEAR_TIME.multipliedBy(nClears);
//        }
//
//        @Override
//        void execute() throws InterruptedException, ExecutionException, TimeoutException {
//            rafts.clear(nClears);
//            // TODO: Note, unlike initImages, the clear command remains active until the clears are complete (Correct?)
//            Future waitUntilClear = ccs.waitForStatus(Rafts.RaftsState.QUIESCENT);
//            waitUntilClear.get(Rafts.CLEAR_TIME.multipliedBy(nClears).plus(Duration.ofSeconds(1)).toMillis(), TimeUnit.MILLISECONDS);
//        }
//
//        @Override
//        public String toString() {
//            return "Clear(" + getCmdId() + "){" + "nClears=" + nClears + '}';
//        }
//
//        @Override
//        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
////            mgr.ackCommand_clear(getCmdId(), response, timeout, message);
//        }
//    }
//
//    private class StartImage extends OCSExecutor {
//
//        private final String visitName;
//        private final boolean openShutter;
//        private final boolean science;
//        private final boolean wavefront;
//        private final boolean guider;
//        private final double timeout;
//
//        public StartImage(int cmdId, String visitName, boolean openShutter, boolean science, boolean wavefront, boolean guider, double timeout) {
//            super(cmdId);
//            this.visitName = visitName;
//            this.openShutter = openShutter;
//            this.science = science;
//            this.wavefront = wavefront;
//            this.guider = guider;
//            this.timeout = timeout;
//        }
//
//        @Override
//        Duration testPreconditions() throws PreconditionsNotMet {
//            if (!lse209State.isInState(LSE209State.ENABLED)) {
//                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
//            }
//            if (timeout < 1 | timeout > 120) {
//                throw new PreconditionsNotMet("Invalid argument");
//            }
//            if (startImageTimeout != null && !startImageTimeout.isDone()) {
//                throw new PreconditionsNotMet("Exposure in progress");
//            }
//            return Duration.ofSeconds(1);
//        }
//
//        @Override
//        void execute() throws Exception {
//            Future waitUntilReady = ccs.waitForStatus(TakeImageReadinessState.READY);
//            // FIXME: It is not necessary to always clear and prepare the shutter, especially
//            // if we are not actually going to open the shutter.
//            if (takeImageReadinessState.isInState(TakeImageReadinessState.NOT_READY)) {
//                rafts.clear(1);
//                shutter.prepare();
//            }
//
//            waitUntilReady.get(1, TimeUnit.SECONDS);
//            if (openShutter) {
//                shutter.open();
//                rafts.startExposure();
//                // FIXME: Wait for shutter to open? right now we return immediately
//            } else {
//                rafts.startExposure();
//            }
//            startImageTimeout = ccs.schedule(Duration.ofMillis((long) (timeout * 1000)), () -> {
//                imageTimeout();
//            });
//        }
//
//        @Override
//        public String toString() {
//            return "StartImage(" + getCmdId() + "){" + "visitName=" + visitName + ", openShutter=" + openShutter + ", science=" + science + ", wavefront=" + wavefront + ", guider=" + guider + ", timeout=" + timeout + '}';
//        }
//
//        @Override
//        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
////            mgr.ackCommand_startImage(getCmdId(), response, timeout, message);
//        }
//
//    }
//
//    /**
//     * Called if the timeout for a takeImages occurs
//     */
//    private void imageTimeout() {
//        // FIXME: Is this a NOOP if the shutter is already closed?
//        shutter.close();
//        rafts.endExposure(false);
//    }
//
//    private class EndImage extends OCSExecutor {
//
//        public EndImage(int cmdId) {
//            super(cmdId);
//        }
//
//        @Override
//        Duration testPreconditions() throws PreconditionsNotMet {
//            if (!lse209State.isInState(LSE209State.ENABLED)) {
//                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
//            }
//            if (startImageTimeout == null || startImageTimeout.isDone()) {
//                throw new PreconditionsNotMet("No exposure in progress");
//            }
//            return Shutter.MOVE_TIME;
//        }
//
//        @Override
//        void execute() throws Exception {
//            if (!startImageTimeout.cancel(false)) {
//                throw new RuntimeException("Image exposure already timed out");
//            }
//            Future waitUntilClosed = ccs.waitForStatus(ShutterState.CLOSED);
//            shutter.close();
//            waitUntilClosed.get(Shutter.MOVE_TIME.plus(Duration.ofSeconds(1)).toMillis(), TimeUnit.MILLISECONDS);
//            rafts.endExposure(true);
//        }
//
//        @Override
//        public String toString() {
//            return "EndImage(" + getCmdId() + "){" + '}';
//        }
//
//        @Override
//        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
////            mgr.ackCommand_endImage(getCmdId(), response, timeout, message);
//        }
//    }
//
//    private class DiscardRows extends OCSExecutor {
//
//        private final int nRows;
//
//        public DiscardRows(int cmdId, int nRows) {
//            super(cmdId);
//            this.nRows = nRows;
//        }
//
//        @Override
//        Duration testPreconditions() throws PreconditionsNotMet {
//            if (!lse209State.isInState(LSE209State.ENABLED)) {
//                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
//            }
//            if (startImageTimeout == null || startImageTimeout.isDone()) {
//                throw new PreconditionsNotMet("No exposure in progress");
//            }
//            return Duration.ZERO;
//        }
//
//        @Override
//        void execute() throws Exception {
//            // FIXME: Nothing actually happens, should at least generate some events.
//        }
//
//        @Override
//        public String toString() {
//            return "DiscardRows(" + getCmdId() + "){" + "nRows=" + nRows + '}';
//        }
//
//        @Override
//        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
////            mgr.ackCommand_discardRows(getCmdId(), response, timeout, message);
//        }
//
//    }

    class EnterControlExecutor extends OCSExecutor {

        public EnterControlExecutor(EnterControlCommand command) {
            super(command);
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.OFFLINE_AVAILABLE)) {
                throw new PreconditionsNotMet("Command not accepted in " + lse209State);
            }
            return Duration.ZERO;
        }

        @Override
        void execute() throws Exception {
            lse209State.setState(LSE209State.STANDBY);
        }
    }

    class ExitExecutor extends OCSExecutor {

        public ExitExecutor(ExitControlCommand command) {
            super(command);
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.STANDBY)) {
                throw new PreconditionsNotMet("Command not accepted in " + lse209State);
            }
            return Duration.ZERO;
        }

        @Override
        void execute() throws Exception {
            lse209State.setState(LSE209State.OFFLINE_PUBLISH_ONLY);
        }
    }

    class StartExecutor extends OCSExecutor {

        public StartExecutor(StartCommand command) {
            super(command);
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.STANDBY)) {
                throw new PreconditionsNotMet("Command not accepted in " + lse209State);
            }
            return Duration.ZERO;
        }

        @Override
        void execute() throws Exception {
            //TODO: Set the configuration
            lse209State.setState(LSE209State.DISABLED);
        }
    }

    class StandbyExecutor extends OCSExecutor {

        public StandbyExecutor(StandbyCommand command) {
            super(command);
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.DISABLED)) {
                throw new PreconditionsNotMet("Command not accepted in " + lse209State);
            }
            return Duration.ZERO;
        }

        @Override
        void execute() throws Exception {
            //TODO: should we reject the standy command if things are happening?
            //TODO: or wait until things finish and return then?
            lse209State.setState(LSE209State.STANDBY);
        }
    }

    class EnableExecutor extends OCSExecutor {

        public EnableExecutor(EnableCommand command) {
            super(command);
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.DISABLED)) {
                throw new PreconditionsNotMet("Command not accepted in " + lse209State);
            }
            return Duration.ZERO;
        }

        @Override
        void execute() throws Exception {
            lse209State.setState(LSE209State.ENABLED);
        }
    }

    class DisableExecutor extends OCSExecutor {

        public DisableExecutor(DisableCommand command) {
            super(command);
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in " + lse209State);
            }
            // Fixme: Can we reject the disable command if we are busy?
            // What about if we are not idle?
            // Note logic here is incorrect according to Paul Lotz, we must always accept
            // the disable command.
            if (startImageTimeout != null && !startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("Exposure in progress");
            }
            return Duration.ZERO;
        }

        @Override
        void execute() throws Exception {
            //TODO: should we reject the standy command if things are happening?
            //TODO: or wait until things finish and return then?
            lse209State.setState(LSE209State.DISABLED);
        }
    }

    class SetAvailableCommand extends CCSCommand {

        @Override
        void testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.OFFLINE_PUBLISH_ONLY)) {
                throw new PreconditionsNotMet("Command not accepted in " + lse209State);
            }
        }

        @Override
        void execute() throws Exception {
            lse209State.setState(LSE209State.OFFLINE_AVAILABLE);
        }

    }

    class RevokeAvailableCommand extends CCSCommand {

        @Override
        void testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.OFFLINE_AVAILABLE)) {
                throw new PreconditionsNotMet("Command not accepted in " + lse209State);
            }
        }

        @Override
        void execute() throws Exception {
            lse209State.setState(LSE209State.OFFLINE_PUBLISH_ONLY);
        }

    }

    class SimulateFaultCommand extends CCSCommand {

        @Override
        void testPreconditions() throws PreconditionsNotMet {

        }

        @Override
        void execute() throws Exception {
            //TODO: Should we also attempt to stop the subsystems?
            lse209State.setState(LSE209State.FAULT);
        }

    }

    class ClearFaultCommand extends CCSCommand {

        @Override
        void testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.FAULT)) {
                throw new PreconditionsNotMet("Command not accepted in " + lse209State);
            }
        }

        @Override
        void execute() throws Exception {
            lse209State.setState(LSE209State.OFFLINE_PUBLISH_ONLY);
        }

    }
}
