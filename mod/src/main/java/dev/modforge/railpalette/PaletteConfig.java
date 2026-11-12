package dev.modforge.railpalette;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PaletteConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue TRACKS_PER_TICK;
    public static final ModConfigSpec.IntValue CHUNK_LOADS_PER_TICK;
    public static final ModConfigSpec.IntValue JOBS_PER_PLAYER;
    public static final ModConfigSpec.BooleanValue CONSUME_DYE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        TRACKS_PER_TICK = b.comment("Maximum completed track visits per tick. No limit is imposed on total line length.")
            .translation("railpalette.configuration.tracksPerTick")
            .defineInRange("tracksPerTick", 128, 1, 8192);
        CHUNK_LOADS_PER_TICK = b.comment("Maximum synchronous chunk loads per tick while following a railway.")
            .translation("railpalette.configuration.chunkLoadsPerTick")
            .defineInRange("chunkLoadsPerTick", 4, 1, 64);
        JOBS_PER_PLAYER = b.translation("railpalette.configuration.jobsPerPlayer")
            .defineInRange("jobsPerPlayer", 4, 1, 32);
        CONSUME_DYE = b.comment("One dye paints a bogey, a single track, or an entire railway.")
            .translation("railpalette.configuration.consumeDye")
            .define("consumeDye", true);
        SPEC = b.build();
    }

    private PaletteConfig() {}
}
