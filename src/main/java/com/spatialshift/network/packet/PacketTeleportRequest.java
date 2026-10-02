package com.spatialshift.network.packet;

import com.spatialshift.data.TeleportMode;
import com.spatialshift.engine.TeleportEngine;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTeleportRequest implements IMessage {

    private BlockPos corePos;
    private BlockPos targetPos;
    private int targetDimension;
    private TeleportMode mode;

    public PacketTeleportRequest() {
    }

    public PacketTeleportRequest(BlockPos corePos, BlockPos targetPos, int targetDimension, TeleportMode mode) {
        this.corePos = corePos;
        this.targetPos = targetPos;
        this.targetDimension = targetDimension;
        this.mode = mode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.corePos = BlockPos.fromLong(buf.readLong());
        this.targetPos = BlockPos.fromLong(buf.readLong());
        this.targetDimension = buf.readInt();
        this.mode = TeleportMode.values()[buf.readInt()];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(corePos.toLong());
        buf.writeLong(targetPos.toLong());
        buf.writeInt(targetDimension);
        buf.writeInt(mode.ordinal());
    }

    public static class Handler implements IMessageHandler<PacketTeleportRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketTeleportRequest message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                if (player.getDistanceSq(message.corePos) > 64.0) {
                    return;
                }

                TileEntity te = world.getTileEntity(message.corePos);
                if (!(te instanceof TileEntityTeleportCore)) {
                    return;
                }

                TileEntityTeleportCore core = (TileEntityTeleportCore) te;
                if (!core.consumeFuel()) {
                    return;
                }

                TeleportEngine.executeTeleport(
                    world,
                    player,
                    core,
                    message.targetPos,
                    message.targetDimension,
                    message.mode
                );
            });

            return null;
        }
    }
}
