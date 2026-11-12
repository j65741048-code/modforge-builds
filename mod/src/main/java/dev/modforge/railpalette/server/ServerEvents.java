package dev.modforge.railpalette.server;

import com.mojang.brigadier.Command;
import dev.modforge.railpalette.PaletteConfig;
import dev.modforge.railpalette.RailPalette;
import dev.modforge.railpalette.data.PaintJob;
import dev.modforge.railpalette.data.PaletteData;
import dev.modforge.railpalette.integration.CreateAccess;
import dev.modforge.railpalette.network.BogeyPacket;
import dev.modforge.railpalette.network.PaletteNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ServerEvents {
    @SubscribeEvent
    public void useBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof DyeItem dyeItem)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockEntity be = event.getLevel().getBlockEntity(pos);
        boolean bogey = CreateAccess.isBogey(be);
        boolean track = CreateAccess.isTrack(event.getLevel().getBlockState(pos).getBlock());
        if (!bogey && !track) {
            return;
        }
        if (!event.getEntity().mayBuild()) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!level.mayInteract(player, pos)) {
            return;
        }

        int dye = dyeItem.getDyeColor().getId();
        Component name = Component.translatable("item.minecraft." + dyeItem.getDyeColor().getName() + "_dye");

        if (bogey) {
            int previous = be.getPersistentData().contains(RailPalette.DYE_TAG)
                ? be.getPersistentData().getInt(RailPalette.DYE_TAG) : -1;
            if (previous == dye) {
                player.displayClientMessage(Component.translatable("railpalette.already"), true);
                return;
            }
            be.getPersistentData().putInt(RailPalette.DYE_TAG, dye);
            CreateAccess.notifyBogey(be);
            PacketDistributor.sendToPlayersTrackingChunk(level, new net.minecraft.world.level.ChunkPos(pos),
                BogeyPacket.block(level.dimension().location(), pos, dye));
            player.displayClientMessage(Component.translatable("railpalette.painted_bogey", name), true);
        } else if (player.isShiftKeyDown()) {
            if (!PaintService.queue(player, pos, dye)) {
                return;
            }
            player.displayClientMessage(Component.translatable("railpalette.queued", name), true);
        } else {
            if (!PaintService.paint(level, pos, dye)) {
                player.displayClientMessage(Component.translatable("railpalette.already"), true);
                return;
            }
            player.displayClientMessage(Component.translatable("railpalette.painted_track", name), true);
        }
        consume(player, stack, event.getHand(), pos);
    }

    @SubscribeEvent
    public void useEntity(PlayerInteractEvent.EntityInteractSpecific event) {
        ItemStack stack = event.getItemStack();
        Entity entity = event.getTarget();
        if (!(stack.getItem() instanceof DyeItem dyeItem) || !CreateAccess.isCarriage(entity)
            || !event.getEntity().mayBuild()) {
            return;
        }
        int index = CreateAccess.findBogey(entity, entity.position().add(event.getLocalPos()));
        if (index < 0) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag data = CreateAccess.carriageData(entity, index);
        if (data == null || !player.serverLevel().mayInteract(player, entity.blockPosition())) {
            return;
        }
        int dye = dyeItem.getDyeColor().getId();
        if (data.contains(RailPalette.DYE_TAG) && data.getInt(RailPalette.DYE_TAG) == dye) {
            player.displayClientMessage(Component.translatable("railpalette.already"), true);
            return;
        }
        data.putInt(RailPalette.DYE_TAG, dye);
        CreateAccess.dirtyRailwayData();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,
            BogeyPacket.entity(entity.level().dimension().location(), entity.getId(), index, dye));
        Component color = Component.translatable("item.minecraft." + DyeColor.byId(dye).getName() + "_dye");
        player.displayClientMessage(Component.translatable("railpalette.painted_bogey", color), true);
        consume(player, stack, event.getHand(), entity.blockPosition());
    }

    private static void consume(ServerPlayer player, ItemStack stack, InteractionHand hand, BlockPos pos) {
        if (PaletteConfig.CONSUME_DYE.get() && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.swing(hand, true);
        player.serverLevel().playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.25f);
    }

    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        PaintService.tick(event.getServer());
    }

    @SubscribeEvent
    public void watch(ChunkWatchEvent.Watch event) {
        PaletteNetwork.sendChunk(event.getPlayer(), event.getLevel(), event.getPos());
        var chunk = event.getLevel().getChunk(event.getPos().x, event.getPos().z);
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (CreateAccess.isBogey(be)) {
                int dye = be.getPersistentData().contains(RailPalette.DYE_TAG)
                    ? be.getPersistentData().getInt(RailPalette.DYE_TAG) : -1;
                PacketDistributor.sendToPlayer(event.getPlayer(),
                    BogeyPacket.block(event.getLevel().dimension().location(), be.getBlockPos(), dye));
            }
        }
    }

    @SubscribeEvent
    public void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !CreateAccess.isCarriage(event.getTarget())) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            CompoundTag data = CreateAccess.carriageData(event.getTarget(), i);
            if (data != null && data.contains(RailPalette.DYE_TAG)) {
                PacketDistributor.sendToPlayer(player, BogeyPacket.entity(
                    event.getTarget().level().dimension().location(), event.getTarget().getId(),
                    i, data.getInt(RailPalette.DYE_TAG)));
            }
        }
    }

    @SubscribeEvent
    public void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            clear(level, event.getPos());
        }
    }

    @SubscribeEvent
    public void placeBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PaintService.paint(level, event.getPos(), -1);
            BlockEntity be = level.getBlockEntity(event.getPos());
            if (CreateAccess.isBogey(be)) {
                int dye = be.getPersistentData().contains(RailPalette.DYE_TAG)
                    ? be.getPersistentData().getInt(RailPalette.DYE_TAG) : -1;
                PacketDistributor.sendToPlayersTrackingChunk(level,
                    new net.minecraft.world.level.ChunkPos(event.getPos()),
                    BogeyPacket.block(level.dimension().location(), event.getPos(), dye));
            }
        }
    }

    @SubscribeEvent
    public void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (BlockPos pos : event.getAffectedBlocks()) {
                clear(level, pos);
            }
        }
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        PaintService.paint(level, pos, -1);
        if (CreateAccess.isBogey(level.getBlockEntity(pos))) {
            PacketDistributor.sendToPlayersTrackingChunk(level, new net.minecraft.world.level.ChunkPos(pos),
                BogeyPacket.block(level.dimension().location(), pos, -1));
        }
    }

    @SubscribeEvent
    public void stopped(ServerStoppedEvent event) {
        PaletteNetwork.clear();
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("railpalette")
            .then(Commands.literal("status").executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                PaletteData data = PaletteData.get(player.server);
                long count = 0;
                long painted = 0;
                long pending = 0;
                for (PaintJob job : data.jobs) {
                    if (job.owner.equals(player.getUUID())) {
                        count++;
                        painted += job.painted;
                        pending += job.frontier.size();
                    }
                }
                player.sendSystemMessage(Component.translatable("railpalette.status", count, painted, pending));
                return Command.SINGLE_SUCCESS;
            }))
            .then(Commands.literal("cancel").executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                PaletteData data = PaletteData.get(player.server);
                int before = data.jobs.size();
                data.jobs.removeIf(job -> job.owner.equals(player.getUUID()));
                int removed = before - data.jobs.size();
                data.setDirty();
                player.sendSystemMessage(Component.translatable("railpalette.cancelled", removed));
                return removed;
            }))
            .then(Commands.literal("jobs").requires(source -> source.hasPermission(2)).executes(context -> {
                int count = PaletteData.get(context.getSource().getServer()).jobs.size();
                context.getSource().sendSuccess(() -> Component.translatable("railpalette.jobs", count), false);
                return count;
            })));
    }
}
