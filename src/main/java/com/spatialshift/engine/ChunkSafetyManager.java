package com.spatialshift.engine;

import com.spatialshift.SpatialShift;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;

import java.util.HashSet;
import java.util.Set;

public class ChunkSafetyManager {

    private final WorldServer world;
    private final Set<ChunkPos> loadedChunks = new HashSet<>();
    private ForgeChunkManager.Ticket ticket;

    public ChunkSafetyManager(WorldServer world) {
        this.world = world;
    }

    public boolean lockChunks(BlockPos origin, Set<BlockPos> relativeOffsets) {
        int minX = origin.getX();
        int maxX = origin.getX();
        int minZ = origin.getZ();
        int maxZ = origin.getZ();

        for (BlockPos offset : relativeOffsets) {
            BlockPos targetPos = origin.add(offset);
            minX = Math.min(minX, targetPos.getX());
            maxX = Math.max(maxX, targetPos.getX());
            minZ = Math.min(minZ, targetPos.getZ());
            maxZ = Math.max(maxZ, targetPos.getZ());
        }

        int minChunkX = (minX >> 4) - 1;
        int maxChunkX = (maxX >> 4) + 1;
        int minChunkZ = (minZ >> 4) - 1;
        int maxChunkZ = (maxZ >> 4) + 1;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                loadedChunks.add(new ChunkPos(cx, cz));
            }
        }

        if (ticket == null) {
            ticket = ForgeChunkManager.requestTicket(SpatialShift.instance, world, ForgeChunkManager.Type.NORMAL);
        }
        if (ticket != null) {
            for (ChunkPos cp : loadedChunks) {
                ForgeChunkManager.forceChunk(ticket, cp);
            }
        }

        for (ChunkPos cp : loadedChunks) {
            Chunk chunk = world.getChunkProvider().provideChunk(cp.x, cp.z);
            if (chunk == null) {
                release();
                return false;
            }
        }

        return true;
    }

    public void release() {
        if (ticket != null) {
            for (ChunkPos cp : loadedChunks) {
                ForgeChunkManager.unforceChunk(ticket, cp);
            }
            ForgeChunkManager.releaseTicket(ticket);
            ticket = null;
        }
        loadedChunks.clear();
    }
}
