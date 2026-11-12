package dev.modforge.railpalette.network;

import dev.modforge.railpalette.RailPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BogeyPacket(ResourceLocation dimension, long position, int entityId, int index, int dye)
    implements CustomPacketPayload {
    public static final Type<BogeyPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(RailPalette.ID, "bogey"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BogeyPacket> CODEC = StreamCodec.of(
        (buffer, packet) -> {
            buffer.writeResourceLocation(packet.dimension);
            buffer.writeLong(packet.position);
            buffer.writeInt(packet.entityId);
            buffer.writeByte(packet.index);
            buffer.writeByte(packet.dye);
        },
        buffer -> {
            ResourceLocation dimension = buffer.readResourceLocation();
            long position = buffer.readLong();
            int entity = buffer.readInt();
            int index = buffer.readByte();
            int dye = buffer.readByte();
            if (index < 0 || index > 1 || dye < -1 || dye > 15) {
                throw new IllegalArgumentException("Invalid bogey palette packet");
            }
            return new BogeyPacket(dimension, position, entity, index, dye);
        }
    );

    public static BogeyPacket block(ResourceLocation dimension, BlockPos pos, int dye) {
        return new BogeyPacket(dimension, pos.asLong(), -1, 0, dye);
    }

    public static BogeyPacket entity(ResourceLocation dimension, int entityId, int index, int dye) {
        return new BogeyPacket(dimension, 0, entityId, index, dye);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
