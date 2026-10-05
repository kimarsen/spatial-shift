package com.spatialshift.multiblock;

import com.spatialshift.init.ModBlocks;
import com.spatialshift.tileentity.TileEntityFuelCompartment;
import com.spatialshift.tileentity.TileEntityHyperdriveCore;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ShipNetworkScanner {

    private static final int MAX_CABLE_SCAN = 256;
    private static final int MAX_PIPE_SCAN = 256;

    public static boolean isHyperdriveConnectedAndActive(World world, BlockPos corePos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = corePos.add(dx, dy, dz);
                    visited.add(p);
                    for (EnumFacing facing : EnumFacing.VALUES) {
                        BlockPos neighbor = p.offset(facing);
                        if (world.getBlockState(neighbor).getBlock() == ModBlocks.HYPER_CABLE && visited.add(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        int scanCount = 0;
        while (!queue.isEmpty() && scanCount < MAX_CABLE_SCAN) {
            BlockPos current = queue.poll();
            scanCount++;

            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(facing);
                Block block = world.getBlockState(neighbor).getBlock();

                if (block == ModBlocks.HYPERDRIVE_CORE) {
                    TileEntity te = world.getTileEntity(neighbor);
                    if (te instanceof TileEntityHyperdriveCore) {
                        TileEntityHyperdriveCore hd = (TileEntityHyperdriveCore) te;
                        hd.checkActiveState();
                        if (hd.isActive()) {
                            return true;
                        }
                    }
                } else if (block == ModBlocks.CORE_FRAME) {
                    BlockPos center = MultiblockValidator.findCenter(world, neighbor);
                    if (center != null && world.getBlockState(center).getBlock() == ModBlocks.HYPERDRIVE_CORE) {
                        TileEntity te = world.getTileEntity(center);
                        if (te instanceof TileEntityHyperdriveCore) {
                            TileEntityHyperdriveCore hd = (TileEntityHyperdriveCore) te;
                            hd.checkActiveState();
                            if (hd.isActive()) {
                                return true;
                            }
                        }
                    }
                } else if (block == ModBlocks.HYPER_CABLE) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        return false;
    }

    public static List<TileEntityFuelCompartment> findConnectedFuelCompartments(World world, BlockPos corePos) {
        List<TileEntityFuelCompartment> list = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> foundCenters = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = corePos.add(dx, dy, dz);
                    visited.add(p);
                    for (EnumFacing facing : EnumFacing.VALUES) {
                        BlockPos neighbor = p.offset(facing);
                        if (world.getBlockState(neighbor).getBlock() == ModBlocks.FUEL_PIPE && visited.add(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        int scanCount = 0;
        while (!queue.isEmpty() && scanCount < MAX_PIPE_SCAN) {
            BlockPos current = queue.poll();
            scanCount++;

            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(facing);
                Block block = world.getBlockState(neighbor).getBlock();

                if (block == ModBlocks.FUEL_COMPARTMENT_CORE) {
                    if (foundCenters.add(neighbor)) {
                        TileEntity te = world.getTileEntity(neighbor);
                        if (te instanceof TileEntityFuelCompartment && MultiblockValidator.isValidFuelCompartmentPattern(world, neighbor)) {
                            list.add((TileEntityFuelCompartment) te);
                        }
                    }
                } else if (block == ModBlocks.CORE_FRAME) {
                    BlockPos center = MultiblockValidator.findCenter(world, neighbor);
                    if (center != null && world.getBlockState(center).getBlock() == ModBlocks.FUEL_COMPARTMENT_CORE) {
                        if (foundCenters.add(center)) {
                            TileEntity te = world.getTileEntity(center);
                            if (te instanceof TileEntityFuelCompartment && MultiblockValidator.isValidFuelCompartmentPattern(world, center)) {
                                list.add((TileEntityFuelCompartment) te);
                            }
                        }
                    }
                } else if (block == ModBlocks.FUEL_PIPE) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        return list;
    }

    public static Set<BlockPos> collectShipMachinery(World world, BlockPos corePos) {
        Set<BlockPos> machinery = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        addCubeModule(corePos, machinery);
        queue.addAll(machinery);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos n = current.offset(facing);
                if (machinery.contains(n)) {
                    continue;
                }

                Block b = world.getBlockState(n).getBlock();
                if (b == ModBlocks.HYPER_CABLE || b == ModBlocks.FUEL_PIPE) {
                    if (machinery.add(n)) {
                        queue.add(n);
                    }
                } else if (b == ModBlocks.CORE_FRAME || b == ModBlocks.HYPERDRIVE_CORE || b == ModBlocks.FUEL_COMPARTMENT_CORE) {
                    BlockPos center = MultiblockValidator.findCenter(world, n);
                    if (center != null) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                for (int dz = -1; dz <= 1; dz++) {
                                    BlockPos modPos = center.add(dx, dy, dz);
                                    if (machinery.add(modPos)) {
                                        queue.add(modPos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return machinery;
    }

    private static void addCubeModule(BlockPos center, Set<BlockPos> set) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    set.add(center.add(dx, dy, dz));
                }
            }
        }
    }
}
