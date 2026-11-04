package com.example.healingruby;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class RubyItem extends Item {
    public RubyItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isAlive() || user.getHealth() >= user.getMaxHealth()) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            user.heal(8.0F);
            if (!user.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            user.getItemCooldownManager().set(this, 20);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS, 0.7F, 1.2F);
        }

        return TypedActionResult.success(stack, world.isClient);
    }
}
