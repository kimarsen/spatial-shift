package com.spatialshift.data;

public enum WandMode {
    BOX,
    VOXEL;

    public WandMode next() {
        WandMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
