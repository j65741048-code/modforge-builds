package dev.modforge.railpalette.client;

import dev.modforge.railpalette.RailPalette;
import dev.modforge.railpalette.integration.CreateAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = RailPalette.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientModels {
    private ClientModels() {}

    @SubscribeEvent
    public static void models(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) -> {
            if (!location.getVariant().equals("inventory")
                && CreateAccess.isTrack(BuiltInRegistries.BLOCK.get(location.id()))) {
                return new PaintedTrackModel(model);
            }
            return model;
        });
    }
}
