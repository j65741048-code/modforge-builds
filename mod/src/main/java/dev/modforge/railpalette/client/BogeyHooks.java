package dev.modforge.railpalette.client;

import dev.modforge.railpalette.RailPalette;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class BogeyHooks {
    private static final ThreadLocal<Integer> COLOR = ThreadLocal.withInitial(() -> -1);
    private static final Map<Object, Boolean> WHEELS = new ConcurrentHashMap<>();

    private BogeyHooks() {}

    public static void enter(CompoundTag data) {
        COLOR.set(data.contains(RailPalette.DYE_TAG) ? data.getInt(RailPalette.DYE_TAG) : -1);
    }

    public static void exit() {
        COLOR.set(-1);
    }

    public static void tintTransform(Object transform, Object partialModel) {
        boolean wheel = WHEELS.computeIfAbsent(partialModel, BogeyHooks::isWheel);
        TintAccess.tint(transform, PaletteCache.rgb(wheel ? COLOR.get() : -1));
    }

    private static boolean isWheel(Object partialModel) {
        for (Class<?> type = partialModel.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && field.getType() == ResourceLocation.class) {
                    try {
                        field.setAccessible(true);
                        ResourceLocation id = (ResourceLocation) field.get(partialModel);
                        if (id != null && id.getPath().toLowerCase(java.util.Locale.ROOT).contains("wheel")) {
                            return true;
                        }
                    } catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException("Cannot identify Create bogey partial model", exception);
                    }
                }
            }
        }
        return false;
    }
}
