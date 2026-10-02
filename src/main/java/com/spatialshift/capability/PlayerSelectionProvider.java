package com.spatialshift.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public class PlayerSelectionProvider implements ICapabilitySerializable<NBTTagCompound> {

    private final IPlayerSelection instance = new PlayerSelection();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == PlayerSelection.CAPABILITY;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == PlayerSelection.CAPABILITY) {
            return PlayerSelection.CAPABILITY.cast(instance);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return (NBTTagCompound) PlayerSelection.CAPABILITY.getStorage().writeNBT(PlayerSelection.CAPABILITY, instance, null);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        PlayerSelection.CAPABILITY.getStorage().readNBT(PlayerSelection.CAPABILITY, instance, null, nbt);
    }
}
