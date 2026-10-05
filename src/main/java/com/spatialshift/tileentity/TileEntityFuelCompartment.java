package com.spatialshift.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntityFuelCompartment extends TileEntity {

    public static final int MAX_FUEL = 10000;
    private int storedFuel = 0;

    public int getStoredFuel() {
        return storedFuel;
    }

    public void setStoredFuel(int storedFuel) {
        this.storedFuel = Math.min(MAX_FUEL, Math.max(0, storedFuel));
        markDirty();
    }

    public int addFuel(int amount) {
        int space = MAX_FUEL - storedFuel;
        int toAdd = Math.min(space, amount);
        storedFuel += toAdd;
        markDirty();
        return toAdd;
    }

    public int extractFuel(int amount) {
        int toExtract = Math.min(storedFuel, amount);
        storedFuel -= toExtract;
        markDirty();
        return toExtract;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("StoredFuel")) {
            storedFuel = compound.getInteger("StoredFuel");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("StoredFuel", storedFuel);
        return compound;
    }
}
