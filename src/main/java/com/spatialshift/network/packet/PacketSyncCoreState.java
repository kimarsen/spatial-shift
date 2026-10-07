package com.spatialshift.network.packet;

import com.spatialshift.tileentity.TileEntityTeleportCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
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
    private int trackingTicks;
    private String trackedPlayerName;
    private double trackedPlayerX;
    private double trackedPlayerZ;
    private int trackedPlayerDim;
    private int imperialEyeTicks;
    private String eyeTargetPlayerName;
    private boolean eyeConsentReceived;
    private int incomingEyeTicks;
    private String incomingEyeInitiatorName;
    private boolean incomingEyeConsentGiven;

    public PacketSyncCoreState() {
    }

    public PacketSyncCoreState(
        BlockPos corePos,
        float currentHeat,
        boolean safetyLockEnabled,
        boolean hyperdriveActive,
        int totalFuel,
        int maxFuel,
        int warmupTicks,
        int totalWarmupTicks,
        int trackingTicks,
        String trackedPlayerName,
        double trackedPlayerX,
        double trackedPlayerZ,
        int trackedPlayerDim,
        int imperialEyeTicks,
        String eyeTargetPlayerName,
        boolean eyeConsentReceived,
        int incomingEyeTicks,
        String incomingEyeInitiatorName,
        boolean incomingEyeConsentGiven
    ) {
        this.corePos = corePos;
        this.currentHeat = currentHeat;
        this.safetyLockEnabled = safetyLockEnabled;
        this.hyperdriveActive = hyperdriveActive;
        this.totalFuel = totalFuel;
        this.maxFuel = maxFuel;
        this.warmupTicks = warmupTicks;
        this.totalWarmupTicks = totalWarmupTicks;
        this.trackingTicks = trackingTicks;
        this.trackedPlayerName = trackedPlayerName != null ? trackedPlayerName : "";
        this.trackedPlayerX = trackedPlayerX;
        this.trackedPlayerZ = trackedPlayerZ;
        this.trackedPlayerDim = trackedPlayerDim;
        this.imperialEyeTicks = imperialEyeTicks;
        this.eyeTargetPlayerName = eyeTargetPlayerName != null ? eyeTargetPlayerName : "";
        this.eyeConsentReceived = eyeConsentReceived;
        this.incomingEyeTicks = incomingEyeTicks;
        this.incomingEyeInitiatorName = incomingEyeInitiatorName != null ? incomingEyeInitiatorName : "";
        this.incomingEyeConsentGiven = incomingEyeConsentGiven;
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
        this.trackingTicks = buf.readInt();
        this.trackedPlayerName = ByteBufUtils.readUTF8String(buf);
        this.trackedPlayerX = buf.readDouble();
        this.trackedPlayerZ = buf.readDouble();
        this.trackedPlayerDim = buf.readInt();
        this.imperialEyeTicks = buf.readInt();
        this.eyeTargetPlayerName = ByteBufUtils.readUTF8String(buf);
        this.eyeConsentReceived = buf.readBoolean();
        this.incomingEyeTicks = buf.readInt();
        this.incomingEyeInitiatorName = ByteBufUtils.readUTF8String(buf);
        this.incomingEyeConsentGiven = buf.readBoolean();
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
        buf.writeInt(trackingTicks);
        ByteBufUtils.writeUTF8String(buf, trackedPlayerName);
        buf.writeDouble(trackedPlayerX);
        buf.writeDouble(trackedPlayerZ);
        buf.writeInt(trackedPlayerDim);
        buf.writeInt(imperialEyeTicks);
        ByteBufUtils.writeUTF8String(buf, eyeTargetPlayerName);
        buf.writeBoolean(eyeConsentReceived);
        buf.writeInt(incomingEyeTicks);
        ByteBufUtils.writeUTF8String(buf, incomingEyeInitiatorName);
        buf.writeBoolean(incomingEyeConsentGiven);
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
                            message.totalWarmupTicks,
                            message.trackingTicks,
                            message.trackedPlayerName,
                            message.trackedPlayerX,
                            message.trackedPlayerZ,
                            message.trackedPlayerDim,
                            message.imperialEyeTicks,
                            message.eyeTargetPlayerName,
                            message.eyeConsentReceived,
                            message.incomingEyeTicks,
                            message.incomingEyeInitiatorName,
                            message.incomingEyeConsentGiven
                        );
                    }
                }
            });
            return null;
        }
    }
}
