package com.spatialshift.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.UUID;

public class AnchorData {

    private final BlockPos pos;
    private final int dimensionId;
    private final String name;
    private final UUID ownerUuid;
    private final AnchorAccess access;

    public AnchorData(BlockPos pos, int dimensionId, String name, UUID ownerUuid, AnchorAccess access) {
        this.pos = Objects.requireNonNull(pos);
        this.dimensionId = dimensionId;
        this.name = Objects.requireNonNull(name);
        this.ownerUuid = Objects.requireNonNull(ownerUuid);
        this.access = Objects.requireNonNull(access);
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public AnchorAccess getAccess() {
        return access;
    }

    public boolean canAccess(UUID playerUuid) {
        return access == AnchorAccess.PUBLIC || ownerUuid.equals(playerUuid);
    }

    public NBTTagCompound writeToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("Pos", pos.toLong());
        tag.setInteger("Dim", dimensionId);
        tag.setString("Name", name);
        tag.setUniqueId("Owner", ownerUuid);
        tag.setString("Access", access.name());
        return tag;
    }

    public static AnchorData readFromNbt(NBTTagCompound tag) {
        BlockPos pos = BlockPos.fromLong(tag.getLong("Pos"));
        int dim = tag.getInteger("Dim");
        String name = tag.getString("Name");
        UUID owner = tag.getUniqueId("Owner");
        AnchorAccess access = AnchorAccess.valueOf(tag.getString("Access"));
        return new AnchorData(pos, dim, name, owner, access);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnchorData)) return false;
        AnchorData that = (AnchorData) o;
        return dimensionId == that.dimensionId && pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, dimensionId);
    }
}
