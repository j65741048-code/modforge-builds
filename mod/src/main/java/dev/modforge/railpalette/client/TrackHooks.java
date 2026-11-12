package dev.modforge.railpalette.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class TrackHooks {
    private static final Map<Object, Boolean> VISUALS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Class<?>, List<Field>> BE_FIELDS = new ConcurrentHashMap<>();

    private TrackHooks() {}

    public static MultiBufferSource buffers(Object object, MultiBufferSource original) {
        if (!(object instanceof BlockEntity be) || be.getLevel() == null) {
            return original;
        }
        int dye = PaletteCache.track(be.getLevel().dimension().location(), be.getBlockPos());
        if (dye < 0) {
            return original;
        }
        int rgb = PaletteCache.rgb(dye);
        return type -> new TintConsumer(original.getBuffer(type), rgb);
    }

    public static void refresh(Object visual) {
        VISUALS.put(visual, Boolean.TRUE);
        BlockEntity be = owner(visual);
        if (be != null && be.getLevel() != null) {
            int dye = PaletteCache.track(be.getLevel().dimension().location(), be.getBlockPos());
            TintAccess.tint(visual, PaletteCache.rgb(dye));
        }
    }

    public static void refreshAll() {
        List<Object> visuals;
        synchronized (VISUALS) {
            visuals = new ArrayList<>(VISUALS.keySet());
        }
        for (Object visual : visuals) {
            refresh(visual);
        }
    }

    private static BlockEntity owner(Object visual) {
        List<Field> fields = BE_FIELDS.computeIfAbsent(visual.getClass(), type -> {
            List<Field> result = new ArrayList<>();
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())
                        && BlockEntity.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        result.add(field);
                    }
                }
            }
            return List.copyOf(result);
        });
        for (Field field : fields) {
            try {
                if (field.get(visual) instanceof BlockEntity be) {
                    return be;
                }
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot resolve Create track visual owner", exception);
            }
        }
        return null;
    }

    public static void clear() {
        VISUALS.clear();
    }
}
