package com.spatialshift.engine;

import com.spatialshift.capability.IPlayerSelection;
import com.spatialshift.capability.PlayerSelection;
import com.spatialshift.data.TeleportMode;
import com.spatialshift.fx.TeleportEffects;
import com.spatialshift.item.ItemSelectionWand;
import com.spatialshift.multiblock.ShipNetworkScanner;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TeleportEngine {

    private static final int MAX_CORE_RADIUS = 50;

    public static boolean validatePreflight(WorldServer world, EntityPlayerMP player, TileEntityTeleportCore core, BlockPos requestedTarget, boolean isAnchorTarget) {
        BlockPos corePos = core.getPos();
        double dist = Math.sqrt(corePos.distanceSq(requestedTarget));
        int distanceBlocks = (int) Math.ceil(dist);
        int fuelNeeded = Math.max(1000, ((distanceBlocks + 999) / 1000) * 1000);

        if (!core.hasFuel(fuelNeeded)) {
            world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 0.7F);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.no_fuel"), true);
            return false;
        }

        IPlayerSelection selection = player.getCapability(PlayerSelection.CAPABILITY, null);
        if (selection == null || selection.getSelectedPositions().isEmpty()) {
            world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_NOTE_BASS, SoundCategory.BLOCKS, 1.0F, 0.6F);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.selection_empty"), true);
            return false;
        }

        if (distanceBlocks > 1000 && !core.isHyperdriveActive() && core.isSafetyLockEnabled()) {
            world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 0.8F);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.safety_lock_distance"), true);
            return false;
        }

        if (core.getCurrentHeat() > TileEntityTeleportCore.SAFE_HEAT_THRESHOLD && core.isSafetyLockEnabled()) {
            world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 0.8F);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.core_overheated"), true);
            return false;
        }

        return true;
    }

    public static void executeTeleport(WorldServer world, EntityPlayerMP player, TileEntityTeleportCore core, BlockPos requestedTarget, int targetDim, TeleportMode mode, boolean isAnchorTarget) {
        BlockPos corePos = core.getPos();
        double dist = Math.sqrt(corePos.distanceSq(requestedTarget));
        int distanceBlocks = (int) Math.ceil(dist);
        int fuelNeeded = Math.max(1000, ((distanceBlocks + 999) / 1000) * 1000);

        if (!core.hasFuel(fuelNeeded)) {
            world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 0.7F);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.no_fuel"), true);
            return;
        }

        IPlayerSelection selection = player.getCapability(PlayerSelection.CAPABILITY, null);
        if (selection == null || selection.getSelectedPositions().isEmpty()) {
            world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_NOTE_BASS, SoundCategory.BLOCKS, 1.0F, 0.6F);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.selection_empty"), true);
            return;
        }

        float crashChance = 0.0F;
        if (distanceBlocks > 1000 && !core.isHyperdriveActive()) {
            if (core.isSafetyLockEnabled()) {
                world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 0.8F);
                player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.safety_lock_distance"), true);
                return;
            } else {
                int excess = distanceBlocks - 1000;
                crashChance = Math.min(0.85F, ((excess + 99) / 100) * 0.15F);
            }
        }

        if (core.getCurrentHeat() > TileEntityTeleportCore.SAFE_HEAT_THRESHOLD) {
            if (core.isSafetyLockEnabled()) {
                world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 0.8F);
                player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.core_overheated"), true);
                return;
            } else {
                crashChance = Math.max(crashChance, 0.85F);
            }
        }

        LongSet positions = selection.getSelectedPositions();
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
                world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_NOTE_BASS, SoundCategory.BLOCKS, 1.0F, 0.6F);
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

        Set<BlockPos> machinery = ShipNetworkScanner.collectShipMachinery(world, corePos);
        for (BlockPos machPos : machinery) {
            relativeOffsets.add(machPos.subtract(corePos));
            selection.addPosition(machPos);
            minX = Math.min(minX, machPos.getX());
            minY = Math.min(minY, machPos.getY());
            minZ = Math.min(minZ, machPos.getZ());
            maxX = Math.max(maxX, machPos.getX());
            maxY = Math.max(maxY, machPos.getY());
            maxZ = Math.max(maxZ, machPos.getZ());
        }

        int minRelativeY = minY - corePos.getY();
        int maxRelativeY = maxY - corePos.getY();
        BlockPos initialTarget = computeTargetOrigin(world, requestedTarget, mode, isAnchorTarget, maxRelativeY, minRelativeY);

        ChunkSafetyManager chunkManager = new ChunkSafetyManager(world);
        if (!chunkManager.lockChunks(initialTarget, relativeOffsets)) {
            world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 0.5F);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.chunk_load_failed"), true);
            chunkManager.release();
            return;
        }

        BlockPos validTarget = CollisionDetector.findNearestValidPosition(world, initialTarget, relativeOffsets, corePos, 64);
        if (validTarget == null) {
            world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.8F, 0.6F);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.no_free_space"), true);
            chunkManager.release();
            return;
        }

        if (!validTarget.equals(initialTarget)) {
            chunkManager.lockChunks(validTarget, relativeOffsets);
        }

        if (!core.consumeFuel(fuelNeeded)) {
            world.playSound(null, corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 0.7F);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.no_fuel"), true);
            chunkManager.release();
            return;
        }

        VoxelSnapshot snapshot = createSnapshot(world, corePos, relativeOffsets, minX, minY, minZ, maxX, maxY, maxZ);
        if (!snapshot.getEntities().contains(player)) {
            snapshot.getEntities().add(player);
        }

        if (crashChance > 0.0F && world.rand.nextFloat() < crashChance) {
            handleCrash(world, player, corePos, validTarget, minRelativeY, snapshot, relativeOffsets);
            chunkManager.release();
            return;
        }

        TeleportEffects.playDepartureEffects(world, corePos, snapshot.getBounds());

        clearSource(world, corePos, relativeOffsets);
        pasteSnapshot(world, validTarget, snapshot);
        EntityRelocator.relocateEntities(snapshot.getEntities(), corePos, validTarget);

        BlockPos displacement = validTarget.subtract(corePos);
        selection.shift(displacement);
        ItemSelectionWand.syncSelection(player, selection);

        AxisAlignedBB destinationBounds = computeShiftedBounds(snapshot.getBounds(), corePos, validTarget);
        TeleportEffects.playArrivalEffects(world, validTarget, destinationBounds, snapshot.getEntities());

        TileEntity newTe = world.getTileEntity(validTarget);
        if (newTe instanceof TileEntityTeleportCore) {
            TileEntityTeleportCore newCore = (TileEntityTeleportCore) newTe;
            if (newCore.isHyperdriveActive()) {
                float heatToAdd = 15.0F + (float) (distanceBlocks / 2000.0) * 10.0F;
                newCore.addHeat(heatToAdd);
            } else {
                newCore.addHeat(100.0F);
            }
            newCore.syncToPlayer(player);
        }

        chunkManager.release();
        player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.teleport_success"), true);
    }

    private static void handleCrash(WorldServer world, EntityPlayerMP player, BlockPos corePos, BlockPos targetPos, int minRelativeY, VoxelSnapshot snapshot, Set<BlockPos> relativeOffsets) {
        float roll = world.rand.nextFloat();

        if (roll < 0.33F) {
            int drop = calculateMaxDrop(world, corePos, relativeOffsets);
            BlockPos dropOrigin = corePos.down(drop);
            clearSource(world, corePos, relativeOffsets);
            pasteSnapshot(world, dropOrigin, snapshot);
            EntityRelocator.relocateEntities(snapshot.getEntities(), corePos, dropOrigin);

            world.createExplosion(null, dropOrigin.getX() + 0.5, dropOrigin.getY() + 0.5, dropOrigin.getZ() + 0.5, 6.0F, true);
            float underPower = drop <= 20 ? 3.0F : 6.0F;
            BlockPos underPos = dropOrigin.down(Math.abs(minRelativeY) + 1);
            world.createExplosion(null, underPos.getX() + 0.5, Math.max(1, underPos.getY()), underPos.getZ() + 0.5, underPower, true);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.crash_departure"), true);
        } else if (roll < 0.67F) {
            int drop = calculateMaxDrop(world, targetPos, relativeOffsets);
            BlockPos dropTarget = targetPos.down(drop);
            clearSource(world, corePos, relativeOffsets);
            pasteSnapshot(world, dropTarget, snapshot);
            EntityRelocator.relocateEntities(snapshot.getEntities(), corePos, dropTarget);

            world.createExplosion(null, dropTarget.getX() + 0.5, dropTarget.getY() + 0.5, dropTarget.getZ() + 0.5, 6.0F, true);
            float underPower = drop <= 20 ? 3.0F : 6.0F;
            BlockPos underPos = dropTarget.down(Math.abs(minRelativeY) + 1);
            world.createExplosion(null, underPos.getX() + 0.5, Math.max(1, underPos.getY()), underPos.getZ() + 0.5, underPower, true);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.crash_destination"), true);
        } else {
            int actualX = (int) Math.round(corePos.getX() + (targetPos.getX() - corePos.getX()) * 0.6);
            int actualY = (int) Math.round(corePos.getY() + (targetPos.getY() - corePos.getY()) * 0.6);
            int actualZ = (int) Math.round(corePos.getZ() + (targetPos.getZ() - corePos.getZ()) * 0.6);
            BlockPos partialTarget = new BlockPos(actualX, actualY, actualZ);
            BlockPos safePartial = CollisionDetector.findNearestValidPosition(world, partialTarget, relativeOffsets, corePos, 64);
            if (safePartial == null) {
                safePartial = partialTarget;
            }

            clearSource(world, corePos, relativeOffsets);
            pasteSnapshot(world, safePartial, snapshot);
            EntityRelocator.relocateEntities(snapshot.getEntities(), corePos, safePartial);

            world.createExplosion(null, safePartial.getX() + 0.5, safePartial.getY() + 0.5, safePartial.getZ() + 0.5, 6.0F, true);
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.crash_partial"), true);
        }
    }

    private static int calculateMaxDrop(World world, BlockPos origin, Set<BlockPos> relativeOffsets) {
        int maxDrop = 0;
        for (int d = 1; d <= 256; d++) {
            boolean blocked = false;
            for (BlockPos offset : relativeOffsets) {
                BlockPos p = origin.add(offset).down(d);
                if (p.getY() < 1) {
                    blocked = true;
                    break;
                }
                BlockPos relToOrigin = p.subtract(origin);
                if (relativeOffsets.contains(relToOrigin)) {
                    continue;
                }
                IBlockState state = world.getBlockState(p);
                if (!state.getBlock().isAir(state, world, p) && !state.getBlock().isReplaceable(world, p)) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) {
                break;
            }
            maxDrop = d;
        }
        return maxDrop;
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

            TileEntity te = world.getTileEntity(targetPos);
            if (te != null) {
                te.readFromNBT(nbt);
                te.markDirty();
            } else {
                te = TileEntity.create(world, nbt);
                if (te != null) {
                    world.setTileEntity(targetPos, te);
                    te.markDirty();
                }
            }
            IBlockState state = world.getBlockState(targetPos);
            world.notifyBlockUpdate(targetPos, state, state, 3);
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
