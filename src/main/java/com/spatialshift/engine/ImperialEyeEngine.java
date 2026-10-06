package com.spatialshift.engine;

import com.spatialshift.capability.IPlayerSelection;
import com.spatialshift.capability.PlayerSelection;
import com.spatialshift.fx.TeleportEffects;
import com.spatialshift.item.ItemSelectionWand;
import com.spatialshift.multiblock.MultiblockValidator;
import com.spatialshift.multiblock.ShipNetworkScanner;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class ImperialEyeEngine {

    public static class ValidationResult {
        public final boolean success;
        public final String errorMessageKey;
        public final EntityPlayerMP targetPlayer;
        public final TileEntityTeleportCore targetCore;

        public ValidationResult(boolean success, String errorMessageKey, EntityPlayerMP targetPlayer, TileEntityTeleportCore targetCore) {
            this.success = success;
            this.errorMessageKey = errorMessageKey;
            this.targetPlayer = targetPlayer;
            this.targetCore = targetCore;
        }
    }

    public static ValidationResult validateTarget(WorldServer world, EntityPlayerMP summoner, TileEntityTeleportCore summonerCore, String targetPlayerName) {
        if (targetPlayerName == null || targetPlayerName.trim().isEmpty()) {
            return new ValidationResult(false, "message.spatialshift.eye_player_not_found", null, null);
        }

        EntityPlayerMP targetPlayer = world.getMinecraftServer().getPlayerList().getPlayerByUsername(targetPlayerName.trim());
        if (targetPlayer == null) {
            return new ValidationResult(false, "message.spatialshift.eye_player_not_found", null, null);
        }

        if (targetPlayer.getUniqueID().equals(summoner.getUniqueID())) {
            return new ValidationResult(false, "message.spatialshift.eye_cannot_target_self", null, null);
        }

        if (targetPlayer.dimension != summoner.dimension) {
            return new ValidationResult(false, "message.spatialshift.eye_different_dimension", null, null);
        }

        TileEntityTeleportCore targetCore = findOwnedCoreNearPlayer(world, targetPlayer);
        if (targetCore == null) {
            return new ValidationResult(false, "message.spatialshift.eye_no_owned_core", null, null);
        }

        return new ValidationResult(true, null, targetPlayer, targetCore);
    }

    public static TileEntityTeleportCore findOwnedCoreNearPlayer(WorldServer world, EntityPlayerMP player) {
        BlockPos playerPos = new BlockPos(player);
        int chunkRadius = 3;
        int centerChunkX = playerPos.getX() >> 4;
        int centerChunkZ = playerPos.getZ() >> 4;

        TileEntityTeleportCore nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                if (world.isChunkGeneratedAt(cx, cz)) {
                    Chunk chunk = world.getChunk(cx, cz);
                    for (TileEntity te : chunk.getTileEntityMap().values()) {
                        if (te instanceof TileEntityTeleportCore) {
                            TileEntityTeleportCore core = (TileEntityTeleportCore) te;
                            if (core.getOwnerUuid() != null && core.getOwnerUuid().equals(player.getUniqueID())) {
                                if (MultiblockValidator.isValidPattern(world, core.getPos())) {
                                    double dSq = core.getPos().distanceSq(playerPos);
                                    if (dSq <= 50.0 * 50.0 && dSq < nearestDistSq) {
                                        nearestDistSq = dSq;
                                        nearest = core;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return nearest;
    }

    public static void applyInitiationEffects(WorldServer world, TileEntityTeleportCore summonerCore, EntityPlayerMP summoner, TileEntityTeleportCore targetCore, EntityPlayerMP targetPlayer) {
        BlockPos sumPos = summonerCore.getPos();
        BlockPos tarPos = targetCore.getPos();

        AxisAlignedBB sumArea = new AxisAlignedBB(sumPos).grow(50.0);
        AxisAlignedBB tarArea = new AxisAlignedBB(tarPos).grow(50.0);

        List<EntityLivingBase> sumEntities = world.getEntitiesWithinAABB(EntityLivingBase.class, sumArea);
        for (EntityLivingBase entity : sumEntities) {
            entity.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 200, 0, false, true));
        }

        List<EntityLivingBase> tarEntities = world.getEntitiesWithinAABB(EntityLivingBase.class, tarArea);
        for (EntityLivingBase entity : tarEntities) {
            entity.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 200, 0, false, true));
        }

        world.playSound(null, sumPos.getX() + 0.5, sumPos.getY() + 0.5, sumPos.getZ() + 0.5, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 0.9F, 0.7F);
        world.playSound(null, tarPos.getX() + 0.5, tarPos.getY() + 0.5, tarPos.getZ() + 0.5, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 0.9F, 0.7F);

        summoner.sendStatusMessage(new TextComponentTranslation("message.spatialshift.eye_started", targetPlayer.getName()), true);
        targetPlayer.sendStatusMessage(new TextComponentTranslation("message.spatialshift.eye_targeted", summoner.getName()), true);
    }

    public static boolean executeRelocation(WorldServer world, TileEntityTeleportCore summonerCore, EntityPlayerMP summoner, UUID targetPlayerUuid) {
        EntityPlayerMP targetPlayer = (EntityPlayerMP) world.getPlayerEntityByUUID(targetPlayerUuid);
        if (targetPlayer == null) {
            summoner.sendStatusMessage(new TextComponentTranslation("message.spatialshift.eye_player_not_found"), true);
            return false;
        }

        TileEntityTeleportCore targetCore = findOwnedCoreNearPlayer(world, targetPlayer);
        if (targetCore == null) {
            summoner.sendStatusMessage(new TextComponentTranslation("message.spatialshift.eye_no_owned_core"), true);
            return false;
        }

        BlockPos summonerCorePos = summonerCore.getPos();
        BlockPos targetCorePos = targetCore.getPos();

        IPlayerSelection summonerSel = summoner.getCapability(PlayerSelection.CAPABILITY, null);
        Set<BlockPos> summonerOffsets = collectShipOffsets(world, summonerCore, summonerSel);

        IPlayerSelection targetSel = targetPlayer.getCapability(PlayerSelection.CAPABILITY, null);
        Set<BlockPos> targetOffsets = collectShipOffsets(world, targetCore, targetSel);

        BlockPos destinationOrigin = findNearestAdjacentPosition(world, summonerCorePos, summonerOffsets, targetCorePos, targetOffsets);
        if (destinationOrigin == null) {
            summoner.sendStatusMessage(new TextComponentTranslation("message.spatialshift.no_free_space"), true);
            return false;
        }

        ChunkSafetyManager chunkManager = new ChunkSafetyManager(world);
        if (!chunkManager.lockChunks(targetCorePos, targetOffsets) || !chunkManager.lockChunks(destinationOrigin, targetOffsets)) {
            summoner.sendStatusMessage(new TextComponentTranslation("message.spatialshift.chunk_load_failed"), true);
            chunkManager.release();
            return false;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos offset : targetOffsets) {
            BlockPos p = targetCorePos.add(offset);
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }

        VoxelSnapshot snapshot = createSnapshot(world, targetCorePos, targetOffsets, minX, minY, minZ, maxX, maxY, maxZ);
        if (!snapshot.getEntities().contains(targetPlayer)) {
            snapshot.getEntities().add(targetPlayer);
        }

        TeleportEffects.playDepartureEffects(world, targetCorePos, snapshot.getBounds());

        clearSource(world, targetCorePos, targetOffsets);
        pasteSnapshot(world, destinationOrigin, snapshot);
        EntityRelocator.relocateEntities(snapshot.getEntities(), targetCorePos, destinationOrigin);

        if (targetSel != null) {
            BlockPos displacement = destinationOrigin.subtract(targetCorePos);
            targetSel.shift(displacement);
            ItemSelectionWand.syncSelection(targetPlayer, targetSel);
        }

        AxisAlignedBB destBounds = computeShiftedBounds(snapshot.getBounds(), targetCorePos, destinationOrigin);
        TeleportEffects.playArrivalEffects(world, destinationOrigin, destBounds, snapshot.getEntities());

        chunkManager.release();

        summoner.sendStatusMessage(new TextComponentTranslation("message.spatialshift.eye_success"), true);
        targetPlayer.sendStatusMessage(new TextComponentTranslation("message.spatialshift.eye_success"), true);
        return true;
    }

    public static Set<BlockPos> collectShipOffsets(WorldServer world, TileEntityTeleportCore core, IPlayerSelection selection) {
        BlockPos corePos = core.getPos();
        Set<BlockPos> relativeOffsets = new HashSet<>();

        if (selection != null && !selection.getSelectedPositions().isEmpty()) {
            LongIterator iter = selection.getSelectedPositions().iterator();
            while (iter.hasNext()) {
                BlockPos pos = BlockPos.fromLong(iter.nextLong());
                if (Math.abs(pos.getX() - corePos.getX()) <= 50 && Math.abs(pos.getZ() - corePos.getZ()) <= 50 && Math.abs(pos.getY() - corePos.getY()) <= 50) {
                    relativeOffsets.add(pos.subtract(corePos));
                }
            }
        } else {
            Queue<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            queue.add(corePos);
            visited.add(corePos);

            int maxBlocks = 4096;
            while (!queue.isEmpty() && relativeOffsets.size() < maxBlocks) {
                BlockPos cur = queue.poll();
                relativeOffsets.add(cur.subtract(corePos));

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) {
                                continue;
                            }
                            BlockPos next = cur.add(dx, dy, dz);
                            if (visited.add(next)) {
                                if (Math.abs(next.getX() - corePos.getX()) <= 50 && Math.abs(next.getY() - corePos.getY()) <= 50 && Math.abs(next.getZ() - corePos.getZ()) <= 50) {
                                    IBlockState state = world.getBlockState(next);
                                    if (!state.getBlock().isAir(state, world, next)) {
                                        queue.add(next);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Set<BlockPos> machinery = ShipNetworkScanner.collectShipMachinery(world, corePos);
        for (BlockPos machPos : machinery) {
            relativeOffsets.add(machPos.subtract(corePos));
        }

        return relativeOffsets;
    }

    public static BlockPos findNearestAdjacentPosition(WorldServer world, BlockPos summonerCorePos, Set<BlockPos> summonerOffsets, BlockPos targetCorePos, Set<BlockPos> targetOffsets) {
        int minSumX = Integer.MAX_VALUE;
        int minSumY = Integer.MAX_VALUE;
        int minSumZ = Integer.MAX_VALUE;
        int maxSumX = Integer.MIN_VALUE;
        int maxSumY = Integer.MIN_VALUE;
        int maxSumZ = Integer.MIN_VALUE;

        Set<BlockPos> summonerAbsPositions = new HashSet<>();
        for (BlockPos off : summonerOffsets) {
            BlockPos abs = summonerCorePos.add(off);
            summonerAbsPositions.add(abs);
            minSumX = Math.min(minSumX, off.getX());
            minSumY = Math.min(minSumY, off.getY());
            minSumZ = Math.min(minSumZ, off.getZ());
            maxSumX = Math.max(maxSumX, off.getX());
            maxSumY = Math.max(maxSumY, off.getY());
            maxSumZ = Math.max(maxSumZ, off.getZ());
        }

        int minTarX = Integer.MAX_VALUE;
        int minTarY = Integer.MAX_VALUE;
        int minTarZ = Integer.MAX_VALUE;
        int maxTarX = Integer.MIN_VALUE;
        int maxTarY = Integer.MIN_VALUE;
        int maxTarZ = Integer.MIN_VALUE;

        for (BlockPos off : targetOffsets) {
            minTarX = Math.min(minTarX, off.getX());
            minTarY = Math.min(minTarY, off.getY());
            minTarZ = Math.min(minTarZ, off.getZ());
            maxTarX = Math.max(maxTarX, off.getX());
            maxTarY = Math.max(maxTarY, off.getY());
            maxTarZ = Math.max(maxTarZ, off.getZ());
        }

        int targetSpanX = maxTarX - minTarX + 1;
        int targetSpanZ = maxTarZ - minTarZ + 1;

        int[][] directions = new int[][]{
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {-1, 1},
            {1, -1},
            {-1, -1}
        };

        for (int distance = 2; distance <= 64; distance += 4) {
            for (int[] dir : directions) {
                int candX;
                int candZ;

                if (dir[0] > 0) {
                    candX = summonerCorePos.getX() + maxSumX + distance - minTarX;
                } else if (dir[0] < 0) {
                    candX = summonerCorePos.getX() + minSumX - distance - maxTarX;
                } else {
                    candX = summonerCorePos.getX();
                }

                if (dir[1] > 0) {
                    candZ = summonerCorePos.getZ() + maxSumZ + distance - minTarZ;
                } else if (dir[1] < 0) {
                    candZ = summonerCorePos.getZ() + minSumZ - distance - maxTarZ;
                } else {
                    candZ = summonerCorePos.getZ();
                }

                BlockPos probe = new BlockPos(candX, 0, candZ);
                int topY = world.getTopSolidOrLiquidBlock(probe).getY();
                int surfaceY = topY + 1 - minTarY;
                int deckY = summonerCorePos.getY() + minSumY - minTarY;
                int candY = Math.max(deckY, surfaceY);

                for (int dy = 0; dy <= 16; dy++) {
                    int testY = candY + dy;
                    if (testY + maxTarY > 255 || testY + minTarY < 1) {
                        continue;
                    }

                    BlockPos candidateOrigin = new BlockPos(candX, testY, candZ);

                    boolean overlapsSummoner = false;
                    for (BlockPos toff : targetOffsets) {
                        if (summonerAbsPositions.contains(candidateOrigin.add(toff))) {
                            overlapsSummoner = true;
                            break;
                        }
                    }

                    if (overlapsSummoner) {
                        continue;
                    }

                    if (CollisionDetector.isAreaFree(world, candidateOrigin, targetOffsets, targetCorePos)) {
                        return candidateOrigin;
                    }
                }
            }
        }

        return null;
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
