package com.spatialshift.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AnchorSavedData extends WorldSavedData {

    private static final String DATA_NAME = "spatialshift_anchors";

    private final Map<Long, AnchorData> anchors = new HashMap<>();

    public AnchorSavedData() {
        super(DATA_NAME);
    }

    public AnchorSavedData(String name) {
        super(name);
    }

    public static AnchorSavedData get(World world) {
        World rootWorld = (world.getMinecraftServer() != null) ? world.getMinecraftServer().getEntityWorld() : world;
        MapStorage storage = rootWorld.getMapStorage();
        if (storage == null) {
            return new AnchorSavedData();
        }
        AnchorSavedData data = (AnchorSavedData) storage.getOrLoadData(AnchorSavedData.class, DATA_NAME);
        if (data == null) {
            data = new AnchorSavedData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public void registerAnchor(AnchorData anchor) {
        anchors.put(anchor.getPos().toLong(), anchor);
        markDirty();
    }

    public void removeAnchor(long posLong) {
        if (anchors.remove(posLong) != null) {
            markDirty();
        }
    }

    public AnchorData getAnchor(long posLong) {
        return anchors.get(posLong);
    }

    public List<AnchorData> getAvailableAnchors(UUID playerUuid) {
        List<AnchorData> available = new ArrayList<>();
        for (AnchorData anchor : anchors.values()) {
            if (anchor.canAccess(playerUuid)) {
                available.add(anchor);
            }
        }
        return Collections.unmodifiableList(available);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        anchors.clear();
        NBTTagList list = nbt.getTagList("Anchors", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            AnchorData data = AnchorData.readFromNbt(tag);
            anchors.put(data.getPos().toLong(), data);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (AnchorData data : anchors.values()) {
            list.appendTag(data.writeToNbt());
        }
        compound.setTag("Anchors", list);
        return compound;
    }
}
