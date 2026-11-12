package dev.modforge.railpalette.network;

import dev.modforge.railpalette.client.ClientPackets;
import dev.modforge.railpalette.data.PaletteData;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PaletteNetwork {
    private record ChunkKey(ResourceLocation dimension, long chunk) {}
    private static final Map<ChunkKey, Long2IntOpenHashMap> DIRTY = new HashMap<>();

    private PaletteNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(TrackPacket.TYPE, TrackPacket.CODEC,
            (packet, context) -> context.enqueueWork(() -> ClientPackets.tracks(packet)));
        registrar.playToClient(BogeyPacket.TYPE, BogeyPacket.CODEC,
            (packet, context) -> context.enqueueWork(() -> ClientPackets.bogey(packet)));
    }

    public static void queueTrack(ServerLevel level, BlockPos pos, int dye) {
        ChunkKey key = new ChunkKey(level.dimension().location(), new ChunkPos(pos).toLong());
        DIRTY.computeIfAbsent(key, ignored -> new Long2IntOpenHashMap()).put(pos.asLong(), dye);
    }

    public static void flush(MinecraftServer server) {
        DIRTY.forEach((key, map) -> {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, key.dimension));
            if (level != null) {
                sendBatches(key, map, false,
                    packet -> PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(key.chunk), packet));
            }
        });
        DIRTY.clear();
    }

    public static void sendChunk(ServerPlayer player, ServerLevel level, ChunkPos chunk) {
        ChunkKey key = new ChunkKey(level.dimension().location(), chunk.toLong());
        Long2IntOpenHashMap map = PaletteData.get(level.getServer()).chunk(key.dimension, key.chunk);
        sendBatches(key, map, true, packet -> PacketDistributor.sendToPlayer(player, packet));
    }

    private static void sendBatches(ChunkKey key, Long2IntOpenHashMap map, boolean replace,
                                    Consumer<TrackPacket> sender) {
        if (map == null || map.isEmpty()) {
            if (replace) {
                sender.accept(new TrackPacket(key.dimension, key.chunk, true, new long[0], new byte[0]));
            }
            return;
        }
        var iterator = map.long2IntEntrySet().iterator();
        boolean first = true;
        int remaining = map.size();
        while (remaining > 0) {
            int size = Math.min(remaining, TrackPacket.MAX_ENTRIES);
            long[] positions = new long[size];
            byte[] dyes = new byte[size];
            for (int i = 0; i < size; i++) {
                Long2IntMap.Entry entry = iterator.next();
                positions[i] = entry.getLongKey();
                dyes[i] = (byte) entry.getIntValue();
            }
            sender.accept(new TrackPacket(key.dimension, key.chunk, replace && first, positions, dyes));
            first = false;
            remaining -= size;
        }
    }

    public static void clear() {
        DIRTY.clear();
    }
}
