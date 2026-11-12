package dev.modforge.railpalette.client;

import dev.modforge.railpalette.RailPalette;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.ClientTickEvent;

@EventBusSubscriber(modid = RailPalette.ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ClientPackets.tick();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPackets.clear();
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof DyeItem) {
            event.getToolTip().add(Component.translatable("railpalette.tooltip.bogey")
                .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("railpalette.tooltip.track")
                .withStyle(ChatFormatting.GRAY));
        }
    }
}
