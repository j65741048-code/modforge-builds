package dev.modforge.railpalette.data;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public final class PaletteData extends SavedData {
    private static final Factory<PaletteData> FACTORY =
        new Factory<>(PaletteData::new, PaletteData::load, null);

    private final Map<ResourceLocation, Long2ObjectOpenHashMap<Long2IntOpenHashMap>> colors = new HashMap<>();
    public final ArrayDeque<PaintJob> jobs = new ArrayDeque<>();

    public static PaletteData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "railpalette");
    }

    public Long2IntOpenHashMap chunk(ResourceLocation dimension, long chunk) {
        var dimensionColors = colors.get(dimension);
        return dimensionColors == null ? null : dimensionColors.get(chunk);
    }

    public int color(ResourceLocation dimension, BlockPos pos) {
        var map = chunk(dimension, new ChunkPos(pos).toLong());
        return map == null ? -1 : map.getOrDefault(pos.asLong(), -1);
    }

    public boolean setColor(ResourceLocation dimension, BlockPos pos, int dye) {
        int old = color(dimension, pos);
        if (old == dye) {
            return false;
        }
        long chunkKey = new ChunkPos(pos).toLong();
        if (dye < 0) {
            var dimensionColors = colors.get(dimension);
            if (dimensionColors != null) {
                var map = dimensionColors.get(chunkKey);
                if (map != null) {
                    map.remove(pos.asLong());
                    if (map.isEmpty()) {
                        dimensionColors.remove(chunkKey);
                    }
                }
                if (dimensionColors.isEmpty()) {
                    colors.remove(dimension);
                }
            }
        } else {
            colors.computeIfAbsent(dimension, ignored -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(chunkKey, ignored -> new Long2IntOpenHashMap())
                .put(pos.asLong(), dye);
        }
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag dimensions = new ListTag();
        colors.forEach((dimension, chunks) -> {
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("Id", dimension.toString());
            ListTag chunkTags = new ListTag();
            chunks.long2ObjectEntrySet().forEach(chunk -> {
                CompoundTag chunkTag = new CompoundTag();
                chunkTag.putLong("Chunk", chunk.getLongKey());
                long[] positions = new long[chunk.getValue().size()];
                byte[] dyes = new byte[positions.length];
                int index = 0;
                for (Long2IntMap.Entry entry : chunk.getValue().long2IntEntrySet()) {
                    positions[index] = entry.getLongKey();
                    dyes[index++] = (byte) entry.getIntValue();
                }
                chunkTag.putLongArray("Positions", positions);
                chunkTag.putByteArray("Dyes", dyes);
                chunkTags.add(chunkTag);
            });
            dimensionTag.put("Chunks", chunkTags);
            dimensions.add(dimensionTag);
        });
        root.put("Dimensions", dimensions);
        ListTag jobTags = new ListTag();
        jobs.forEach(job -> jobTags.add(job.save()));
        root.put("Jobs", jobTags);
        return root;
    }

    private static PaletteData load(CompoundTag root, HolderLookup.Provider registries) {
        PaletteData data = new PaletteData();
        ListTag dimensions = root.getList("Dimensions", Tag.TAG_COMPOUND);
        for (int d = 0; d < dimensions.size(); d++) {
            CompoundTag dimensionTag = dimensions.getCompound(d);
            ResourceLocation dimension = ResourceLocation.parse(dimensionTag.getString("Id"));
            var chunks = new Long2ObjectOpenHashMap<Long2IntOpenHashMap>();
            ListTag chunkTags = dimensionTag.getList("Chunks", Tag.TAG_COMPOUND);
            for (int c = 0; c < chunkTags.size(); c++) {
                CompoundTag chunkTag = chunkTags.getCompound(c);
                long[] positions = chunkTag.getLongArray("Positions");
                byte[] dyes = chunkTag.getByteArray("Dyes");
                Long2IntOpenHashMap map = new Long2IntOpenHashMap();
                for (int i = 0; i < Math.min(positions.length, dyes.length); i++) {
                    if (dyes[i] >= 0 && dyes[i] <= 15) {
                        map.put(positions[i], dyes[i]);
                    }
                }
                if (!map.isEmpty()) {
                    chunks.put(chunkTag.getLong("Chunk"), map);
                }
            }
            data.colors.put(dimension, chunks);
        }
        ListTag jobs = root.getList("Jobs", Tag.TAG_COMPOUND);
        for (int i = 0; i < jobs.size(); i++) {
            data.jobs.addLast(PaintJob.load(jobs.getCompound(i)));
        }
        return data;
    }
}
