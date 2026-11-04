package dev.oldcontent;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class OldContent implements ModInitializer {
    public static final String MOD_ID = "old_content";
    public static final Item RUBY = new Item(new Item.Settings());
    public static final Item QUIVER = new QuiverItem(new Item.Settings().maxCount(1));
    public static final Block GEAR = new GearBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.IRON_GRAY).strength(1.5F).sounds(BlockSoundGroup.METAL).nonOpaque());
    public static final Item GEAR_ITEM = new BlockItem(GEAR, new Item.Settings());

    private static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("ruby"), RUBY);
        Registry.register(Registries.ITEM, id("quiver"), QUIVER);
        Registry.register(Registries.BLOCK, id("gear"), GEAR);
        Registry.register(Registries.ITEM, id("gear"), GEAR_ITEM);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(RUBY));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(QUIVER));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(GEAR_ITEM));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("old").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    player.getInventory().offerOrDrop(new ItemStack(RUBY, 16));
                    player.getInventory().offerOrDrop(new ItemStack(QUIVER));
                    player.getInventory().offerOrDrop(new ItemStack(GEAR_ITEM, 16));
                    player.getInventory().offerOrDrop(new ItemStack(Items.ARROW, 64));
                    context.getSource().sendFeedback(() -> Text.translatable("message.old_content.kit"), false);
                    return 1;
                })));
    }
}
