package toyocsbridge;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import toyocsbridge.State.StateChangeListener;

/**
 * Trivial CCS simulation. This class deals with routing status messages and 
 * scheduling actions. it also allows status listeners to be added which will
 * receive notification of any status change.
 * @author tonyj
 */
class CCS {

    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(4);
    private final List<StateChangeListener<? extends Enum>> listeners = new CopyOnWriteArrayList<>();
    private final List<FutureStatus> waiters = new CopyOnWriteArrayList<>();

    private final AggregateStatus as = new AggregateStatus();
    
    <T> ScheduledFuture<T> schedule(Duration when, Callable<T> callable) {
        return scheduler.schedule(callable, when.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    ScheduledFuture<?> schedule(Duration when, Runnable runnable) {
        return scheduler.schedule(runnable, when.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    <T extends Enum> void notifyStateChanged(State<T> currentState, T oldState) {
        for (StateChangeListener l : listeners) {
            l.stateChanged(currentState, oldState);
        }
        for (FutureStatus waiter : waiters) {
            if (as.hasState(waiter.state)) {
                waiter.done();
            }
        }
    }
    
    void addStateChangeListener(StateChangeListener<? extends Enum> listener) {
        listeners.add(listener);
    }
    
    void removeStateChangeListener(StateChangeListener<? extends Enum> listener) {
        listeners.remove(listener);
    }

    void shutdown() {
        scheduler.shutdown();
    }

    public AggregateStatus getAggregateStatus() {
        return as;
    }

    Future<Void> waitForStatus(Enum state) {
        FutureStatus waiter = new FutureStatus(state);
        if (as.hasState(state)) {
            waiter.done();
        }
        else {
            waiters.add(waiter);
        }
        return waiter;
    }
    /**
     * An implementation of a future which wait for a particular status.
     */
    private class FutureStatus implements Future<Void> {

        private boolean done = false;
        private boolean cancelled = false;
        private final Enum state;

        FutureStatus(Enum state) {
            this.state = state;
        }
        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            waiters.remove(this);
            notifyAll();
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
        
        @Override
        public boolean isDone() {
            return done;
        }
        
        @Override
        public synchronized Void get() throws InterruptedException, ExecutionException {
            while (!done) {
                wait();
            }
            return null;
        }
        
        @Override
        public synchronized Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            wait(unit.toMillis(timeout));
            if (!done) throw new TimeoutException("Timeout waiting for state: "+state);
            return null;
        }
        
        synchronized void done() {
            done = true;
            notifyAll();
        }
    }
    
}
