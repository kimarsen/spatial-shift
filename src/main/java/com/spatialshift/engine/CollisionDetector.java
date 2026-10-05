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

        int minRelY = 0;
        int maxRelY = 0;
        for (BlockPos offset : relativeOffsets) {
            if (offset.getY() < minRelY) {
                minRelY = offset.getY();
            }
            if (offset.getY() > maxRelY) {
                maxRelY = offset.getY();
            }
        }

        for (int r = 1; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }

                    for (int dy = 0; dy <= r; dy++) {
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

        int topTerrainY = world.getTopSolidOrLiquidBlock(initialTarget).getY();
        int safeSurfaceY = topTerrainY + 1 - minRelY;
        int startY = Math.max(initialTarget.getY() + 1, safeSurfaceY);
        int maxY = 255 - maxRelY;

        for (int y = startY; y <= maxY; y++) {
            BlockPos candidate = new BlockPos(initialTarget.getX(), y, initialTarget.getZ());
            if (isAreaFree(world, candidate, relativeOffsets, sourceOrigin)) {
                return candidate;
            }
        }

        for (int dy = 1; dy <= 64; dy++) {
            int y = initialTarget.getY() + dy;
            if (y <= maxY) {
                BlockPos candidate = new BlockPos(initialTarget.getX(), y, initialTarget.getZ());
                if (isAreaFree(world, candidate, relativeOffsets, sourceOrigin)) {
                    return candidate;
                }
            }
            int downY = initialTarget.getY() - dy;
            if (downY + minRelY >= 1) {
                BlockPos candidate = new BlockPos(initialTarget.getX(), downY, initialTarget.getZ());
                if (isAreaFree(world, candidate, relativeOffsets, sourceOrigin)) {
                    return candidate;
                }
            }
        }

        for (int r = 1; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }

                    BlockPos columnProbe = new BlockPos(initialTarget.getX() + dx, 0, initialTarget.getZ() + dz);
                    int colTop = world.getTopSolidOrLiquidBlock(columnProbe).getY();
                    int colSurfaceY = colTop + 1 - minRelY;
                    if (colSurfaceY <= maxY) {
                        BlockPos surfaceCandidate = new BlockPos(columnProbe.getX(), colSurfaceY, columnProbe.getZ());
                        if (isAreaFree(world, surfaceCandidate, relativeOffsets, sourceOrigin)) {
                            return surfaceCandidate;
                        }
                    }
                }
            }
        }

        return null;
    }
}
