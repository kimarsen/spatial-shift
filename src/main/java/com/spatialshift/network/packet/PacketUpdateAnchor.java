package com.spatialshift.network.packet;

import com.spatialshift.data.AnchorAccess;
import com.spatialshift.data.AnchorData;
import com.spatialshift.data.AnchorSavedData;
import com.spatialshift.network.PacketHandler;
import com.spatialshift.tileentity.TileEntityAnchor;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

public class PacketUpdateAnchor implements IMessage {

    private BlockPos pos;
    private String newName;
    private AnchorAccess access;

    public PacketUpdateAnchor() {
    }

    public PacketUpdateAnchor(BlockPos pos, String newName, AnchorAccess access) {
        this.pos = pos;
        this.newName = newName;
        this.access = access;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.newName = ByteBufUtils.readUTF8String(buf);
        this.access = AnchorAccess.values()[buf.readInt()];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        ByteBufUtils.writeUTF8String(buf, newName);
        buf.writeInt(access.ordinal());
    }

    public static class Handler implements IMessageHandler<PacketUpdateAnchor, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateAnchor message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                double distSq = player.getDistanceSq(message.pos.getX() + 0.5, message.pos.getY() + 0.5, message.pos.getZ() + 0.5);
                if (distSq > 64.0) {
                    return;
                }

                TileEntity te = world.getTileEntity(message.pos);
                if (te instanceof TileEntityAnchor) {
                    TileEntityAnchor anchor = (TileEntityAnchor) te;
                    if (anchor.getOwnerUuid().equals(player.getUniqueID())) {
                        String name = message.newName == null ? "" : message.newName.trim();
                        if (name.length() > 32) {
                            name = name.substring(0, 32);
                        }
                        if (name.isEmpty()) {
                            name = "Anchor";
                        }

                        anchor.setAnchorName(name);
                        anchor.setAccess(message.access);

                        IBlockState state = world.getBlockState(message.pos);
                        world.notifyBlockUpdate(message.pos, state, state, 3);

                        List<AnchorData> anchors = AnchorSavedData.get(world).getAvailableAnchors(player.getUniqueID());
                        PacketHandler.sendTo(new PacketSyncAnchors(anchors), player);
                    }
                }
            });

            return null;
        }
    }
}
