package dev.modforge.railpalette.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class TintConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int red;
    private final int green;
    private final int blue;

    public TintConsumer(VertexConsumer delegate, int rgb) {
        this.delegate = delegate;
        this.red = (rgb >> 16) & 255;
        this.green = (rgb >> 8) & 255;
        this.blue = rgb & 255;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(r * red / 255, g * green / 255, b * blue / 255, a);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }
}
