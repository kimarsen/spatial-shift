package com.spatialshift.data;

public enum AnchorAccess {
    PUBLIC,
    PRIVATE;

    public AnchorAccess next() {
        AnchorAccess[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
