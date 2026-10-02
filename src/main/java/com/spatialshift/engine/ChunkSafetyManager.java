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
        for (BlockPos offset : relativeOffsets) {
            BlockPos targetPos = origin.add(offset);
            loadedChunks.add(new ChunkPos(targetPos));
        }

        ticket = ForgeChunkManager.requestTicket(SpatialShift.instance, world, ForgeChunkManager.Type.NORMAL);
        if (ticket != null) {
            for (ChunkPos cp : loadedChunks) {
                ForgeChunkManager.forceChunk(ticket, cp);
            }
        }

        for (ChunkPos cp : loadedChunks) {
            Chunk chunk = world.getChunkProvider().loadChunk(cp.x, cp.z);
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
