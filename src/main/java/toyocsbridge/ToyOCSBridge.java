package toyocsbridge;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import toyocsbridge.OCSCommandExecutor.CCSCommand;
import toyocsbridge.OCSCommandExecutor.OCSCommand;
import toyocsbridge.OCSCommandExecutor.PreconditionsNotMet;
import toyocsbridge.State.StateChangeListener;

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

    public ToyOCSBridge() {
        // We are ready to take an image only if the rafts have been cleared, and the shutter
        // has been prepared.
        ccs.addStateChangeListener(new StateChangeListener() {

            @Override
            public void stateChanged(State state, Enum oldState) {
                AggregateStatus as = ccs.getAggregateStatus();
                if (as.hasState(Rafts.RaftsState.QUIESCENT, Shutter.ShutterReadinessState.READY)) {
                    takeImageReadinessState.setState(TakeImageReadinessState.READY);
                } else if (!as.hasState(TakeImageReadinessState.GETTING_READY)) {
                    takeImageReadinessState.setState(TakeImageReadinessState.NOT_READY);
                }
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void clear(int cmdId, int nClears) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void startImage(int cmdId, String visitName, boolean openShutter, boolean science, boolean wavefront, boolean guider, double timeout) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void endImage(int cmdId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void discardRows(int cmdId, int nRows) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void enterControl(int cmdId) {
        OCSCommand takeControl = new EnterControlCommand(cmdId);
        ocs.executeCommand(takeControl);
    }

    void exit(int cmdId) {
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
            return Duration.ZERO;
        }

        @Override
        void execute() {
            Duration takeImagesExpected = Duration.ofMillis((long) (deltaT * 1000));
            takeImageReadinessState.setState(TakeImageReadinessState.GETTING_READY);
            ccs.schedule(takeImagesExpected.minus(Rafts.CLEAR_TIME), () -> {
                rafts.clear();
            });
            ccs.schedule(takeImagesExpected.minus(Shutter.PREP_TIME), () -> {
                shutter.prepare();
            });
        }

        @Override
        public String toString() {
            return "InitImageCommand{" + "deltaT=" + deltaT + '}';
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
            // Worse case estimate
            return Duration.ofMillis((long) (exposure * 1000)).plus(Shutter.MOVE_TIME).plus(Rafts.READOUT_TIME).multipliedBy(nImages);
        }

        @Override
        void execute() throws InterruptedException, ExecutionException, TimeoutException {
            Duration exposeTime = Duration.ofMillis((long) (exposure * 1000));
            for (int i = 0; i < nImages; i++) {
                Future waitUntilReady = ccs.waitForStatus(TakeImageReadinessState.READY);
                if (takeImageReadinessState.isInState(TakeImageReadinessState.NOT_READY)) {
                    rafts.clear();
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
            return "TakeImagesCommand{" + "exposure=" + exposure + ", nImages=" + nImages + ", openShutter=" + openShutter + ", science=" + science + ", wavefront=" + wavefront + ", guider=" + guider + ", visitName=" + visitName + '}';
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
            return "SetFilterCommand{" + "filter=" + filter + '}';
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
            return "EnterControlCommand";
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
            return "ExitCommand";
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
            return "StartCommand{" + "configuration=" + configuration + '}';
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
            return "StandbyCommand";
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
            return "EnabledCommand";
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
            return "DisableCommand";
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
