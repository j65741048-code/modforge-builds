package dev.modforge.railpalette;

import com.mojang.logging.LogUtils;
import dev.modforge.railpalette.network.PaletteNetwork;
import dev.modforge.railpalette.server.ServerEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(RailPalette.ID)
public final class RailPalette {
    public static final String ID = "railpalette";
    public static final String DYE_TAG = "railpalette_dye";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RailPalette(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, PaletteConfig.SPEC);
        modBus.addListener(PaletteNetwork::register);
        NeoForge.EVENT_BUS.register(new ServerEvents());
    }
}
