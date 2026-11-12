package dev.modforge.railpalette.server;

import dev.modforge.railpalette.data.RailNode;
import dev.modforge.railpalette.integration.CreateAccess;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

public final class RailTraversal {
    private static final TicketType<ChunkPos> TICKET = TicketType.create(
        "railpalette", Comparator.comparingLong(ChunkPos::toLong), 60);

    public static final class Deferred extends RuntimeException {
        private Deferred() {
            super(null, null, false, false);
        }
    }

    private static final Deferred DEFERRED = new Deferred();
    private final MinecraftServer server;
    private int chunkLoadsLeft;

    public RailTraversal(MinecraftServer server, int chunkLoads) {
        this.server = server;
        this.chunkLoadsLeft = chunkLoads;
    }

    public ServerLevel level(RailNode node) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, node.dimension()));
        if (level == null) {
            throw new IllegalStateException("Railway refers to unavailable dimension " + node.dimension());
        }
        return level;
    }

    public void ensureLoaded(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return;
        }
        int x = pos.getX() >> 4;
        int z = pos.getZ() >> 4;
        if (!level.hasChunk(x, z)) {
            if (chunkLoadsLeft == 0) {
                throw DEFERRED;
            }
            chunkLoadsLeft--;
            ChunkPos chunk = new ChunkPos(x, z);
            level.getChunkSource().addRegionTicket(TICKET, chunk, 2, chunk);
            level.getChunk(x, z);
        }
    }

    public boolean isTrack(RailNode node) {
        ServerLevel level = level(node);
        BlockPos pos = node.pos();
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        ensureLoaded(level, pos);
        return CreateAccess.isTrack(level.getBlockState(pos).getBlock());
    }

    public Set<RailNode> neighbors(RailNode node) {
        ServerLevel level = level(node);
        BlockPos pos = node.pos();
        ensureLoaded(level, pos);
        BlockState state = level.getBlockState(pos);
        Set<CreateAccess.Endpoint> own = CreateAccess.endpoints(level, pos, state);
        Set<RailNode> candidates = new HashSet<>();
        Set<RailNode> result = new HashSet<>();

        for (CreateAccess.Endpoint endpoint : own) {
            BlockPos center = BlockPos.containing(endpoint.location());
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        candidates.add(new RailNode(endpoint.dimension(), center.offset(x, y, z).asLong()));
                    }
                }
            }
        }

        for (RailNode curve : CreateAccess.curveEnds(level, pos)) {
            if (!curve.equals(node) && isTrack(curve)) {
                result.add(curve);
            }
        }

        candidates.remove(node);
        for (RailNode candidate : candidates) {
            if (result.contains(candidate) || !isTrack(candidate)) {
                continue;
            }
            ServerLevel otherLevel = level(candidate);
            BlockPos otherPos = candidate.pos();
            Set<CreateAccess.Endpoint> other = CreateAccess.endpoints(
                otherLevel, otherPos, otherLevel.getBlockState(otherPos));
            for (CreateAccess.Endpoint endpoint : other) {
                if (own.contains(endpoint)) {
                    result.add(candidate);
                    break;
                }
            }
        }
        return result;
    }
}
