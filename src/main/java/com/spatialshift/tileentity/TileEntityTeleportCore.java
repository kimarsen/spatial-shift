package com.spatialshift.tileentity;

import com.spatialshift.init.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class TileEntityTeleportCore extends TileEntity {

    private final ItemStackHandler fuelInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() == ModItems.DIMENSIONAL_FUEL;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    public ItemStackHandler getFuelInventory() {
        return fuelInventory;
    }

    public boolean hasFuel() {
        ItemStack stack = fuelInventory.getStackInSlot(0);
        return !stack.isEmpty() && stack.getItem() == ModItems.DIMENSIONAL_FUEL && stack.getCount() > 0;
    }

    public boolean consumeFuel() {
        ItemStack stack = fuelInventory.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() == ModItems.DIMENSIONAL_FUEL) {
            fuelInventory.extractItem(0, 1, false);
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("FuelInventory")) {
            fuelInventory.deserializeNBT(compound.getCompoundTag("FuelInventory"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("FuelInventory", fuelInventory.serializeNBT());
        return compound;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(fuelInventory);
        }
        return super.getCapability(capability, facing);
    }
}
