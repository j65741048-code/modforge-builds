package dev.modforge.railpalette.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record RailNode(ResourceLocation dimension, long position) {
    public BlockPos pos() {
        return BlockPos.of(position);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Position", position);
        return tag;
    }

    public static RailNode load(CompoundTag tag) {
        return new RailNode(ResourceLocation.parse(tag.getString("Dimension")), tag.getLong("Position"));
    }
}
