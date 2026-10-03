package com.spatialshift.engine;

import com.spatialshift.capability.IPlayerSelection;
import com.spatialshift.capability.PlayerSelection;
import com.spatialshift.data.TeleportMode;
import com.spatialshift.fx.TeleportEffects;
import com.spatialshift.item.ItemSelectionWand;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TeleportEngine {

    private static final int MAX_CORE_RADIUS = 50;

    public static void executeTeleport(WorldServer world, EntityPlayerMP player, TileEntityTeleportCore core, BlockPos requestedTarget, int targetDim, TeleportMode mode, boolean isAnchorTarget) {
        if (!core.hasFuel()) {
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.no_fuel"), true);
            return;
        }

        IPlayerSelection selection = player.getCapability(PlayerSelection.CAPABILITY, null);
        if (selection == null || selection.getSelectedPositions().isEmpty()) {
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.selection_empty"), true);
            return;
        }

        LongSet positions = selection.getSelectedPositions();
        BlockPos corePos = core.getPos();

        Set<BlockPos> relativeOffsets = new HashSet<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        LongIterator iter = positions.iterator();
        while (iter.hasNext()) {
            BlockPos pos = BlockPos.fromLong(iter.nextLong());

            if (Math.abs(pos.getX() - corePos.getX()) > MAX_CORE_RADIUS || Math.abs(pos.getZ() - corePos.getZ()) > MAX_CORE_RADIUS) {
                player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.area_outside_core_range"), true);
                return;
            }

            relativeOffsets.add(pos.subtract(corePos));

            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        int minRelativeY = minY - corePos.getY();
        int maxRelativeY = maxY - corePos.getY();
        BlockPos initialTarget = computeTargetOrigin(world, requestedTarget, mode, isAnchorTarget, maxRelativeY, minRelativeY);

        ChunkSafetyManager chunkManager = new ChunkSafetyManager(world);
        if (!chunkManager.lockChunks(initialTarget, relativeOffsets)) {
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.chunk_load_failed"), true);
            chunkManager.release();
            return;
        }

        BlockPos validTarget = CollisionDetector.findNearestValidPosition(world, initialTarget, relativeOffsets, 64);
        if (validTarget == null) {
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.no_free_space"), true);
            chunkManager.release();
            return;
        }

        if (!core.consumeFuel()) {
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.no_fuel"), true);
            chunkManager.release();
            return;
        }

        VoxelSnapshot snapshot = createSnapshot(world, corePos, relativeOffsets, minX, minY, minZ, maxX, maxY, maxZ);
        if (!snapshot.getEntities().contains(player)) {
            snapshot.getEntities().add(player);
        }

        pasteSnapshot(world, validTarget, snapshot);
        EntityRelocator.relocateEntities(snapshot.getEntities(), corePos, validTarget);
        clearSource(world, corePos, relativeOffsets);

        BlockPos displacement = validTarget.subtract(corePos);
        selection.shift(displacement);
        ItemSelectionWand.syncSelection(player, selection);

        AxisAlignedBB destinationBounds = computeShiftedBounds(snapshot.getBounds(), corePos, validTarget);
        TeleportEffects.playEffects(world, validTarget, destinationBounds, snapshot.getEntities());

        chunkManager.release();
        player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.teleport_success"), true);
    }

    private static BlockPos computeTargetOrigin(WorldServer world, BlockPos target, TeleportMode mode, boolean isAnchorTarget, int maxRelativeY, int minRelativeY) {
        if (!isAnchorTarget) {
            int targetY = mode == TeleportMode.AIR ? target.getY() + 20 : target.getY();
            if (targetY + maxRelativeY > 255) {
                targetY = 255 - maxRelativeY;
            }
            if (targetY + minRelativeY < 0) {
                targetY = -minRelativeY;
            }
            return new BlockPos(target.getX(), targetY, target.getZ());
        } else {
            if (mode == TeleportMode.AIR) {
                int targetY = target.getY() + 20;
                if (targetY + maxRelativeY > 255) {
                    targetY = 255 - maxRelativeY;
                }
                if (targetY + minRelativeY < 0) {
                    targetY = -minRelativeY;
                }
                return new BlockPos(target.getX(), targetY, target.getZ());
            } else {
                BlockPos ground = world.getTopSolidOrLiquidBlock(target);
                int targetY = ground.getY() + 1 - minRelativeY;
                if (targetY + maxRelativeY > 255) {
                    targetY = 255 - maxRelativeY;
                }
                if (targetY + minRelativeY < 0) {
                    targetY = -minRelativeY;
                }
                return new BlockPos(target.getX(), targetY, target.getZ());
            }
        }
    }

    private static VoxelSnapshot createSnapshot(WorldServer world, BlockPos origin, Set<BlockPos> offsets, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        VoxelSnapshot snapshot = new VoxelSnapshot(origin);
        AxisAlignedBB bounds = new AxisAlignedBB(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
        snapshot.setBounds(bounds);

        for (BlockPos offset : offsets) {
            BlockPos sourcePos = origin.add(offset);
            IBlockState state = world.getBlockState(sourcePos);
            snapshot.putBlock(offset, state);

            TileEntity te = world.getTileEntity(sourcePos);
            if (te != null) {
                NBTTagCompound nbt = new NBTTagCompound();
                te.writeToNBT(nbt);
                snapshot.putTileEntity(offset, nbt);
            }
        }

        snapshot.getEntities().addAll(world.getEntitiesWithinAABB(Entity.class, bounds));
        return snapshot;
    }

    private static void pasteSnapshot(WorldServer world, BlockPos destinationOrigin, VoxelSnapshot snapshot) {
        for (Map.Entry<BlockPos, IBlockState> entry : snapshot.getStates().entrySet()) {
            BlockPos targetPos = destinationOrigin.add(entry.getKey());
            world.setBlockState(targetPos, entry.getValue(), 2);
        }

        for (Map.Entry<BlockPos, NBTTagCompound> entry : snapshot.getTileEntities().entrySet()) {
            BlockPos targetPos = destinationOrigin.add(entry.getKey());
            NBTTagCompound nbt = entry.getValue().copy();
            nbt.setInteger("x", targetPos.getX());
            nbt.setInteger("y", targetPos.getY());
            nbt.setInteger("z", targetPos.getZ());

            TileEntity te = TileEntity.create(world, nbt);
            if (te != null) {
                world.setTileEntity(targetPos, te);
            }
        }
    }

    private static void clearSource(WorldServer world, BlockPos origin, Set<BlockPos> offsets) {
        for (BlockPos offset : offsets) {
            BlockPos sourcePos = origin.add(offset);
            world.removeTileEntity(sourcePos);
            world.setBlockState(sourcePos, Blocks.AIR.getDefaultState(), 2);
        }
    }

    private static AxisAlignedBB computeShiftedBounds(AxisAlignedBB bounds, BlockPos oldOrigin, BlockPos newOrigin) {
        if (bounds == null) {
            return null;
        }
        double dx = newOrigin.getX() - oldOrigin.getX();
        double dy = newOrigin.getY() - oldOrigin.getY();
        double dz = newOrigin.getZ() - oldOrigin.getZ();
        return bounds.offset(dx, dy, dz);
    }
}
