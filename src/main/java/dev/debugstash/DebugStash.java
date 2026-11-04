package dev.debugstash;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class DebugStash implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("Debug")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> giveStash(context.getSource())));
            dispatcher.register(CommandManager.literal("debug")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> giveStash(context.getSource())));
        });
    }

    private static int giveStash(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        List<ItemStack> stacks = new ArrayList<>();

        // Vanilla has no dedicated eggs for these entities. EntityTag supplies
        // the entity type while retaining a normal, client-compatible egg item.
        stacks.add(entityEgg(Items.ZOMBIE_SPAWN_EGG, "minecraft:giant", "Giant Spawn Egg"));
        stacks.add(entityEgg(Items.EVOKER_SPAWN_EGG, "minecraft:illusioner", "Illusioner Spawn Egg"));
        stacks.add(new ItemStack(Items.ZOMBIE_HORSE_SPAWN_EGG));

        ItemStack killerRabbit = entityEgg(Items.RABBIT_SPAWN_EGG, "minecraft:rabbit", "Killer Bunny Spawn Egg");
        killerRabbit.getOrCreateSubNbt("EntityTag").putInt("RabbitType", 99);
        stacks.add(killerRabbit);

        stacks.add(new ItemStack(Items.DEBUG_STICK));
        stacks.add(new ItemStack(Items.COMMAND_BLOCK, 16));
        stacks.add(new ItemStack(Items.CHAIN_COMMAND_BLOCK, 16));
        stacks.add(new ItemStack(Items.REPEATING_COMMAND_BLOCK, 16));
        stacks.add(new ItemStack(Items.COMMAND_BLOCK_MINECART));
        stacks.add(new ItemStack(Items.BARRIER, 64));
        stacks.add(new ItemStack(Items.LIGHT, 64));
        stacks.add(new ItemStack(Items.STRUCTURE_VOID, 64));
        stacks.add(new ItemStack(Items.STRUCTURE_BLOCK));
        stacks.add(new ItemStack(Items.JIGSAW));
        stacks.add(new ItemStack(Items.SPAWNER));
        stacks.add(new ItemStack(Items.BEDROCK, 64));
        stacks.add(new ItemStack(Items.END_PORTAL_FRAME, 12));
        stacks.add(new ItemStack(Items.BUDDING_AMETHYST, 16));
        stacks.add(new ItemStack(Items.REINFORCED_DEEPSLATE, 16));

        int dropped = 0;
        for (ItemStack stack : stacks) {
            player.getInventory().insertStack(stack);
            if (!stack.isEmpty()) {
                player.dropItem(stack, false);
                dropped++;
            }
        }
        player.currentScreenHandler.sendContentUpdates();

        String message = "Debug stash granted: unused-mob eggs and restricted items/blocks.";
        if (dropped > 0) {
            message += " Inventory full: " + dropped + " remaining stack(s) dropped at your feet.";
        }
        message += " Creative mode is recommended; vanilla operator restrictions still apply.";
        final String feedback = message;
        source.sendFeedback(() -> Text.literal(feedback), true);
        return stacks.size();
    }

    private static ItemStack entityEgg(Item baseEgg, String entityId, String name) {
        ItemStack stack = new ItemStack(baseEgg);
        NbtCompound entityTag = stack.getOrCreateSubNbt("EntityTag");
        entityTag.putString("id", entityId);
        stack.setCustomName(Text.literal(name));
        return stack;
    }
}
