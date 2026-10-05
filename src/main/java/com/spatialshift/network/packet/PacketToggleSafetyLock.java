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

public class PacketToggleSafetyLock implements IMessage {

    private BlockPos corePos;

    public PacketToggleSafetyLock() {
    }

    public PacketToggleSafetyLock(BlockPos corePos) {
        this.corePos = corePos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.corePos = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(corePos.toLong());
    }

    public static class Handler implements IMessageHandler<PacketToggleSafetyLock, IMessage> {
        @Override
        public IMessage onMessage(PacketToggleSafetyLock message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                TileEntity te = world.getTileEntity(message.corePos);
                if (te instanceof TileEntityTeleportCore) {
                    TileEntityTeleportCore core = (TileEntityTeleportCore) te;
                    core.toggleSafetyLock();
                    core.syncToPlayer(player);
                }
            });

            return null;
        }
    }
}
