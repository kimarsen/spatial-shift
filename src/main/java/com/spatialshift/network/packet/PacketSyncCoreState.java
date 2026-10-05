package com.spatialshift.network.packet;

import com.spatialshift.tileentity.TileEntityTeleportCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncCoreState implements IMessage {

    private BlockPos corePos;
    private float currentHeat;
    private boolean safetyLockEnabled;
    private boolean hyperdriveActive;
    private int totalFuel;
    private int maxFuel;
    private int warmupTicks;
    private int totalWarmupTicks;

    public PacketSyncCoreState() {
    }

    public PacketSyncCoreState(BlockPos corePos, float currentHeat, boolean safetyLockEnabled, boolean hyperdriveActive, int totalFuel, int maxFuel, int warmupTicks, int totalWarmupTicks) {
        this.corePos = corePos;
        this.currentHeat = currentHeat;
        this.safetyLockEnabled = safetyLockEnabled;
        this.hyperdriveActive = hyperdriveActive;
        this.totalFuel = totalFuel;
        this.maxFuel = maxFuel;
        this.warmupTicks = warmupTicks;
        this.totalWarmupTicks = totalWarmupTicks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.corePos = BlockPos.fromLong(buf.readLong());
        this.currentHeat = buf.readFloat();
        this.safetyLockEnabled = buf.readBoolean();
        this.hyperdriveActive = buf.readBoolean();
        this.totalFuel = buf.readInt();
        this.maxFuel = buf.readInt();
        this.warmupTicks = buf.readInt();
        this.totalWarmupTicks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(corePos.toLong());
        buf.writeFloat(currentHeat);
        buf.writeBoolean(safetyLockEnabled);
        buf.writeBoolean(hyperdriveActive);
        buf.writeInt(totalFuel);
        buf.writeInt(maxFuel);
        buf.writeInt(warmupTicks);
        buf.writeInt(totalWarmupTicks);
    }

    public static class Handler implements IMessageHandler<PacketSyncCoreState, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncCoreState message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().world != null) {
                    TileEntity te = Minecraft.getMinecraft().world.getTileEntity(message.corePos);
                    if (te instanceof TileEntityTeleportCore) {
                        TileEntityTeleportCore core = (TileEntityTeleportCore) te;
                        core.setClientSyncedState(
                            message.currentHeat,
                            message.safetyLockEnabled,
                            message.hyperdriveActive,
                            message.totalFuel,
                            message.maxFuel,
                            message.warmupTicks,
                            message.totalWarmupTicks
                        );
                    }
                }
            });
            return null;
        }
    }
}
