package com.spatialshift.fx;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Random;

public class TeleportEffects {

    private static final Random RANDOM = new Random();

    public static void playEffects(WorldServer world, BlockPos corePos, AxisAlignedBB bounds, List<Entity> entities) {
        playArrivalEffects(world, corePos, bounds, entities);
    }

    public static void playDepartureEffects(WorldServer world, BlockPos corePos, AxisAlignedBB bounds) {
        world.playSound(
            null,
            corePos.getX() + 0.5,
            corePos.getY() + 0.5,
            corePos.getZ() + 0.5,
            SoundEvents.BLOCK_END_PORTAL_SPAWN,
            SoundCategory.BLOCKS,
            3.0F,
            1.2F
        );

        spawnCoreParticles(world, corePos);

        if (bounds != null) {
            spawnPerimeterParticles(world, bounds);
        }
    }

    public static void playArrivalEffects(WorldServer world, BlockPos corePos, AxisAlignedBB bounds, List<Entity> entities) {
        world.playSound(
            null,
            corePos.getX() + 0.5,
            corePos.getY() + 0.5,
            corePos.getZ() + 0.5,
            SoundEvents.BLOCK_END_PORTAL_SPAWN,
            SoundCategory.BLOCKS,
            3.0F,
            1.0F
        );

        spawnCoreParticles(world, corePos);

        if (bounds != null) {
            spawnPerimeterParticles(world, bounds);
        }

        for (Entity entity : entities) {
            if (entity instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP) entity;

                world.playSound(
                    null,
                    player.posX,
                    player.posY,
                    player.posZ,
                    SoundEvents.BLOCK_PORTAL_TRAVEL,
                    SoundCategory.PLAYERS,
                    1.5F,
                    1.0F
                );

                player.connection.sendPacket(new SPacketSoundEffect(
                    SoundEvents.BLOCK_PORTAL_TRAVEL,
                    SoundCategory.PLAYERS,
                    player.posX,
                    player.posY,
                    player.posZ,
                    1.5F,
                    1.0F
                ));

                if (RANDOM.nextFloat() < 0.33F) {
                    player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
                }
            }
        }
    }

    private static void spawnCoreParticles(WorldServer world, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 1.0;
        double centerZ = pos.getZ() + 0.5;

        for (int i = 0; i < 40; i++) {
            double angle = (2 * Math.PI * i) / 40;
            double radius = 1.0 + RANDOM.nextDouble() * 2.0;
            double px = centerX + radius * Math.cos(angle);
            double py = centerY + RANDOM.nextDouble() * 3.0;
            double pz = centerZ + radius * Math.sin(angle);

            world.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticle(EnumParticleTypes.DRAGON_BREATH, px, py, pz, 1, 0.02, 0.05, 0.02, 0.01);
        }
    }

    private static void spawnPerimeterParticles(WorldServer world, AxisAlignedBB bounds) {
        double minX = bounds.minX;
        double maxX = bounds.maxX;
        double minY = bounds.minY;
        double maxY = bounds.maxY;
        double minZ = bounds.minZ;
        double maxZ = bounds.maxZ;

        for (double y = minY; y <= maxY; y += 1.0) {
            for (double x = minX - 1.0; x <= maxX + 1.0; x += 2.0) {
                world.spawnParticle(EnumParticleTypes.PORTAL, x, y, minZ - 0.5, 2, 0.05, 0.1, 0.05, 0.02);
                world.spawnParticle(EnumParticleTypes.PORTAL, x, y, maxZ + 0.5, 2, 0.05, 0.1, 0.05, 0.02);
            }
            for (double z = minZ - 1.0; z <= maxZ + 1.0; z += 2.0) {
                world.spawnParticle(EnumParticleTypes.PORTAL, minX - 0.5, y, z, 2, 0.05, 0.1, 0.05, 0.02);
                world.spawnParticle(EnumParticleTypes.PORTAL, maxX + 0.5, y, z, 2, 0.05, 0.1, 0.05, 0.02);
            }
        }
    }
}
