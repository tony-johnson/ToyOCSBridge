package toyocsbridge;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates a state, and generates state change events.
 *
 * @author tonyj
 * @param <T> The enumeration representing the states.
 */
public class State<T extends Enum> {

    private T currentState;
    private final Class<T> enumClass;
    private final CCS ccs;
    private final List<StateChangeListener<T>> listeners = new CopyOnWriteArrayList<>();
    private static final Logger logger = Logger.getLogger(State.class.getName());

    /**
     * Constructor
     *
     * @param ccs The CCS used to send status change notifications
     * @param initialState Initial state
     */
    State(CCS ccs, T initialState) {
        this.ccs = ccs;
        this.enumClass = (Class<T>) initialState.getClass();
        currentState = initialState;
        ccs.getAggregateStatus().add(this);
    }

    /**
     * Changes the current state. Generates a status change notification when
     * the status is changed.
     *
     * @param state The new state
     */
    void setState(T state) {
        if (currentState != state) {
            T oldState = currentState;
            currentState = state;
            logger.log(Level.INFO, String.format("State Changed %s: %s->%s",currentState.getClass().getSimpleName(), oldState, currentState));

            for (StateChangeListener l : listeners) {
                l.stateChanged(this, oldState);
            }
            ccs.notifyStateChanged(this, oldState);
        }
    }

    T getState() {
        return currentState;
    }

    public Class<T> getEnumClass() {
        return enumClass;
    }

    boolean isInState(T state) {
        return currentState == state;
    }
    
    void addStateChangeListener(StateChangeListener<T> listener) {
        listeners.add(listener);
    }
    
    void removeStateChangeListener(StateChangeListener<T> listener) {
        listeners.remove(listener);
    }

    /**
     * Check the state and generate an exception if the current state does not
     * match.
     *
     * @param states The expected state, may be a list of possible states
     * @throws toyocsbridge.State.InvalidStateException If the state does not
     * match.
     */
    void checkState(T... states) throws InvalidStateException {
        for (T state : states) {
            if (state == currentState) {
                return;
            }
        }
        throw new InvalidStateException(String.format("State: %s expected %s was %s", enumClass.getSimpleName(), Arrays.toString(states), currentState));
    }

    static class InvalidStateException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public InvalidStateException(String reason) {
            super(reason);
        }
    }

    static interface StateChangeListener<T extends Enum> {
        void stateChanged(State<T> state, T oldState);
    }

    @Override
    public String toString() {
        return "State{" + enumClass.getSimpleName() +" = "+ currentState +'}';
    }
}
