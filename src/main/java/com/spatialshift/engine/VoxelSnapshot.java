package com.spatialshift.engine;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoxelSnapshot {

    private final BlockPos origin;
    private final Map<BlockPos, IBlockState> states = new HashMap<>();
    private final Map<BlockPos, NBTTagCompound> tileEntities = new HashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private AxisAlignedBB bounds = null;

    public VoxelSnapshot(BlockPos origin) {
        this.origin = origin;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public Map<BlockPos, IBlockState> getStates() {
        return states;
    }

    public Map<BlockPos, NBTTagCompound> getTileEntities() {
        return tileEntities;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public AxisAlignedBB getBounds() {
        return bounds;
    }

    public void setBounds(AxisAlignedBB bounds) {
        this.bounds = bounds;
    }

    public void putBlock(BlockPos relativePos, IBlockState state) {
        states.put(relativePos, state);
    }

    public void putTileEntity(BlockPos relativePos, NBTTagCompound nbt) {
        tileEntities.put(relativePos, nbt);
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }
}
