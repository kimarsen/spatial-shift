package com.spatialshift.capability;

import com.spatialshift.data.WandMode;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;

public interface IPlayerSelection {

    int MAX_SELECTION_SPAN = 100;
    int MAX_SELECTION_VOLUME = 32768;

    WandMode getMode();

    void setMode(WandMode mode);

    BlockPos getPrimaryPos();

    void setPrimaryPos(BlockPos pos);

    BlockPos getSecondaryPos();

    void setSecondaryPos(BlockPos pos);

    LongSet getSelectedPositions();

    boolean addPosition(BlockPos pos);

    void removePosition(BlockPos pos);

    boolean containsPosition(BlockPos pos);

    void clear();

    boolean rebuildFromBox();

    void setFromPackedArray(long[] packed);

    void shift(BlockPos offset);
}
