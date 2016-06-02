package toyocsbridge;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A collection of different state objects.
 * @author tonyj
 */
public class AggregateStatus {
 
    private final Map<Class<? extends Enum>,State> states = new LinkedHashMap<>();
       
    void add(State<?> state) {
        states.put(state.getEnumClass(), state);
    }
    /**
     * Test if all given states are present in the aggregate status.
     * @param states The states to look for
     * @return <code>true</code> if all states are present
     */
    boolean hasState(Enum ... statesToTest) {
        for (Enum e : statesToTest) {
            Class<? extends Enum> enumClass = e.getClass();
            State state = states.get(enumClass);
            if (state == null || !state.isInState(e)) {
                return false;
            }
        }
        return true;
    }
    
    Collection<State> getStates() {
        return states.values();
    }

    @Override
    public String toString() {
        return "AggregateStatus{" + "states=" + states.values() + '}';
    }
}
