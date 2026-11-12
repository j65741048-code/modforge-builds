package dev.modforge.railpalette.server;

import dev.modforge.railpalette.PaletteConfig;
import dev.modforge.railpalette.RailPalette;
import dev.modforge.railpalette.data.PaintJob;
import dev.modforge.railpalette.data.PaletteData;
import dev.modforge.railpalette.data.RailNode;
import dev.modforge.railpalette.network.PaletteNetwork;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class PaintService {
    private PaintService() {}

    public static boolean queue(ServerPlayer player, BlockPos pos, int dye) {
        PaletteData data = PaletteData.get(player.server);
        long count = data.jobs.stream().filter(job -> job.owner.equals(player.getUUID())).count();
        if (count >= PaletteConfig.JOBS_PER_PLAYER.get()) {
            player.displayClientMessage(Component.translatable("railpalette.queue_full"), true);
            return false;
        }
        data.jobs.addLast(new PaintJob(player.getUUID(), dye,
            new RailNode(player.serverLevel().dimension().location(), pos.asLong())));
        data.setDirty();
        return true;
    }

    public static boolean paint(ServerLevel level, BlockPos pos, int dye) {
        PaletteData data = PaletteData.get(level.getServer());
        if (!data.setColor(level.dimension().location(), pos, dye)) {
            return false;
        }
        PaletteNetwork.queueTrack(level, pos, dye);
        return true;
    }

    public static void tick(MinecraftServer server) {
        PaletteData data = PaletteData.get(server);
        RailTraversal traversal = new RailTraversal(server, PaletteConfig.CHUNK_LOADS_PER_TICK.get());
        int visits = PaletteConfig.TRACKS_PER_TICK.get();

        while (visits-- > 0 && !data.jobs.isEmpty()) {
            PaintJob job = data.jobs.peekFirst();
            if (job.frontier.isEmpty()) {
                finish(server, data, job);
                continue;
            }
            RailNode node = job.frontier.peekFirst();
            try {
                if (!traversal.isTrack(node)) {
                    job.frontier.removeFirst();
                    data.setDirty();
                    continue;
                }
                Set<RailNode> neighbors = traversal.neighbors(node);
                job.frontier.removeFirst();
                neighbors.forEach(job::offer);
                paint(traversal.level(node), node.pos(), job.dye);
                job.painted++;
                data.setDirty();
                if (job.frontier.isEmpty()) {
                    finish(server, data, job);
                }
            } catch (RailTraversal.Deferred deferred) {
                break;
            } catch (RuntimeException exception) {
                RailPalette.LOGGER.error("Railway painting failed at {} in {}",
                    node.pos(), node.dimension(), exception);
                data.jobs.removeFirst();
                data.setDirty();
                ServerPlayer player = server.getPlayerList().getPlayer(job.owner);
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("railpalette.failed"));
                }
            }
        }
        PaletteNetwork.flush(server);
    }

    private static void finish(MinecraftServer server, PaletteData data, PaintJob job) {
        data.jobs.removeFirst();
        data.setDirty();
        ServerPlayer player = server.getPlayerList().getPlayer(job.owner);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("railpalette.complete", job.painted));
        }
    }
}
