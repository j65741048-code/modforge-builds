package dev.modforge.railpalette.client;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public final class PaletteCache {
    public static final Map<ResourceLocation, Map<Long, Integer>> TRACKS = new ConcurrentHashMap<>();
    public static final Map<ResourceLocation, Map<Long, Integer>> BOGEYS = new ConcurrentHashMap<>();

    private PaletteCache() {}

    public static int track(ResourceLocation dimension, BlockPos pos) {
        Map<Long, Integer> map = TRACKS.get(dimension);
        return map == null ? -1 : map.getOrDefault(pos.asLong(), -1);
    }

    public static int bogey(ResourceLocation dimension, BlockPos pos) {
        Map<Long, Integer> map = BOGEYS.get(dimension);
        return map == null ? -2 : map.getOrDefault(pos.asLong(), -2);
    }

    public static int rgb(int dye) {
        return dye < 0 ? 0xFFFFFF : DyeColor.byId(dye).getTextureDiffuseColor();
    }

    public static void clear() {
        TRACKS.clear();
        BOGEYS.clear();
    }
}
