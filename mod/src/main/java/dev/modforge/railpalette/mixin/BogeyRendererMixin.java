package dev.modforge.railpalette.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.trains.bogey.BogeyRenderer", remap = false)
public abstract class BogeyRendererMixin {}
