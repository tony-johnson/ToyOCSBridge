package toyocsbridge;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    private final CCS ccs = new CCS();
    private final Shutter shutter = new Shutter(ccs);
    private final Rafts rafts = new Rafts(ccs);
    private final Filter fcs = new Filter(ccs);
    private OCSCommandExecutor ocs = new OCSCommandExecutor(ccs);

    private final State takeImageReadinessState = new State(ccs, TakeImageReadinessState.NOT_READY);

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
     * Allow a user to provide an alternative implementation of the OCSCommandExecutor.
     * Used to override the default OCSCommandExecutor with one that actually sends
     * acknowledgments back to OCS.
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
        ocs.executeOCSCommand(initImage);
    }

    void takeImages(int cmdId, double exposure, int nImages, boolean openShutter) {
        OCSCommand takeImages = new TakeImagesCommand(cmdId, exposure, nImages, openShutter);
        ocs.executeOCSCommand(takeImages);
    }

    void setFilter(int cmdId, String filterName) {
        OCSCommand setFilter = new SetFilterCommand(cmdId, filterName);
        ocs.executeOCSCommand(setFilter);
    }

    public Filter getFCS() {
        return fcs;
    }

    CCS getCCS() {
        return ccs;
    }

    class InitImageCommand extends OCSCommand {

        private double deltaT;

        public InitImageCommand(int cmdId, double deltaT) {
            super(cmdId);
            this.deltaT = deltaT;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
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

        private double exposure;
        private int nImages;
        private boolean openShutter;

        public TakeImagesCommand(int cmdId, double exposure, int nImages, boolean openShutter) {
            super(cmdId);
            this.exposure = exposure;
            this.nImages = nImages;
            this.openShutter = openShutter;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
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
            return "TakeImagesCommand{" + "exposure=" + exposure + ", nImages=" + nImages + ", openShutter=" + openShutter + '}';
        }
    }

    class SetFilterCommand extends OCSCommand {

        private String filter;

        public SetFilterCommand(int cmdId, String filter) {
            super(cmdId);
            this.filter = filter;
        }

        @Override
        Duration testPreconditions() throws PreconditionsNotMet {
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

}
