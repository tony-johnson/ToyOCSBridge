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
 * This is a toy for experimenting with the OCS event behaviour. It is not 
 * real CCS code and is not intended to become real CCS code.
 * @author tonyj
 */
public class ToyOCSBridge {

    enum TakeImageReadinessState { READY, NOT_READY, GETTING_READY };
    
    private final CCS ccs = new CCS();
    private final Shutter shutter = new Shutter(ccs);
    private final Rafts rafts = new Rafts(ccs);
    private final OCSCommandExecutor ocs = new OCSCommandExecutor(ccs);

    private final State takeImageReadinessState = new State(ccs,TakeImageReadinessState.NOT_READY);

    public ToyOCSBridge() {
        // We are ready to take an image only if the rafts have been cleared, and the shutter
        // has been prepared.
        ccs.addStateChangeListener(new StateChangeListener(){

            @Override
            public void stateChanged(State state, Enum oldState) {
                AggregateStatus as = ccs.getAggregateStatus();
                if (as.hasState(Rafts.RaftsState.READY, Shutter.ShutterReadinessState.READY)) {
                    takeImageReadinessState.setState(TakeImageReadinessState.READY);
                } else if (!as.hasState(TakeImageReadinessState.GETTING_READY)) {
                    takeImageReadinessState.setState(TakeImageReadinessState.NOT_READY);
                }
            }
        });
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ToyOCSBridge ocs = new ToyOCSBridge();
        ocs.run();        
    }

       
    private void run() {
        OCSCommand initImage = new InitImageCommand(1.0);
        OCSCommand takeImages = new TakeImagesCommand(15.0,2,true);
        ocs.executeOCSCommand(initImage);
        ccs.schedule(Duration.ofSeconds(2), ()->{ocs.executeOCSCommand(takeImages);});
    }
    
    private class InitImageCommand extends OCSCommand {
        private double deltaT;

        public InitImageCommand(double deltaT) {
            this.deltaT = deltaT;
        }

        @Override
        void testPreconditions() throws PreconditionsNotMet {
            if (deltaT<=0 || deltaT>15) {
                throw new PreconditionsNotMet("Invalid deltaT: "+deltaT);
            }
        }

        @Override
        void execute() {
            Duration takeImagesExpected = Duration.ofMillis((long) (deltaT*1000));
            takeImageReadinessState.setState(TakeImageReadinessState.GETTING_READY);   
            ccs.schedule(takeImagesExpected.minus(Rafts.CLEAR_TIME),()->{rafts.clear();});
            ccs.schedule(takeImagesExpected.minus(Shutter.PREP_TIME),()->{shutter.prepare();});
        }
    }
    
    private class TakeImagesCommand extends OCSCommand {
        private double exposure;
        private int nImages;
        private boolean openShutter;

        public TakeImagesCommand(double exposure, int nImages, boolean openShutter) {
            this.exposure = exposure;
            this.nImages = nImages;
            this.openShutter = openShutter;
        }

        @Override
        void testPreconditions() throws PreconditionsNotMet {
            if (nImages<=0 || nImages>10 || exposure<1 || exposure>30) {
                throw new PreconditionsNotMet("Invalid argument") ;
            }
        }

        @Override
        void execute() throws InterruptedException, ExecutionException, TimeoutException {
            Duration exposeTime = Duration.ofMillis((long) (exposure*1000));
            for (int i=0; i<nImages; i++) {
                Future waitUntilReady = ccs.waitForStatus(TakeImageReadinessState.READY);
                if (takeImageReadinessState.isInState(TakeImageReadinessState.NOT_READY)) {
                    rafts.clear();
                    shutter.prepare();
                }

                waitUntilReady.get(1,TimeUnit.SECONDS);
                if (openShutter) {
                    shutter.expose(exposeTime);
                    rafts.expose(exposeTime.plus(Shutter.MOVE_TIME));
                    // For the last exposure we only wait until the readout starts
                    // For other exposures we must wait until readout is complete
                    Future waitUntilDone = ccs.waitForStatus(i+1<nImages ? Rafts.RaftsState.READY : Rafts.RaftsState.READING_OUT);
                    waitUntilDone.get(exposeTime.plus(Shutter.MOVE_TIME).plus(Rafts.READOUT_TIME).plusSeconds(1).toMillis(),TimeUnit.MILLISECONDS);
                } else {
                    rafts.expose(exposeTime);
                    Future waitUntilDone = ccs.waitForStatus(i+1<nImages ? Rafts.RaftsState.READY : Rafts.RaftsState.READING_OUT);
                    waitUntilDone.get(exposeTime.plus(Shutter.MOVE_TIME).plus(Rafts.READOUT_TIME).plusSeconds(1).toMillis(),TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}
