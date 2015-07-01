package toyocsbridge;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import toyocsbridge.State.StateChangeListener;

/**
 * A trivial Rafts simulation.
 *
 * @author tonyj
 */
public class Rafts {

    /**
     * Time to readout the science rafts
     */
    static final Duration READOUT_TIME = Duration.ofMillis(2000);
    /**
     * Time to clear the sensors
     */
    static final Duration CLEAR_TIME = Duration.ofMillis(70);
    /**
     * Idle time before a clear is required
     */
    static final Duration IDLE_BEFORE_CLEAR = Duration.ofMillis(4000);

    public enum RaftsState {

        CLEARING, READY, INTEGRATING, READING_OUT, NEEDS_CLEAR
    }

    private final State raftsState;
    private final CCS ccs;

    Rafts(CCS ccs) {
        this.ccs = ccs;
        raftsState = new State(ccs, RaftsState.NEEDS_CLEAR);
        // Whenever we enter ready state, we start a timer to indocate when a clear is needed
        // If we exit ready state we cancel the timer.
        raftsState.addStateChangeListener(new StateChangeListener<RaftsState>() {
            private ScheduledFuture<?> clearFuture;

            @Override
            public void stateChanged(State<RaftsState> currentState, RaftsState oldState) {
                if (currentState.isInState(RaftsState.READY)) {
                    clearFuture = ccs.schedule(IDLE_BEFORE_CLEAR, () -> {
                        raftsState.setState(RaftsState.NEEDS_CLEAR);
                    });
                } else {
                    if (clearFuture != null) {
                        clearFuture.cancel(false);
                    }
                }
            }

        });
    }

    void expose(Duration integrationTime) {
        raftsState.checkState(RaftsState.READY);
        raftsState.setState(RaftsState.INTEGRATING);
        ccs.schedule(integrationTime, () -> {
            raftsState.setState(RaftsState.READING_OUT);
        });
        ccs.schedule(integrationTime.plus(READOUT_TIME), () -> {
            raftsState.setState(RaftsState.READY);
        });
    }

    void clear() {
        raftsState.checkState(RaftsState.READY, RaftsState.NEEDS_CLEAR);
        raftsState.setState(RaftsState.CLEARING);
        ccs.schedule(CLEAR_TIME, () -> {
            raftsState.setState(RaftsState.READY);
        });
    }

}
