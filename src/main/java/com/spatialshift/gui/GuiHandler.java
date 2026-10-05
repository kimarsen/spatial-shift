package com.spatialshift.gui;

import com.spatialshift.container.ContainerAnchor;
import com.spatialshift.container.ContainerTeleportCore;
import com.spatialshift.data.AnchorData;
import com.spatialshift.data.AnchorSavedData;
import com.spatialshift.network.PacketHandler;
import com.spatialshift.network.packet.PacketSyncAnchors;
import com.spatialshift.tileentity.TileEntityAnchor;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;
import java.util.List;

public class GuiHandler implements IGuiHandler {

    public static final int GUI_CORE = 1;
    public static final int GUI_ANCHOR = 2;

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_CORE) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te instanceof TileEntityTeleportCore) {
                if (player instanceof EntityPlayerMP) {
                    List<AnchorData> anchors = AnchorSavedData.get(world).getAvailableAnchors(player.getUniqueID());
                    PacketHandler.sendTo(new PacketSyncAnchors(anchors), (EntityPlayerMP) player);
                }
                return new ContainerTeleportCore(player.inventory, (TileEntityTeleportCore) te);
            }
        } else if (ID == GUI_ANCHOR) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te instanceof TileEntityAnchor) {
                return new ContainerAnchor((TileEntityAnchor) te);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_CORE) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te instanceof TileEntityTeleportCore) {
                return new GuiTeleportCore(player.inventory, (TileEntityTeleportCore) te);
            }
        } else if (ID == GUI_ANCHOR) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te instanceof TileEntityAnchor) {
                return new GuiAnchor((TileEntityAnchor) te);
            }
        }
        return null;
    }
}
