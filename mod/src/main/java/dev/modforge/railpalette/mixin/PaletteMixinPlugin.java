package dev.modforge.railpalette.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Inserts type-discovered renderer hooks. Create's renderer overload descriptors
 * differ between 6.0 patch releases; semantic parameter types remain stable.
 * No Create classes are loaded while mixin configuration is being initialized.
 */
public final class PaletteMixinPlugin implements IMixinConfigPlugin {
    private static final String BOGEY_HOOKS = "dev/modforge/railpalette/client/BogeyHooks";
    private static final String TRACK_HOOKS = "dev/modforge/railpalette/client/TrackHooks";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.endsWith(".BogeyRendererMixin")) {
            transformModels(targetClass);
        } else if (mixinClassName.endsWith(".SmallBogeyRendererMixin")
            || mixinClassName.endsWith(".LargeBogeyRendererMixin")) {
            transformBogeyRender(targetClass);
        } else if (mixinClassName.endsWith(".TrackRendererMixin")) {
            transformTrackRenderer(targetClass);
        } else if (mixinClassName.endsWith(".TrackVisualMixin")) {
            transformTrackVisual(targetClass);
        }
    }

    private static int argument(MethodNode method, java.util.function.Predicate<Type> predicate) {
        int local = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        for (Type type : Type.getArgumentTypes(method.desc)) {
            if (predicate.test(type)) {
                return local;
            }
            local += type.getSize();
        }
        return -1;
    }

    private static void transformModels(ClassNode target) {
        int count = 0;
        for (MethodNode method : target.methods) {
            if (!method.name.equals("getTransform") || (method.access & Opcodes.ACC_ABSTRACT) != 0) {
                continue;
            }
            int model = argument(method, type -> type.getDescriptor().endsWith("/PartialModel;"));
            if (model < 0) {
                continue;
            }
            Type result = Type.getReturnType(method.desc);
            if (result.getSort() != Type.OBJECT && result.getSort() != Type.ARRAY) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() == Opcodes.ARETURN) {
                    InsnList hook = new InsnList();
                    hook.add(new InsnNode(Opcodes.DUP));
                    hook.add(new VarInsnNode(Opcodes.ALOAD, model));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BOGEY_HOOKS, "tintTransform",
                        "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
                    method.instructions.insertBefore(instruction, hook);
                    count++;
                }
            }
            method.maxStack += 2;
        }
        require(count, target, "bogey wheel model transforms");
    }

    private static void transformBogeyRender(ClassNode target) {
        int count = 0;
        for (MethodNode method : target.methods) {
            if (!method.name.equals("render") || (method.access & Opcodes.ACC_ABSTRACT) != 0) {
                continue;
            }
            int data = argument(method,
                type -> type.getDescriptor().equals("Lnet/minecraft/nbt/CompoundTag;"));
            if (data < 0) {
                continue;
            }
            InsnList entry = new InsnList();
            entry.add(new VarInsnNode(Opcodes.ALOAD, data));
            entry.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BOGEY_HOOKS, "enter",
                "(Lnet/minecraft/nbt/CompoundTag;)V", false));
            method.instructions.insert(entry);
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                int opcode = instruction.getOpcode();
                if (opcode == Opcodes.RETURN || opcode == Opcodes.ATHROW) {
                    method.instructions.insertBefore(instruction,
                        new MethodInsnNode(Opcodes.INVOKESTATIC, BOGEY_HOOKS, "exit", "()V", false));
                }
            }
            method.maxStack += 1;
            count++;
        }
        require(count, target, "bogey dye rendering context");
    }

    private static void transformTrackRenderer(ClassNode target) {
        int count = 0;
        for (MethodNode method : target.methods) {
            if (!method.name.equals("renderSafe")) {
                continue;
            }
            int be = argument(method, type -> type.getDescriptor().endsWith("/TrackBlockEntity;"));
            int buffers = argument(method,
                type -> type.getDescriptor().equals("Lnet/minecraft/client/renderer/MultiBufferSource;"));
            if (be < 0 || buffers < 0) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, be));
            hook.add(new VarInsnNode(Opcodes.ALOAD, buffers));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TRACK_HOOKS, "buffers",
                "(Ljava/lang/Object;Lnet/minecraft/client/renderer/MultiBufferSource;)"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;", false));
            hook.add(new VarInsnNode(Opcodes.ASTORE, buffers));
            method.instructions.insert(hook);
            method.maxStack += 2;
            count++;
        }
        require(count, target, "buffered curve rendering");
    }

    private static void transformTrackVisual(ClassNode target) {
        int constructors = 0;
        for (MethodNode method : target.methods) {
            boolean constructor = method.name.equals("<init>");
            boolean update = method.name.equals("update") || method.name.equals("beginFrame")
                || method.name.equals("updateLight");
            if ((!constructor && !update) || (method.access & Opcodes.ACC_STATIC) != 0) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() == Opcodes.RETURN) {
                    InsnList hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TRACK_HOOKS, "refresh",
                        "(Ljava/lang/Object;)V", false));
                    method.instructions.insertBefore(instruction, hook);
                    if (constructor) {
                        constructors++;
                    }
                }
            }
            method.maxStack += 1;
        }
        require(constructors, target, "instanced curve rendering");
    }

    private static void require(int count, ClassNode target, String feature) {
        if (count == 0) {
            throw new IllegalStateException("Rail Palette could not install " + feature + " in "
                + target.name + ". Install a supported Create 6.0.x build for Minecraft 1.21.1.");
        }
    }
}
