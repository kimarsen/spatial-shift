package com.spatialshift.multiblock;

import com.spatialshift.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MultiblockValidator {

    public static boolean isValidPattern(World world, BlockPos corePos) {
        if (!world.isAirBlock(corePos.up())) {
            return false;
        }

        IBlockState centerState = world.getBlockState(corePos);
        if (centerState.getBlock() != ModBlocks.TELEPORT_CORE) {
            return false;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos framePos = corePos.add(dx, 0, dz);
                IBlockState state = world.getBlockState(framePos);
                if (state.getBlock() != ModBlocks.CORE_FRAME) {
                    return false;
                }
            }
        }

        return true;
    }

    public static BlockPos findCenterCore(World world, BlockPos clickedPos) {
        if (world.getBlockState(clickedPos).getBlock() == ModBlocks.TELEPORT_CORE) {
            return clickedPos;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos candidate = clickedPos.add(dx, 0, dz);
                if (world.getBlockState(candidate).getBlock() == ModBlocks.TELEPORT_CORE) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
