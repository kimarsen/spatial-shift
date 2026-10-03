package com.spatialshift.engine;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

public class CollisionDetector {

    public static boolean isAreaFree(World world, BlockPos candidateOrigin, Set<BlockPos> relativeOffsets, BlockPos sourceOrigin) {
        for (BlockPos offset : relativeOffsets) {
            BlockPos targetPos = candidateOrigin.add(offset);
            int y = targetPos.getY();
            if (y < 0 || y > 255) {
                return false;
            }

            BlockPos relToSource = targetPos.subtract(sourceOrigin);
            if (relativeOffsets.contains(relToSource)) {
                continue;
            }

            IBlockState state = world.getBlockState(targetPos);
            Block block = state.getBlock();
            if (!block.isAir(state, world, targetPos) && !block.isReplaceable(world, targetPos)) {
                return false;
            }
        }
        return true;
    }

    public static BlockPos findNearestValidPosition(World world, BlockPos initialTarget, Set<BlockPos> relativeOffsets, BlockPos sourceOrigin, int maxRadius) {
        if (isAreaFree(world, initialTarget, relativeOffsets, sourceOrigin)) {
            return initialTarget;
        }

        for (int r = 1; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }

                    for (int dy = 0; dy <= 4; dy++) {
                        BlockPos candidate = initialTarget.add(dx, dy, dz);
                        if (isAreaFree(world, candidate, relativeOffsets, sourceOrigin)) {
                            return candidate;
                        }
                        if (dy != 0) {
                            candidate = initialTarget.add(dx, -dy, dz);
                            if (isAreaFree(world, candidate, relativeOffsets, sourceOrigin)) {
                                return candidate;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
