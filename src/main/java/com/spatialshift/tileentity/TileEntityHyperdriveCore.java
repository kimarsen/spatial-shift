package com.spatialshift.tileentity;

import com.spatialshift.multiblock.MultiblockValidator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

public class TileEntityHyperdriveCore extends TileEntity implements ITickable {

    private boolean active = false;

    @Override
    public void update() {
        if (world != null && !world.isRemote && world.getTotalWorldTime() % 20L == 0L) {
            checkActiveState();
        }
    }

    public void checkActiveState() {
        if (!MultiblockValidator.isValidHyperdrivePattern(world, pos)) {
            active = false;
            return;
        }

        boolean powered = world.isBlockPowered(pos);
        if (!powered) {
            for (int dy = -1; dy <= 1 && !powered; dy++) {
                for (int dx = -1; dx <= 1 && !powered; dx++) {
                    for (int dz = -1; dz <= 1 && !powered; dz++) {
                        BlockPos p = pos.add(dx, dy, dz);
                        if (world.isBlockPowered(p)) {
                            powered = true;
                        }
                    }
                }
            }
        }
        active = powered;
    }

    public boolean isActive() {
        return active;
    }
}
