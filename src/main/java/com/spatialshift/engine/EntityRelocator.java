package com.spatialshift.engine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class EntityRelocator {

    public static void relocateEntities(List<Entity> entities, BlockPos sourceOrigin, BlockPos destinationOrigin) {
        double offsetX = destinationOrigin.getX() - sourceOrigin.getX();
        double offsetY = destinationOrigin.getY() - sourceOrigin.getY();
        double offsetZ = destinationOrigin.getZ() - sourceOrigin.getZ();

        for (Entity entity : entities) {
            double targetX = entity.posX + offsetX;
            double targetY = entity.posY + offsetY;
            double targetZ = entity.posZ + offsetZ;

            if (entity instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP) entity;
                player.connection.setPlayerLocation(targetX, targetY, targetZ, player.rotationYaw, player.rotationPitch);
            } else {
                entity.setLocationAndAngles(targetX, targetY, targetZ, entity.rotationYaw, entity.rotationPitch);
            }
        }
    }
}
