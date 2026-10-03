package com.spatialshift.capability;

import com.spatialshift.data.WandMode;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;

public class PlayerSelectionStorage implements Capability.IStorage<IPlayerSelection> {

    @Override
    public NBTBase writeNBT(Capability<IPlayerSelection> capability, IPlayerSelection instance, EnumFacing side) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Mode", instance.getMode().name());

        if (instance.getPrimaryPos() != null) {
            tag.setLong("Primary", instance.getPrimaryPos().toLong());
        }
        if (instance.getSecondaryPos() != null) {
            tag.setLong("Secondary", instance.getSecondaryPos().toLong());
        }

        LongSet positions = instance.getSelectedPositions();
        NBTTagList list = new NBTTagList();
        for (long pos : positions) {
            list.appendTag(new NBTTagLong(pos));
        }
        tag.setTag("Positions", list);
        return tag;
    }

    @Override
    public void readNBT(Capability<IPlayerSelection> capability, IPlayerSelection instance, EnumFacing side, NBTBase nbt) {
        if (!(nbt instanceof NBTTagCompound)) {
            return;
        }

        NBTTagCompound tag = (NBTTagCompound) nbt;
        if (tag.hasKey("Mode")) {
            try {
                instance.setMode(WandMode.valueOf(tag.getString("Mode")));
            } catch (IllegalArgumentException e) {
                instance.setMode(WandMode.BOX);
            }
        }
        if (tag.hasKey("Primary")) {
            instance.setPrimaryPos(BlockPos.fromLong(tag.getLong("Primary")));
        }
        if (tag.hasKey("Secondary")) {
            instance.setSecondaryPos(BlockPos.fromLong(tag.getLong("Secondary")));
        }
        if (tag.hasKey("Positions", Constants.NBT.TAG_LIST)) {
            NBTTagList list = tag.getTagList("Positions", Constants.NBT.TAG_LONG);
            long[] array = new long[list.tagCount()];
            for (int i = 0; i < list.tagCount(); i++) {
                NBTBase base = list.get(i);
                if (base instanceof NBTPrimitive) {
                    array[i] = ((NBTPrimitive) base).getLong();
                }
            }
            instance.setFromPackedArray(array);
        }
    }
}
