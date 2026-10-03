package com.spatialshift.capability;

import com.spatialshift.SpatialShift;
import com.spatialshift.data.WandMode;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlayerSelection implements IPlayerSelection {

    @CapabilityInject(IPlayerSelection.class)
    public static Capability<IPlayerSelection> CAPABILITY = null;

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(SpatialShift.MODID, "player_selection");

    private WandMode mode = WandMode.BOX;
    private BlockPos primaryPos = null;
    private BlockPos secondaryPos = null;
    private final LongSet selectedPositions = new LongOpenHashSet();

    @Override
    public WandMode getMode() {
        return mode;
    }

    @Override
    public void setMode(WandMode mode) {
        this.mode = mode;
    }

    @Override
    public BlockPos getPrimaryPos() {
        return primaryPos;
    }

    @Override
    public void setPrimaryPos(BlockPos pos) {
        this.primaryPos = pos;
    }

    @Override
    public BlockPos getSecondaryPos() {
        return secondaryPos;
    }

    @Override
    public void setSecondaryPos(BlockPos pos) {
        this.secondaryPos = pos;
    }

    @Override
    public LongSet getSelectedPositions() {
        return selectedPositions;
    }

    @Override
    public void addPosition(BlockPos pos) {
        selectedPositions.add(pos.toLong());
    }

    @Override
    public void removePosition(BlockPos pos) {
        selectedPositions.remove(pos.toLong());
    }

    @Override
    public boolean containsPosition(BlockPos pos) {
        return selectedPositions.contains(pos.toLong());
    }

    @Override
    public void clear() {
        primaryPos = null;
        secondaryPos = null;
        selectedPositions.clear();
    }

    @Override
    public void rebuildFromBox() {
        selectedPositions.clear();
        if (primaryPos == null || secondaryPos == null) {
            return;
        }

        int minX = Math.min(primaryPos.getX(), secondaryPos.getX());
        int maxX = Math.max(primaryPos.getX(), secondaryPos.getX());
        int minY = Math.min(primaryPos.getY(), secondaryPos.getY());
        int maxY = Math.max(primaryPos.getY(), secondaryPos.getY());
        int minZ = Math.min(primaryPos.getZ(), secondaryPos.getZ());
        int maxZ = Math.max(primaryPos.getZ(), secondaryPos.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    selectedPositions.add(new BlockPos(x, y, z).toLong());
                }
            }
        }
    }

    @Override
    public void setFromPackedArray(long[] packed) {
        selectedPositions.clear();
        for (long p : packed) {
            selectedPositions.add(p);
        }
    }

    @Override
    public void shift(BlockPos offset) {
        if (primaryPos != null) {
            primaryPos = primaryPos.add(offset);
        }
        if (secondaryPos != null) {
            secondaryPos = secondaryPos.add(offset);
        }
        if (!selectedPositions.isEmpty()) {
            LongSet shifted = new LongOpenHashSet(selectedPositions.size());
            LongIterator iter = selectedPositions.iterator();
            while (iter.hasNext()) {
                BlockPos pos = BlockPos.fromLong(iter.nextLong());
                shifted.add(pos.add(offset).toLong());
            }
            selectedPositions.clear();
            selectedPositions.addAll(shifted);
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(IDENTIFIER, new PlayerSelectionProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        IPlayerSelection oldCap = event.getOriginal().getCapability(CAPABILITY, null);
        IPlayerSelection newCap = event.getEntityPlayer().getCapability(CAPABILITY, null);

        if (oldCap != null && newCap != null) {
            newCap.setMode(oldCap.getMode());
            newCap.setPrimaryPos(oldCap.getPrimaryPos());
            newCap.setSecondaryPos(oldCap.getSecondaryPos());
            newCap.setFromPackedArray(oldCap.getSelectedPositions().toLongArray());
        }
    }
}
