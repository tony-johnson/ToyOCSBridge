package toyocsbridge;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A trivial Filter simulation.
 *
 * @author tonyj
 */
public class Filter {

    public enum FilterState {

        UNLOADING, LOADING, LOADED, UNLOADED, ROTATING
    }

    static final Duration LOAD_TIME = Duration.ofMillis(15000);
    static final Duration ROTATION_TIME_PER_DEGREE = Duration.ofMillis(100);
    static final Duration UNLOAD_TIME = Duration.ofMillis(15000);

    private List<String> availableFilters = Arrays.asList(new String[]{"u-10", "g-9", "r-1", "i-9", "x-100"});
    private String currentFilter;
    private int currentRotationPosition = 0;

    private final State filterState;
    private final CCS ccs;

    Filter(CCS ccs) {
        this.ccs = ccs;
        filterState = new State(ccs, FilterState.UNLOADED);
    }

    boolean filterIsAvailable(String filter) {
        return availableFilters.contains(filter);
    }

    List<String> getAvailableFilters() {
        return Collections.unmodifiableList(availableFilters);
    }

    void setFilter(String filter) throws InterruptedException, ExecutionException, TimeoutException {
        int position = availableFilters.indexOf(filter);
        if (position < 0) {
            throw new IllegalArgumentException("Invalid filter: " + filter);
        } else if (filter.equals(currentFilter)) {
            // No-op?
        } else {

            if (currentFilter != null) {
                filterState.setState(FilterState.UNLOADING);
                Future<Void> waitForUnloaded = ccs.waitForStatus(FilterState.UNLOADED);
                ccs.schedule(UNLOAD_TIME, () -> {
                    filterState.setState(FilterState.UNLOADED);
                    currentFilter = null;
                });
                waitForUnloaded.get(UNLOAD_TIME.toMillis() * 2, TimeUnit.MILLISECONDS);
            }
            int targetRotation = position * 360 / 5;
            if (currentRotationPosition != targetRotation) {
                int degreesToRotate = Math.abs(currentRotationPosition - targetRotation) % 360;
                filterState.setState(FilterState.ROTATING);
                Future<Void> waitForRotation = ccs.waitForStatus(FilterState.UNLOADED);
                Duration rotationTime = ROTATION_TIME_PER_DEGREE.multipliedBy(degreesToRotate);
                ccs.schedule(rotationTime, () -> {
                    filterState.setState(FilterState.UNLOADED);
                });
                waitForRotation.get(rotationTime.toMillis() * 2, TimeUnit.MILLISECONDS);
            }
            filterState.setState(FilterState.LOADING);
            Future<Void> waitForUnloaded = ccs.waitForStatus(FilterState.LOADED);
            ccs.schedule(LOAD_TIME, () -> {
                filterState.setState(FilterState.LOADED);
                currentFilter = filter;
            });
            waitForUnloaded.get(LOAD_TIME.toMillis() * 2, TimeUnit.MILLISECONDS);
        }
    }
}
