package com.example.healingruby;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class HealingRubyMod implements ModInitializer {
    public static final String MOD_ID = "healing_ruby";
    public static final Item RUBY = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "ruby"),
            new RubyItem(new Item.Settings().maxCount(64))
    );

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK)
                .register(entries -> entries.add(RUBY));
    }
}
