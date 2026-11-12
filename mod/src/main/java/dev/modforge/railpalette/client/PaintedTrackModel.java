package dev.modforge.railpalette.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public final class PaintedTrackModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> DYE = new ModelProperty<>();
    private record QuadKey(BakedQuad quad, int dye) {}
    private final Map<QuadKey, BakedQuad> cache = new ConcurrentHashMap<>();

    public PaintedTrackModel(BakedModel original) {
        super(original);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        ModelData original = originalModel.getModelData(level, pos, state, data);
        var clientLevel = Minecraft.getInstance().level;
        int dye = clientLevel == null ? -1 : PaletteCache.track(clientLevel.dimension().location(), pos);
        return original.derive().with(DYE, dye).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource random, ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = originalModel.getQuads(state, side, random, data, renderType);
        Integer dye = data.get(DYE);
        if (dye == null || dye < 0) {
            return quads;
        }
        return quads.stream().map(quad -> cache.computeIfAbsent(new QuadKey(quad, dye),
            key -> tint(key.quad, PaletteCache.rgb(key.dye)))).toList();
    }

    private static BakedQuad tint(BakedQuad quad, int rgb) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        int red = (rgb >> 16) & 255;
        int green = (rgb >> 8) & 255;
        int blue = rgb & 255;
        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * stride + 3;
            int original = vertices[index];
            int r = (original & 255) * red / 255;
            int g = ((original >>> 8) & 255) * green / 255;
            int b = ((original >>> 16) & 255) * blue / 255;
            vertices[index] = (original & 0xFF000000) | (b << 16) | (g << 8) | r;
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }
}
