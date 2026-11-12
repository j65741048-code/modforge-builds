package dev.modforge.railpalette.mixin;

import dev.modforge.railpalette.RailPalette;
import dev.modforge.railpalette.client.PaletteCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity", remap = false)
public abstract class BogeyDataMixin {
    @Inject(method = "getBogeyData", at = @At("RETURN"), remap = false)
    private void railpalette$includeDye(CallbackInfoReturnable<CompoundTag> callback) {
        BlockEntity be = (BlockEntity) (Object) this;
        int dye = be.getPersistentData().contains(RailPalette.DYE_TAG)
            ? be.getPersistentData().getInt(RailPalette.DYE_TAG) : -1;
        if (be.getLevel() != null && be.getLevel().isClientSide) {
            int cached = PaletteCache.bogey(be.getLevel().dimension().location(), be.getBlockPos());
            if (cached >= -1) {
                dye = cached;
            }
        }
        CompoundTag result = callback.getReturnValue();
        if (dye >= 0) {
            result.putInt(RailPalette.DYE_TAG, dye);
        } else {
            result.remove(RailPalette.DYE_TAG);
        }
    }
}
