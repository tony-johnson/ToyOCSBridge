package toyocsbridge;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.lsst.sal.SAL_camera;
import toyocsbridge.OCSCommandExecutor.CCSCommand;
import toyocsbridge.OCSCommandExecutor.OCSCommand;
import toyocsbridge.OCSCommandExecutor.PreconditionsNotMet;
import toyocsbridge.Shutter.ShutterState;

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

    public enum LSE209State {
        OFFLINE_PUBLISH_ONLY, OFFLINE_AVAILABLE, STANDBY, DISABLED, ENABLED, FAULT
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

    void initImage(int cmdId, double deltaT) {
        OCSCommand initImage = new InitImageCommand(cmdId, deltaT);
        ocs.executeCommand(initImage);
    }

    void setFilter(int cmdId, String filterName) {
        OCSCommand setFilter = new SetFilterCommand(cmdId, filterName);
        ocs.executeCommand(setFilter);
    }

    void takeImages(int cmdId, double exposure, int nImages, boolean openShutter, boolean science, boolean wavefront, boolean guider, String visitName) {
        OCSCommand takeImages = new TakeImagesCommand(cmdId, exposure, nImages, openShutter, science, wavefront, guider, visitName);
        ocs.executeCommand(takeImages);
    }

    void initGuiders(int cmdId, String roiSpec) {
        OCSCommand initGuiders = new InitGuiders(cmdId, roiSpec);
        ocs.executeCommand(initGuiders);
    }

    void clear(int cmdId, int nClears) {
        OCSCommand clear = new Clear(cmdId, nClears);
        ocs.executeCommand(clear);
    }

    void startImage(int cmdId, String visitName, boolean openShutter, boolean science, boolean wavefront, boolean guider, double timeout) {
        OCSCommand startImage = new StartImage(cmdId, visitName, openShutter, science, wavefront, guider, timeout);
        ocs.executeCommand(startImage);
    }

    void endImage(int cmdId) {
        OCSCommand endImage = new EndImage(cmdId);
        ocs.executeCommand(endImage);
    }

    void discardRows(int cmdId, int nRows) {
        OCSCommand discardRows = new DiscardRows(cmdId, nRows);
        ocs.executeCommand(discardRows);
    }

    void enterControl(int cmdId) {
        OCSCommand takeControl = new EnterControlCommand(cmdId);
        ocs.executeCommand(takeControl);
    }

    void exitControl(int cmdId) {
        OCSCommand exit = new ExitCommand(cmdId);
        ocs.executeCommand(exit);
    }

    void start(int cmdId, String configuration) {
        OCSCommand start = new StartCommand(cmdId, configuration);
        ocs.executeCommand(start);
    }

    void standby(int cmdId) {
        OCSCommand standby = new StandbyCommand(cmdId);
        ocs.executeCommand(standby);
    }

    void enable(int cmdId) {
        OCSCommand enable = new EnableCommand(cmdId);
        ocs.executeCommand(enable);
    }

    void disable(int cmdId) {
        OCSCommand disable = new DisableCommand(cmdId);
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

    class InitImageCommand extends OCSCommand {

        private final double deltaT;

        public InitImageCommand(int cmdId, double deltaT) {
            super(cmdId);
            this.deltaT = deltaT;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (deltaT <= 0 || deltaT > 15) {
                throw new PreconditionsNotMet("Invalid deltaT: " + deltaT);
            }
            if (startImageTimeout != null && !startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("Exposure in progress");
            }
            return Duration.ZERO;
        }

        @Override
        void execute() {
            Duration takeImagesExpected = Duration.ofMillis((long) (deltaT * 1000));
            takeImageReadinessState.setState(TakeImageReadinessState.GETTING_READY);
            ccs.schedule(takeImagesExpected.minus(Rafts.CLEAR_TIME), () -> {
                rafts.clear(1);
            });
            ccs.schedule(takeImagesExpected.minus(Shutter.PREP_TIME), () -> {
                shutter.prepare();
            });
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_initImage(getCmdId(), response, timeout, message);
        }
    }

    class TakeImagesCommand extends OCSCommand {

        private final double exposure;
        private final int nImages;
        private final boolean openShutter;
        private final boolean science;
        private final boolean wavefront;
        private final boolean guider;
        private final String visitName;

        public TakeImagesCommand(int cmdId, double exposure, int nImages, boolean openShutter, boolean science, boolean wavefront, boolean guider, String visitName) {
            super(cmdId);
            this.exposure = exposure;
            this.nImages = nImages;
            this.openShutter = openShutter;
            this.science = science;
            this.wavefront = wavefront;
            this.guider = guider;
            this.visitName = visitName;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (nImages <= 0 || nImages > 10 || exposure < 1 || exposure > 30) {
                throw new PreconditionsNotMet("Invalid argument");
            }
            if (startImageTimeout != null && !startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("Exposure in progress");
            }
            // Worse case estimate
            return Duration.ofMillis((long) (exposure * 1000)).plus(Shutter.MOVE_TIME).plus(Rafts.READOUT_TIME).multipliedBy(nImages);
        }

        @Override
        void execute() throws InterruptedException, ExecutionException, TimeoutException {
            Duration exposeTime = Duration.ofMillis((long) (exposure * 1000));
            for (int i = 0; i < nImages; i++) {
                Future waitUntilReady = ccs.waitForStatus(TakeImageReadinessState.READY);
                // FIXME: It is not necessarily necessary tp always do a clear _AND_ prepare
                if (takeImageReadinessState.isInState(TakeImageReadinessState.NOT_READY)) {
                    rafts.clear(1);
                    shutter.prepare();
                }

                waitUntilReady.get(1, TimeUnit.SECONDS);
                if (openShutter) {
                    shutter.expose(exposeTime);
                    rafts.expose(exposeTime.plus(Shutter.MOVE_TIME));
                    // For the last exposure we only wait until the readout starts
                    // For other exposures we must wait until readout is complete
                    Future waitUntilDone = ccs.waitForStatus(i + 1 < nImages ? Rafts.RaftsState.QUIESCENT : Rafts.RaftsState.READING_OUT);
                    waitUntilDone.get(exposeTime.plus(Shutter.MOVE_TIME).plus(Rafts.READOUT_TIME).plusSeconds(1).toMillis(), TimeUnit.MILLISECONDS);
                } else {
                    rafts.expose(exposeTime);
                    Future waitUntilDone = ccs.waitForStatus(i + 1 < nImages ? Rafts.RaftsState.QUIESCENT : Rafts.RaftsState.READING_OUT);
                    waitUntilDone.get(exposeTime.plus(Shutter.MOVE_TIME).plus(Rafts.READOUT_TIME).plusSeconds(1).toMillis(), TimeUnit.MILLISECONDS);
                }
            }
        }

        @Override
        public String toString() {
            return "TakeImagesCommand("+getCmdId()+"){" + "exposure=" + exposure + ", nImages=" + nImages + ", openShutter=" + openShutter + ", science=" + science + ", wavefront=" + wavefront + ", guider=" + guider + ", visitName=" + visitName + '}';
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_takeImages(getCmdId(), response, timeout, message);
        }

    }

    class SetFilterCommand extends OCSCommand {

        private final String filter;

        public SetFilterCommand(int cmdId, String filter) {
            super(cmdId);
            this.filter = filter;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (startImageTimeout != null && !startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("Exposure in progress");
            }
            if (!fcs.filterIsAvailable(filter)) {
                throw new PreconditionsNotMet("Invalid filter: " + filter);
            }
            // Worse case
            return Filter.ROTATION_TIME_PER_DEGREE.multipliedBy(360).plus(Filter.LOAD_TIME).plus(Filter.UNLOAD_TIME);
        }

        @Override
        void execute() throws Exception {
            fcs.setFilter(filter);
        }

        @Override
        public String toString() {
            return "SetFilterCommand("+getCmdId()+"){" + "filter=" + filter + '}';
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_setFilter(getCmdId(), response, timeout, message);
        }

    }

    private class InitGuiders extends OCSCommand {

        private final String roiSpec;

        public InitGuiders(int cmdId, String roiSpec) {
            super(cmdId);
            this.roiSpec = roiSpec;
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

        @Override
        public String toString() {
            return "InitGuiders("+getCmdId()+"){" + "roiSpec=" + roiSpec + '}';
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_initGuiders(getCmdId(), response, timeout, message);
        }

    }

    private class Clear extends OCSCommand {

        private final int nClears;

        public Clear(int cmdId, int nClears) {
            super(cmdId);
            this.nClears = nClears;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (nClears <= 0 || nClears > 15) {
                throw new PreconditionsNotMet("Invalid nClears: " + nClears);
            }
            return Rafts.CLEAR_TIME.multipliedBy(nClears);
        }

        @Override
        void execute() throws InterruptedException, ExecutionException, TimeoutException {
            rafts.clear(nClears);
            // TODO: Note, unlike initImages, the clear command remains active until the clears are complete (Correct?)
            Future waitUntilClear = ccs.waitForStatus(Rafts.RaftsState.QUIESCENT);
            waitUntilClear.get(Rafts.CLEAR_TIME.multipliedBy(nClears).plus(Duration.ofSeconds(1)).toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public String toString() {
            return "Clear("+getCmdId()+"){" + "nClears=" + nClears + '}';
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
//            mgr.ackCommand_clear(getCmdId(), response, timeout, message);
        }
    }

    private class StartImage extends OCSCommand {

        private final String visitName;
        private final boolean openShutter;
        private final boolean science;
        private final boolean wavefront;
        private final boolean guider;
        private final double timeout;

        public StartImage(int cmdId, String visitName, boolean openShutter, boolean science, boolean wavefront, boolean guider, double timeout) {
            super(cmdId);
            this.visitName = visitName;
            this.openShutter = openShutter;
            this.science = science;
            this.wavefront = wavefront;
            this.guider = guider;
            this.timeout = timeout;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (timeout < 1 | timeout > 120) {
                throw new PreconditionsNotMet("Invalid argument");
            }
            if (startImageTimeout != null && !startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("Exposure in progress");
            }
            return Duration.ofSeconds(1);
        }

        @Override
        void execute() throws Exception {
            Future waitUntilReady = ccs.waitForStatus(TakeImageReadinessState.READY);
            // FIXME: It is not necessary to always clear and prepare the shutter, especially
            // if we are not actually going to open the shutter.
            if (takeImageReadinessState.isInState(TakeImageReadinessState.NOT_READY)) {
                rafts.clear(1);
                shutter.prepare();
            }

            waitUntilReady.get(1, TimeUnit.SECONDS);
            if (openShutter) {
                shutter.open();
                rafts.startExposure();
                // FIXME: Wait for shutter to open? right now we return immediately
            } else {
                rafts.startExposure();
            }
            startImageTimeout = ccs.schedule(Duration.ofMillis((long) (timeout * 1000)), () -> {
                imageTimeout();
            });
        }

        @Override
        public String toString() {
            return "StartImage("+getCmdId()+"){" + "visitName=" + visitName + ", openShutter=" + openShutter + ", science=" + science + ", wavefront=" + wavefront + ", guider=" + guider + ", timeout=" + timeout + '}';
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
//            mgr.ackCommand_startImage(getCmdId(), response, timeout, message);
        }

    }

    /**
     * Called if the timeout for a takeImages occurs
     */
    private void imageTimeout() {
        // FIXME: Is this a NOOP if the shutter is already closed?
        shutter.close();
        rafts.endExposure(false);
    }

    private class EndImage extends OCSCommand {

        public EndImage(int cmdId) {
            super(cmdId);
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (startImageTimeout == null || startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("No exposure in progress");
            }
            return Shutter.MOVE_TIME;
        }

        @Override
        void execute() throws Exception {
            if (!startImageTimeout.cancel(false)) {
                throw new RuntimeException("Image exposure already timed out");
            }
            Future waitUntilClosed = ccs.waitForStatus(ShutterState.CLOSED);
            shutter.close();
            waitUntilClosed.get(Shutter.MOVE_TIME.plus(Duration.ofSeconds(1)).toMillis(), TimeUnit.MILLISECONDS);
            rafts.endExposure(true);
        }

        @Override
        public String toString() {
            return "EndImage("+getCmdId()+"){" + '}';
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
//            mgr.ackCommand_endImage(getCmdId(), response, timeout, message);
        }
    }

    private class DiscardRows extends OCSCommand {

        private final int nRows;

        public DiscardRows(int cmdId, int nRows) {
            super(cmdId);
            this.nRows = nRows;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
            if (!lse209State.isInState(LSE209State.ENABLED)) {
                throw new PreconditionsNotMet("Command not accepted in: " + lse209State);
            }
            if (startImageTimeout == null || startImageTimeout.isDone()) {
                throw new PreconditionsNotMet("No exposure in progress");
            }
            return Duration.ZERO;
        }

        @Override
        void execute() throws Exception {
            // FIXME: Nothing actually happens, should at least generate some events.
        }

        @Override
        public String toString() {
            return "DiscardRows("+getCmdId()+"){" + "nRows=" + nRows + '}';
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
//            mgr.ackCommand_discardRows(getCmdId(), response, timeout, message);
        }

    }

    class EnterControlCommand extends OCSCommand {

        public EnterControlCommand(int cmdId) {
            super(cmdId);
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

        @Override
        public String toString() {
            return "EnterControlCommand("+getCmdId()+")";
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_enterControl(getCmdId(), response, timeout, message);
        }
    }

    class ExitCommand extends OCSCommand {

        public ExitCommand(int cmdId) {
            super(cmdId);
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

        @Override
        public String toString() {
            return "ExitCommand("+getCmdId()+")";
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_exitControl(getCmdId(), response, timeout, message);
        }
    }

    class StartCommand extends OCSCommand {

        private final String configuration;

        public StartCommand(int cmdId, String configuration) {
            super(cmdId);
            this.configuration = configuration;
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

        @Override
        public String toString() {
            return "StartCommand("+getCmdId()+"){" + "configuration=" + configuration + '}';
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_start(getCmdId(), response, timeout, message);
        }
    }

    class StandbyCommand extends OCSCommand {

        public StandbyCommand(int cmdId) {
            super(cmdId);
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

        @Override
        public String toString() {
            return "StandbyCommand("+getCmdId()+")";
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_standby(getCmdId(), response, timeout, message);
        }
    }

    class EnableCommand extends OCSCommand {

        public EnableCommand(int cmdId) {
            super(cmdId);
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

        @Override
        public String toString() {
            return "EnabledCommand("+getCmdId()+")";
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_enable(getCmdId(), response, timeout, message);            
        }
    }

    class DisableCommand extends OCSCommand {

        public DisableCommand(int cmdId) {
            super(cmdId);
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

        @Override
        public String toString() {
            return "DisableCommand("+getCmdId()+")";
        }

        @Override
        void ackCommand(SAL_camera mgr, int response, int timeout, String message) {
            mgr.ackCommand_disable(getCmdId(), response, timeout, message);
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
