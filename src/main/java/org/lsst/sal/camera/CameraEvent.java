package org.lsst.sal.camera;

import org.lsst.sal.SAL_camera;

/**
 * A base class for all camera events.
 * @author tonyj
 */
public abstract class CameraEvent {

    private final int priority;
    
    CameraEvent(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    abstract void logEvent(SAL_camera mgr);
}
