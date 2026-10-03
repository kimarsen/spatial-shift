package com.spatialshift.capability;

import com.spatialshift.data.WandMode;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;

public interface IPlayerSelection {

    WandMode getMode();

    void setMode(WandMode mode);

    BlockPos getPrimaryPos();

    void setPrimaryPos(BlockPos pos);

    BlockPos getSecondaryPos();

    void setSecondaryPos(BlockPos pos);

    LongSet getSelectedPositions();

    void addPosition(BlockPos pos);

    void removePosition(BlockPos pos);

    boolean containsPosition(BlockPos pos);

    void clear();

    void rebuildFromBox();

    void setFromPackedArray(long[] packed);
 
    void shift(BlockPos offset);
}
