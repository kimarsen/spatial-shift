package com.spatialshift;

import com.spatialshift.capability.PlayerSelection;
import com.spatialshift.capability.PlayerSelectionStorage;
import com.spatialshift.capability.IPlayerSelection;
import com.spatialshift.client.SelectionRenderHandler;
import com.spatialshift.gui.GuiHandler;
import com.spatialshift.init.ModBlocks;
import com.spatialshift.init.ModItems;
import com.spatialshift.network.PacketHandler;
import com.spatialshift.tileentity.TileEntityAnchor;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
    modid = SpatialShift.MODID,
    name = SpatialShift.NAME,
    version = SpatialShift.VERSION,
    acceptedMinecraftVersions = "[1.12.2]"
)
public class SpatialShift {

    public static final String MODID = "spatialshift";
    public static final String NAME = "Spatial Shift";
    public static final String VERSION = "1.6.1";

    @Mod.Instance(MODID)
    public static SpatialShift instance;

    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(MODID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModItems.SELECTION_WAND);
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PacketHandler.registerMessages();

        CapabilityManager.INSTANCE.register(
            IPlayerSelection.class,
            new PlayerSelectionStorage(),
            PlayerSelection::new
        );

        GameRegistry.registerTileEntity(
            TileEntityTeleportCore.class,
            new ResourceLocation(MODID, "teleport_core")
        );
        GameRegistry.registerTileEntity(
            TileEntityAnchor.class,
            new ResourceLocation(MODID, "spatial_anchor")
        );
        GameRegistry.registerTileEntity(
            com.spatialshift.tileentity.TileEntityHyperdriveCore.class,
            new ResourceLocation(MODID, "hyperdrive_core")
        );
        GameRegistry.registerTileEntity(
            com.spatialshift.tileentity.TileEntityFuelCompartment.class,
            new ResourceLocation(MODID, "fuel_compartment")
        );

        ForgeChunkManager.setForcedChunkLoadingCallback(instance, (tickets, world) -> {
            for (ForgeChunkManager.Ticket ticket : tickets) {
                if (ticket.getModData().hasKey("AnchorPos")) {
                    net.minecraft.util.math.BlockPos p = net.minecraft.util.math.BlockPos.fromLong(ticket.getModData().getLong("AnchorPos"));
                    ForgeChunkManager.forceChunk(ticket, new net.minecraft.util.math.ChunkPos(p));
                }
            }
        });

        MinecraftForge.EVENT_BUS.register(new ModBlocks());
        MinecraftForge.EVENT_BUS.register(new ModItems());
        MinecraftForge.EVENT_BUS.register(PlayerSelection.class);

        if (event.getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new SelectionRenderHandler());
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
    }
}
