package com.spatialshift.tileentity;

import com.spatialshift.data.AnchorAccess;
import com.spatialshift.data.AnchorData;
import com.spatialshift.data.AnchorSavedData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.UUID;

public class TileEntityAnchor extends TileEntity {

    private UUID ownerUuid = UUID.randomUUID();
    private String anchorName = "Anchor";
    private AnchorAccess access = AnchorAccess.PUBLIC;

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        updateSavedData();
        markDirty();
    }

    public String getAnchorName() {
        return anchorName;
    }

    public void setAnchorName(String anchorName) {
        this.anchorName = anchorName;
        updateSavedData();
        markDirty();
    }

    public AnchorAccess getAccess() {
        return access;
    }

    public void setAccess(AnchorAccess access) {
        this.access = access;
        updateSavedData();
        markDirty();
    }

    public void toggleAccess() {
        setAccess(access.next());
    }

    public void updateSavedData() {
        if (world != null && !world.isRemote) {
            AnchorSavedData.get(world).registerAnchor(new AnchorData(
                pos,
                world.provider.getDimension(),
                anchorName,
                ownerUuid,
                access
            ));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            updateSavedData();
        }
    }

    @Override
    public void validate() {
        super.validate();
        if (world != null && !world.isRemote) {
            updateSavedData();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasUniqueId("Owner")) {
            this.ownerUuid = compound.getUniqueId("Owner");
        }
        if (compound.hasKey("AnchorName")) {
            this.anchorName = compound.getString("AnchorName");
        }
        if (compound.hasKey("Access")) {
            try {
                this.access = AnchorAccess.valueOf(compound.getString("Access"));
            } catch (IllegalArgumentException e) {
                this.access = AnchorAccess.PUBLIC;
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setUniqueId("Owner", ownerUuid);
        compound.setString("AnchorName", anchorName);
        compound.setString("Access", access.name());
        return compound;
    }
}
