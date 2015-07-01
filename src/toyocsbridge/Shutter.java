package toyocsbridge;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import toyocsbridge.State.StateChangeListener;

/**
 * Very simple shutter simulation
 *
 * @author tonyj
 */
public class Shutter {

    /**
     * Time needed to get the shutter ready
     */
    static final Duration PREP_TIME = Duration.ofMillis(150);
    /**
     * Time shutter can remain ready after prepare or move
     */
    static final Duration READY_TIME = Duration.ofMillis(4000);
    /**
     * Time needed to move a shutter blade
     */
    static final Duration MOVE_TIME = Duration.ofMillis(980);

    public enum ShutterReadinessState {

        NOT_READY, READY, GETTING_READY
    };

    public enum ShutterState {

        CLOSED, OPENING, OPEN, CLOSING
    };

    private final State shutterReadinessState;
    private final State shutterState;

    private CCS ccs;

    Shutter(CCS ccs) {
        this.ccs = ccs;
        shutterReadinessState = new State(ccs, ShutterReadinessState.NOT_READY);
        shutterState = new State(ccs, ShutterState.CLOSED);
        // When the shutter is closed, we only keep the motors powered up for a limited time
        shutterState.addStateChangeListener(new StateChangeListener<ShutterState>() {
            private ScheduledFuture<?> notReadyFuture;

            @Override
            public void stateChanged(State<ShutterState> state, ShutterState oldState) {
                if (state.isInState(ShutterState.CLOSED)) {
                    notReadyFuture = ccs.schedule(READY_TIME, () -> {
                        shutterReadinessState.setState(ShutterReadinessState.NOT_READY);
                    });
                } else {
                    if (notReadyFuture != null) notReadyFuture.cancel(false);
                }
            }

        });
    }

    void prepare() {
        shutterReadinessState.setState(ShutterReadinessState.GETTING_READY);
        ccs.schedule(PREP_TIME, () -> {
            shutterReadinessState.setState(ShutterReadinessState.READY);
        });
    }

    void expose(Duration exposureTime) {
        shutterReadinessState.checkState(ShutterReadinessState.READY);
        shutterState.checkState(ShutterState.CLOSED);
        shutterState.setState(ShutterState.OPENING);
        Duration time = MOVE_TIME;
        // TODO: This does not correctly handle the case when both blades move at once
        ccs.schedule(time, () -> {
            shutterState.setState(ShutterState.OPEN);
        });
        time = time.plus(exposureTime);
        ccs.schedule(time, () -> {
            shutterState.setState(ShutterState.CLOSING);
        });
        time = time.plus(MOVE_TIME);
        ccs.schedule(time, () -> {
            shutterState.setState(ShutterState.CLOSED);
        });
    }
}
