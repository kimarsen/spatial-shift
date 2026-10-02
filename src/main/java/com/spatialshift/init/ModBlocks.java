package com.spatialshift.init;

import com.spatialshift.block.BlockAnchor;
import com.spatialshift.block.BlockCoreFrame;
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

    public static final ItemBlock ITEM_TELEPORT_CORE = new ItemBlock(TELEPORT_CORE);
    public static final ItemBlock ITEM_CORE_FRAME = new ItemBlock(CORE_FRAME);
    public static final ItemBlock ITEM_SPATIAL_ANCHOR = new ItemBlock(SPATIAL_ANCHOR);

    static {
        ITEM_TELEPORT_CORE.setRegistryName(Objects.requireNonNull(TELEPORT_CORE.getRegistryName()));
        ITEM_CORE_FRAME.setRegistryName(Objects.requireNonNull(CORE_FRAME.getRegistryName()));
        ITEM_SPATIAL_ANCHOR.setRegistryName(Objects.requireNonNull(SPATIAL_ANCHOR.getRegistryName()));
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
            TELEPORT_CORE,
            CORE_FRAME,
            SPATIAL_ANCHOR
        );
    }

    @SubscribeEvent
    public void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
            ITEM_TELEPORT_CORE,
            ITEM_CORE_FRAME,
            ITEM_SPATIAL_ANCHOR
        );
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void registerModels(ModelRegistryEvent event) {
        registerBlockModel(TELEPORT_CORE);
        registerBlockModel(CORE_FRAME);
        registerBlockModel(SPATIAL_ANCHOR);
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
