package com.spatialshift.data;

public enum TeleportMode {
    LANDING,
    AIR;

    public TeleportMode next() {
        TeleportMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
