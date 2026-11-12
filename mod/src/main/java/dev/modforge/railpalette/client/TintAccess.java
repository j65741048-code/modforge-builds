package dev.modforge.railpalette.client;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TintAccess {
    private record Access(Method color, Method changed, List<Field> children) {}
    private static final Map<Class<?>, Access> ACCESS = new ConcurrentHashMap<>();

    private TintAccess() {}

    public static void tint(Object object, int rgb) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        visit(object, rgb, seen, 0);
    }

    private static void visit(Object object, int rgb, Set<Object> seen, int depth) {
        if (object == null || depth > 12 || !seen.add(object)) {
            return;
        }
        Class<?> type = object.getClass();
        if (type.isArray()) {
            if (!type.componentType().isPrimitive()) {
                for (int i = 0; i < Array.getLength(object); i++) {
                    visit(Array.get(object, i), rgb, seen, depth + 1);
                }
            }
            return;
        }
        if (object instanceof Map<?, ?> map) {
            map.values().forEach(value -> visit(value, rgb, seen, depth + 1));
            return;
        }
        if (object instanceof Iterable<?> iterable) {
            for (Object child : iterable) {
                visit(child, rgb, seen, depth + 1);
            }
            return;
        }
        String name = type.getName();
        if (!name.startsWith("com.simibubi.create.") && !name.startsWith("net.createmod.")
            && !name.startsWith("dev.engine_room.flywheel.")) {
            return;
        }
        Access access = ACCESS.computeIfAbsent(type, TintAccess::inspect);
        try {
            if (access.color != null) {
                access.color.invoke(object, rgb);
                if (access.changed != null) {
                    access.changed.invoke(object);
                }
                return;
            }
            for (Field field : access.children) {
                visit(field.get(object), rgb, seen, depth + 1);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to tint Create model data " + name, exception);
        }
    }

    private static Access inspect(Class<?> type) {
        Method color = method(type, "color", int.class);
        if (color == null) {
            color = method(type, "setColor", int.class);
        }
        Method changed = method(type, "setChanged");
        List<Field> children = new ArrayList<>();
        if (color == null) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (!current.getName().startsWith("com.simibubi.create.")
                    && !current.getName().startsWith("net.createmod.")
                    && !current.getName().startsWith("dev.engine_room.flywheel.")) {
                    break;
                }
                for (Field field : current.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                        String fieldType = field.getType().getName();
                        if (fieldType.startsWith("net.minecraft.") || fieldType.startsWith("org.joml.")
                            || fieldType.contains("VisualizationContext")
                            || fieldType.contains("InstanceHandle")
                            || fieldType.contains("Instancer")) {
                            continue;
                        }
                        field.setAccessible(true);
                        children.add(field);
                    }
                }
            }
        }
        return new Access(color, changed, List.copyOf(children));
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }
}
