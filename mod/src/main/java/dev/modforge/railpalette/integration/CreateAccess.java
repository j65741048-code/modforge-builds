package dev.modforge.railpalette.integration;

import dev.modforge.railpalette.RailPalette;
import dev.modforge.railpalette.data.RailNode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public final class CreateAccess {
    private static final String TRACK = "com.simibubi.create.content.trains.track.ITrackBlock";
    private static final String BOGEY = "com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity";
    private static final ConcurrentHashMap<Class<?>, Method> CONNECTED = new ConcurrentHashMap<>();

    private CreateAccess() {}

    public static boolean isTrack(Block block) {
        return hasType(block.getClass(), TRACK);
    }

    public static boolean isBogey(BlockEntity be) {
        return be != null && hasType(be.getClass(), BOGEY);
    }

    public static boolean isCarriage(Entity entity) {
        return hasType(entity.getClass(),
            "com.simibubi.create.content.trains.entity.CarriageContraptionEntity");
    }

    public static boolean hasType(Class<?> type, String name) {
        if (type == null) {
            return false;
        }
        if (type.getName().equals(name)) {
            return true;
        }
        for (Class<?> iface : type.getInterfaces()) {
            if (hasType(iface, name)) {
                return true;
            }
        }
        return hasType(type.getSuperclass(), name);
    }

    public record Endpoint(ResourceLocation dimension, long x, long y, long z) {
        public Vec3 location() {
            return new Vec3(x / 4096d, y / 4096d, z / 4096d);
        }
    }

    public static Set<Endpoint> endpoints(ServerLevel level, BlockPos pos, BlockState state) {
        try {
            Method method = CONNECTED.computeIfAbsent(state.getBlock().getClass(), type -> {
                for (Method candidate : type.getMethods()) {
                    if (candidate.getName().equals("getConnected") && candidate.getParameterCount() == 5) {
                        return candidate;
                    }
                }
                throw new IllegalStateException("Create ITrackBlock.getConnected was not found: " + type.getName());
            });
            Object result = method.invoke(state.getBlock(), level, pos, state, false, null);
            if (!(result instanceof Collection<?> locations)) {
                throw new IllegalStateException("Create returned a non-collection track connection set");
            }
            Set<Endpoint> endpoints = new HashSet<>();
            for (Object location : locations) {
                Object value = call(location, "getLocation");
                if (!(value instanceof Vec3 vector)) {
                    throw new IllegalStateException("Create track location did not expose a Vec3");
                }
                Object dimensionValue = field(location, "dimension");
                if (dimensionValue == null) {
                    dimensionValue = callOptional(location, "getDimension");
                }
                ResourceLocation dimension = level.dimension().location();
                if (dimensionValue instanceof ResourceKey<?> key) {
                    dimension = key.location();
                } else if (dimensionValue instanceof ResourceLocation id) {
                    dimension = id;
                }
                endpoints.add(new Endpoint(dimension,
                    Math.round(vector.x * 4096), Math.round(vector.y * 4096), Math.round(vector.z * 4096)));
            }
            return endpoints;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect Create track at " + pos, exception);
        }
    }

    public static List<RailNode> curveEnds(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return List.of();
        }
        Object connections = callOptional(be, "getConnections");
        if (!(connections instanceof Map<?, ?> map)) {
            return List.of();
        }
        List<RailNode> result = new ArrayList<>();
        for (Object key : map.keySet()) {
            if (key instanceof BlockPos endpoint) {
                result.add(new RailNode(level.dimension().location(), endpoint.asLong()));
            }
        }
        return result;
    }

    public static void notifyBogey(BlockEntity be) {
        be.setChanged();
        callOptional(be, "sendData");
        if (be.getLevel() != null) {
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    public static Object carriageBogey(Entity entity, int index) {
        if (!isCarriage(entity)) {
            return null;
        }
        Object carriage = callOptional(entity, "getCarriage");
        if (carriage == null) {
            carriage = field(entity, "carriage");
        }
        if (carriage == null) {
            return null;
        }
        Object pair = field(carriage, "bogeys");
        if (pair != null) {
            return callOptional(pair, index == 0 ? "getFirst" : "getSecond");
        }
        return callOptional(carriage, index == 0 ? "leadingBogey" : "trailingBogey");
    }

    public static CompoundTag carriageData(Entity entity, int index) {
        Object bogey = carriageBogey(entity, index);
        if (bogey == null) {
            return null;
        }
        Object data = field(bogey, "bogeyData");
        if (!(data instanceof CompoundTag)) {
            data = callOptional(bogey, "getBogeyData");
        }
        return data instanceof CompoundTag tag ? tag : null;
    }

    public static int findBogey(Entity entity, Vec3 hit) {
        int bestIndex = -1;
        double bestDistance = 2.25 * 2.25;
        for (int index = 0; index < 2; index++) {
            Object bogey = carriageBogey(entity, index);
            if (bogey == null) {
                continue;
            }
            Object anchor = callOptional(bogey, "getAnchorPosition");
            if (!(anchor instanceof Vec3)) {
                anchor = field(bogey, "anchor");
            }
            if (anchor instanceof Vec3 position) {
                double distance = position.distanceToSqr(hit);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = index;
                }
            }
        }
        return bestIndex;
    }

    public static void dirtyRailwayData() {
        try {
            Class<?> create = Class.forName("com.simibubi.create.Create");
            Object railways = create.getField("RAILWAYS").get(null);
            for (Class<?> type = railways.getClass(); type != null; type = type.getSuperclass()) {
                for (Field f : type.getDeclaredFields()) {
                    if (SavedData.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        if (f.get(railways) instanceof SavedData data) {
                            data.setDirty();
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot mark Create railway data dirty", exception);
        }
    }

    public static Object call(Object object, String method) throws ReflectiveOperationException {
        return object.getClass().getMethod(method).invoke(object);
    }

    public static Object callOptional(Object object, String method) {
        if (object == null) {
            return null;
        }
        try {
            return call(object, method);
        } catch (NoSuchMethodException exception) {
            return null;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create integration call failed: " + method, exception);
        }
    }

    public static Object field(Object object, String name) {
        if (object == null) {
            return null;
        }
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
                // Search the superclass; Create keeps some carriage state in base classes.
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot read Create field " + name, exception);
            }
        }
        return null;
    }
}
