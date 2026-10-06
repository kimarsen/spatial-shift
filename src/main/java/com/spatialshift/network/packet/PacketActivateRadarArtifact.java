package com.spatialshift.network.packet;

import com.spatialshift.tileentity.TileEntityTeleportCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketActivateRadarArtifact implements IMessage {

    private BlockPos corePos;
    private int actionType;
    private String targetPlayerName;

    public PacketActivateRadarArtifact() {
    }

    public PacketActivateRadarArtifact(BlockPos corePos, int actionType, String targetPlayerName) {
        this.corePos = corePos;
        this.actionType = actionType;
        this.targetPlayerName = targetPlayerName != null ? targetPlayerName : "";
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.corePos = BlockPos.fromLong(buf.readLong());
        this.actionType = buf.readInt();
        this.targetPlayerName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(corePos.toLong());
        buf.writeInt(actionType);
        ByteBufUtils.writeUTF8String(buf, targetPlayerName);
    }

    public static class Handler implements IMessageHandler<PacketActivateRadarArtifact, IMessage> {
        @Override
        public IMessage onMessage(PacketActivateRadarArtifact message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                TileEntity te = world.getTileEntity(message.corePos);
                if (te instanceof TileEntityTeleportCore) {
                    TileEntityTeleportCore core = (TileEntityTeleportCore) te;
                    if (message.actionType == 1) {
                        core.activateDivineSight(player, message.targetPlayerName);
                    } else if (message.actionType == 2) {
                        core.activateImperialEye(player, message.targetPlayerName);
                    }
                }
            });

            return null;
        }
    }
}
