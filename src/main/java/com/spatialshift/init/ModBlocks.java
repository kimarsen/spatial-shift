package com.spatialshift.init;

import com.spatialshift.block.BlockAnchor;
import com.spatialshift.block.BlockCoreFrame;
import com.spatialshift.block.BlockFuelCompartmentCore;
import com.spatialshift.block.BlockFuelPipe;
import com.spatialshift.block.BlockHyperCable;
import com.spatialshift.block.BlockHyperdriveCore;
import com.spatialshift.block.BlockTeleportCore;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

public class ModBlocks {

    public static final BlockTeleportCore TELEPORT_CORE = new BlockTeleportCore();
    public static final BlockCoreFrame CORE_FRAME = new BlockCoreFrame();
    public static final BlockAnchor SPATIAL_ANCHOR = new BlockAnchor();
    public static final BlockHyperdriveCore HYPERDRIVE_CORE = new BlockHyperdriveCore();
    public static final BlockHyperCable HYPER_CABLE = new BlockHyperCable();
    public static final BlockFuelCompartmentCore FUEL_COMPARTMENT_CORE = new BlockFuelCompartmentCore();
    public static final BlockFuelPipe FUEL_PIPE = new BlockFuelPipe();

    public static final ItemBlock ITEM_TELEPORT_CORE = new ItemBlock(TELEPORT_CORE);
    public static final ItemBlock ITEM_CORE_FRAME = new ItemBlock(CORE_FRAME);
    public static final ItemBlock ITEM_SPATIAL_ANCHOR = new ItemBlock(SPATIAL_ANCHOR);
    public static final ItemBlock ITEM_HYPERDRIVE_CORE = new ItemBlock(HYPERDRIVE_CORE);
    public static final ItemBlock ITEM_HYPER_CABLE = new ItemBlock(HYPER_CABLE);
    public static final ItemBlock ITEM_FUEL_COMPARTMENT_CORE = new ItemBlock(FUEL_COMPARTMENT_CORE);
    public static final ItemBlock ITEM_FUEL_PIPE = new ItemBlock(FUEL_PIPE);

    static {
        ITEM_TELEPORT_CORE.setRegistryName(Objects.requireNonNull(TELEPORT_CORE.getRegistryName()));
        ITEM_CORE_FRAME.setRegistryName(Objects.requireNonNull(CORE_FRAME.getRegistryName()));
        ITEM_SPATIAL_ANCHOR.setRegistryName(Objects.requireNonNull(SPATIAL_ANCHOR.getRegistryName()));
        ITEM_HYPERDRIVE_CORE.setRegistryName(Objects.requireNonNull(HYPERDRIVE_CORE.getRegistryName()));
        ITEM_HYPER_CABLE.setRegistryName(Objects.requireNonNull(HYPER_CABLE.getRegistryName()));
        ITEM_FUEL_COMPARTMENT_CORE.setRegistryName(Objects.requireNonNull(FUEL_COMPARTMENT_CORE.getRegistryName()));
        ITEM_FUEL_PIPE.setRegistryName(Objects.requireNonNull(FUEL_PIPE.getRegistryName()));
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
            TELEPORT_CORE,
            CORE_FRAME,
            SPATIAL_ANCHOR,
            HYPERDRIVE_CORE,
            HYPER_CABLE,
            FUEL_COMPARTMENT_CORE,
            FUEL_PIPE
        );
    }

    @SubscribeEvent
    public void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
            ITEM_TELEPORT_CORE,
            ITEM_CORE_FRAME,
            ITEM_SPATIAL_ANCHOR,
            ITEM_HYPERDRIVE_CORE,
            ITEM_HYPER_CABLE,
            ITEM_FUEL_COMPARTMENT_CORE,
            ITEM_FUEL_PIPE
        );
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void registerModels(ModelRegistryEvent event) {
        registerBlockModel(TELEPORT_CORE);
        registerBlockModel(CORE_FRAME);
        registerBlockModel(SPATIAL_ANCHOR);
        registerBlockModel(HYPERDRIVE_CORE);
        registerBlockModel(HYPER_CABLE);
        registerBlockModel(FUEL_COMPARTMENT_CORE);
        registerBlockModel(FUEL_PIPE);
    }

    @SideOnly(Side.CLIENT)
    private void registerBlockModel(Block block) {
        ModelLoader.setCustomModelResourceLocation(
            Item.getItemFromBlock(block),
            0,
            new ModelResourceLocation(Objects.requireNonNull(block.getRegistryName()), "inventory")
        );
    }
}
