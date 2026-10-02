package com.spatialshift.init;

import com.spatialshift.SpatialShift;
import com.spatialshift.item.ItemSelectionWand;
import com.spatialshift.item.ItemTeleportFuel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

public class ModItems {

    public static final ItemSelectionWand SELECTION_WAND = new ItemSelectionWand();
    public static final ItemTeleportFuel DIMENSIONAL_FUEL = new ItemTeleportFuel();

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
            SELECTION_WAND,
            DIMENSIONAL_FUEL
        );
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void registerModels(ModelRegistryEvent event) {
        registerModel(SELECTION_WAND);
        registerModel(DIMENSIONAL_FUEL);
    }

    @SideOnly(Side.CLIENT)
    private void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(
            item,
            0,
            new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory")
        );
    }
}
