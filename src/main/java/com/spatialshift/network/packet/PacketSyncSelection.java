package com.spatialshift.network.packet;

import com.spatialshift.capability.IPlayerSelection;
import com.spatialshift.capability.PlayerSelection;
import com.spatialshift.data.WandMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncSelection implements IMessage {

    private WandMode mode;
    private long[] positions;

    public PacketSyncSelection() {
    }

    public PacketSyncSelection(WandMode mode, long[] positions) {
        this.mode = mode;
        this.positions = positions;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mode = WandMode.values()[buf.readInt()];
        int length = buf.readInt();
        this.positions = new long[length];
        for (int i = 0; i < length; i++) {
            this.positions[i] = buf.readLong();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(mode.ordinal());
        buf.writeInt(positions.length);
        for (long pos : positions) {
            buf.writeLong(pos);
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncSelection, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncSelection message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) {
                    return;
                }
                IPlayerSelection selection = player.getCapability(PlayerSelection.CAPABILITY, null);
                if (selection != null) {
                    selection.setMode(message.mode);
                    selection.setFromPackedArray(message.positions);
                }
            });
            return null;
        }
    }
}
