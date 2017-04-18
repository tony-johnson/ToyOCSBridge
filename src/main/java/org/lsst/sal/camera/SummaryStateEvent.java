package org.lsst.sal.camera;

import org.lsst.sal.SAL_camera;

/**
 * The SummaryStateEvent (for LSE209 state changes)
 * @author tonyj
 */
public class SummaryStateEvent extends CameraEvent {

    private final LSE209State state;

    SummaryStateEvent(int priority, int value) {
        super(priority);
        this.state = LSE209State.values()[value];
    }

    // TDOO: I made this up, since it does not appear to be defined anywhere else
    public enum LSE209State {
        OFFLINE_PUBLISH_ONLY, OFFLINE_AVAILABLE, STANDBY, DISABLED, ENABLED, FAULT
    };

    SummaryStateEvent(int priority, LSE209State state) {
        super(priority);
        this.state = state;
    }

    public LSE209State getState() {
        return state;
    }
    
    @Override
    void logEvent(SAL_camera mgr) {
        // Create events 
        mgr.salEvent("camera_logevent_SummaryState");
        camera.logevent_SummaryState summaryStateEvent  = new camera.logevent_SummaryState(); 
        summaryStateEvent.SummaryStateValue = state.ordinal();
        mgr.logEvent_SummaryState(summaryStateEvent, getPriority());
    }
}
