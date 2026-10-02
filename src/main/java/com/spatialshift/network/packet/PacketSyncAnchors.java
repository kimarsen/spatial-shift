package com.spatialshift.network.packet;

import com.spatialshift.client.ClientAnchorCache;
import com.spatialshift.data.AnchorAccess;
import com.spatialshift.data.AnchorData;
import com.spatialshift.gui.GuiTeleportCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketSyncAnchors implements IMessage {

    private List<AnchorData> anchors;

    public PacketSyncAnchors() {
        this.anchors = new ArrayList<>();
    }

    public PacketSyncAnchors(List<AnchorData> anchors) {
        this.anchors = anchors;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        anchors = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            long pos = buf.readLong();
            int dim = buf.readInt();
            String name = ByteBufUtils.readUTF8String(buf);
            long most = buf.readLong();
            long least = buf.readLong();
            int access = buf.readInt();
            anchors.add(new AnchorData(
                BlockPos.fromLong(pos),
                dim,
                name,
                new UUID(most, least),
                AnchorAccess.values()[access]
            ));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(anchors.size());
        for (AnchorData anchor : anchors) {
            buf.writeLong(anchor.getPos().toLong());
            buf.writeInt(anchor.getDimensionId());
            ByteBufUtils.writeUTF8String(buf, anchor.getName());
            buf.writeLong(anchor.getOwnerUuid().getMostSignificantBits());
            buf.writeLong(anchor.getOwnerUuid().getLeastSignificantBits());
            buf.writeInt(anchor.getAccess().ordinal());
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncAnchors, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncAnchors message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientAnchorCache.update(message.anchors);
                GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
                if (currentScreen instanceof GuiTeleportCore) {
                    ((GuiTeleportCore) currentScreen).updateAnchors(message.anchors);
                }
            });
            return null;
        }
    }
}
