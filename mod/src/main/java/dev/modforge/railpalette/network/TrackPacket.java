package dev.modforge.railpalette.network;

import dev.modforge.railpalette.RailPalette;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TrackPacket(ResourceLocation dimension, long chunk, boolean replace,
                          long[] positions, byte[] dyes) implements CustomPacketPayload {
    public static final Type<TrackPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(RailPalette.ID, "tracks"));
    public static final int MAX_ENTRIES = 1024;
    public static final StreamCodec<RegistryFriendlyByteBuf, TrackPacket> CODEC = StreamCodec.of(
        (buffer, packet) -> {
            buffer.writeResourceLocation(packet.dimension);
            buffer.writeLong(packet.chunk);
            buffer.writeBoolean(packet.replace);
            buffer.writeVarInt(packet.positions.length);
            for (int i = 0; i < packet.positions.length; i++) {
                buffer.writeLong(packet.positions[i]);
                buffer.writeByte(packet.dyes[i]);
            }
        },
        buffer -> {
            ResourceLocation dimension = buffer.readResourceLocation();
            long chunk = buffer.readLong();
            boolean replace = buffer.readBoolean();
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_ENTRIES) {
                throw new IllegalArgumentException("Invalid palette packet size");
            }
            long[] positions = new long[count];
            byte[] dyes = new byte[count];
            for (int i = 0; i < count; i++) {
                positions[i] = buffer.readLong();
                dyes[i] = buffer.readByte();
                if (dyes[i] < -1 || dyes[i] > 15) {
                    throw new IllegalArgumentException("Invalid dye id");
                }
            }
            return new TrackPacket(dimension, chunk, replace, positions, dyes);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
