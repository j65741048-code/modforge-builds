package dev.modforge.railpalette.client;

import dev.modforge.railpalette.RailPalette;
import dev.modforge.railpalette.integration.CreateAccess;
import dev.modforge.railpalette.network.BogeyPacket;
import dev.modforge.railpalette.network.TrackPacket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ClientPackets {
    private record Pending(BogeyPacket packet, long expires) {}
    private static final List<Pending> PENDING = new ArrayList<>();
    private static long ticks;

    private ClientPackets() {}

    public static void tracks(TrackPacket packet) {
        var colors = PaletteCache.TRACKS.computeIfAbsent(packet.dimension(), ignored -> new ConcurrentHashMap<>());
        if (packet.replace()) {
            Iterator<Map.Entry<Long, Integer>> iterator = colors.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Integer> entry = iterator.next();
                BlockPos pos = BlockPos.of(entry.getKey());
                if (new ChunkPos(pos).toLong() == packet.chunk()) {
                    iterator.remove();
                    dirty(packet.dimension(), pos);
                }
            }
        }
        for (int i = 0; i < packet.positions().length; i++) {
            long position = packet.positions()[i];
            if (packet.dyes()[i] < 0) {
                colors.remove(position);
            } else {
                colors.put(position, (int) packet.dyes()[i]);
            }
            dirty(packet.dimension(), BlockPos.of(position));
        }
        TrackHooks.refreshAll();
    }

    public static void bogey(BogeyPacket packet) {
        if (packet.entityId() < 0) {
            PaletteCache.BOGEYS.computeIfAbsent(packet.dimension(), ignored -> new ConcurrentHashMap<>())
                .put(packet.position(), packet.dye());
            apply(packet);
        } else {
            PENDING.removeIf(old -> old.packet.dimension().equals(packet.dimension())
                && old.packet.entityId() == packet.entityId() && old.packet.index() == packet.index());
            PENDING.add(new Pending(packet, ticks + 200));
            apply(packet);
        }
    }

    private static boolean apply(BogeyPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(packet.dimension())) {
            return false;
        }
        CompoundTag data;
        if (packet.entityId() < 0) {
            BlockEntity be = minecraft.level.getBlockEntity(BlockPos.of(packet.position()));
            if (!CreateAccess.isBogey(be)) {
                return false;
            }
            data = be.getPersistentData();
        } else {
            Entity entity = minecraft.level.getEntity(packet.entityId());
            if (entity == null) {
                return false;
            }
            data = CreateAccess.carriageData(entity, packet.index());
            if (data == null) {
                return false;
            }
        }
        if (packet.dye() < 0) {
            data.remove(RailPalette.DYE_TAG);
        } else {
            data.putInt(RailPalette.DYE_TAG, packet.dye());
        }
        return true;
    }

    public static void tick() {
        ticks++;
        PENDING.removeIf(pending -> ticks > pending.expires);
        for (Pending pending : PENDING) {
            apply(pending.packet);
        }
    }

    private static void dirty(net.minecraft.resources.ResourceLocation dimension, BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(dimension)) {
            return;
        }
        BlockEntity be = minecraft.level.getBlockEntity(pos);
        if (be != null) {
            be.requestModelDataUpdate();
        }
        minecraft.levelRenderer.setBlocksDirty(
            pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    public static void clear() {
        PENDING.clear();
        ticks = 0;
        PaletteCache.clear();
        TrackHooks.clear();
    }
}
