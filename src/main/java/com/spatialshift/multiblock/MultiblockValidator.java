package com.spatialshift.multiblock;

import com.spatialshift.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MultiblockValidator {

    public static boolean isValidPattern(World world, BlockPos corePos) {
        return isValidModulePattern(world, corePos, ModBlocks.TELEPORT_CORE);
    }

    public static boolean isValidHyperdrivePattern(World world, BlockPos corePos) {
        return isValidModulePattern(world, corePos, ModBlocks.HYPERDRIVE_CORE);
    }

    public static boolean isValidFuelCompartmentPattern(World world, BlockPos corePos) {
        return isValidModulePattern(world, corePos, ModBlocks.FUEL_COMPARTMENT_CORE);
    }

    public static boolean isValidModulePattern(World world, BlockPos centerPos, Block expectedCenter) {
        if (!world.isAirBlock(centerPos.up())) {
            return false;
        }

        if (world.getBlockState(centerPos).getBlock() != expectedCenter) {
            return false;
        }

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (dx == 0 && dy == 1 && dz == 0) {
                        continue;
                    }
                    BlockPos framePos = centerPos.add(dx, dy, dz);
                    if (world.getBlockState(framePos).getBlock() != ModBlocks.CORE_FRAME) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static BlockPos findCenterCore(World world, BlockPos clickedPos) {
        return findCenter(world, clickedPos);
    }

    public static BlockPos findCenter(World world, BlockPos clickedPos) {
        Block clickedBlock = world.getBlockState(clickedPos).getBlock();
        if (isCenterBlock(clickedBlock)) {
            return clickedPos;
        }

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = clickedPos.add(dx, dy, dz);
                    if (isCenterBlock(world.getBlockState(candidate).getBlock())) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isCenterBlock(Block block) {
        return block == ModBlocks.TELEPORT_CORE
            || block == ModBlocks.HYPERDRIVE_CORE
            || block == ModBlocks.FUEL_COMPARTMENT_CORE;
    }
}
