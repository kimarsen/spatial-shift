package com.spatialshift.client;

import com.spatialshift.data.AnchorData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientAnchorCache {

    private static final List<AnchorData> ANCHORS = new ArrayList<>();

    public static synchronized void update(List<AnchorData> newAnchors) {
        ANCHORS.clear();
        ANCHORS.addAll(newAnchors);
    }

    public static synchronized List<AnchorData> getAnchors() {
        return Collections.unmodifiableList(new ArrayList<>(ANCHORS));
    }
}
