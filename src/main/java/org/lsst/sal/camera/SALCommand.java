package org.lsst.sal.camera;

/**
 * Base class for all commands.
 * @author tonyj
 */
class SALCommand {

    private final int cmdId;
    SALCommand(int cmdId) {
        this.cmdId = cmdId;
    }

    public int getCmdId() {
        return cmdId;
    }
}
