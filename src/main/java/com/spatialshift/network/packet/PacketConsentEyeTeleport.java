package com.spatialshift.network.packet;

import com.spatialshift.tileentity.TileEntityTeleportCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketConsentEyeTeleport implements IMessage {

    private BlockPos corePos;
    private boolean accept;

    public PacketConsentEyeTeleport() {
    }

    public PacketConsentEyeTeleport(BlockPos corePos, boolean accept) {
        this.corePos = corePos;
        this.accept = accept;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.corePos = BlockPos.fromLong(buf.readLong());
        this.accept = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(corePos.toLong());
        buf.writeBoolean(accept);
    }

    public static class Handler implements IMessageHandler<PacketConsentEyeTeleport, IMessage> {
        @Override
        public IMessage onMessage(PacketConsentEyeTeleport message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                TileEntity te = world.getTileEntity(message.corePos);
                if (te instanceof TileEntityTeleportCore) {
                    TileEntityTeleportCore core = (TileEntityTeleportCore) te;
                    core.handleEyeConsent(player, message.accept);
                }
            });

            return null;
        }
    }
}
