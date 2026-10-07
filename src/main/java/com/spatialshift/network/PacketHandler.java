package com.spatialshift.network;

import com.spatialshift.SpatialShift;
import com.spatialshift.network.packet.PacketSyncAnchors;
import com.spatialshift.network.packet.PacketSyncSelection;
import com.spatialshift.network.packet.PacketTeleportRequest;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(SpatialShift.MODID);
    private static int packetId = 0;

    public static void registerMessages() {
        INSTANCE.registerMessage(PacketSyncSelection.Handler.class, PacketSyncSelection.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(PacketSyncAnchors.Handler.class, PacketSyncAnchors.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(PacketTeleportRequest.Handler.class, PacketTeleportRequest.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(com.spatialshift.network.packet.PacketSyncCoreState.Handler.class, com.spatialshift.network.packet.PacketSyncCoreState.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(com.spatialshift.network.packet.PacketToggleSafetyLock.Handler.class, com.spatialshift.network.packet.PacketToggleSafetyLock.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(com.spatialshift.network.packet.PacketUpdateAnchor.Handler.class, com.spatialshift.network.packet.PacketUpdateAnchor.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(com.spatialshift.network.packet.PacketActivateRadarArtifact.Handler.class, com.spatialshift.network.packet.PacketActivateRadarArtifact.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(com.spatialshift.network.packet.PacketConsentEyeTeleport.Handler.class, com.spatialshift.network.packet.PacketConsentEyeTeleport.class, packetId++, Side.SERVER);
    }

    public static void sendTo(IMessage message, EntityPlayerMP player) {
        INSTANCE.sendTo(message, player);
    }

    public static void sendToServer(IMessage message) {
        INSTANCE.sendToServer(message);
    }
}
