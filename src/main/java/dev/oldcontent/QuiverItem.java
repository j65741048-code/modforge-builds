package dev.oldcontent;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class QuiverItem extends Item {
    private static final String ARROWS_KEY = "StoredArrows";
    private static final int CAPACITY = 64;

    public QuiverItem(Settings settings) {
        super(settings);
    }

    private static int stored(ItemStack stack) {
        return stack.hasNbt() ? Math.max(0, Math.min(CAPACITY, stack.getNbt().getInt(ARROWS_KEY))) : 0;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack quiver = player.getStackInHand(hand);
        if (!world.isClient) {
            int count = stored(quiver);
            if (player.isSneaking()) {
                quiver.getOrCreateNbt().putInt(ARROWS_KEY, 0);
                if (count > 0) {
                    player.getInventory().offerOrDrop(new ItemStack(Items.ARROW, count));
                }
                player.sendMessage(Text.translatable("message.old_content.withdraw", count), true);
            } else {
                int added = 0;
                for (int slot = 0; slot < player.getInventory().size() && count < CAPACITY; slot++) {
                    ItemStack arrows = player.getInventory().getStack(slot);
                    // Preserve renamed/custom arrows rather than discarding their data.
                    if (arrows.isOf(Items.ARROW) && !arrows.hasNbt()) {
                        int amount = Math.min(CAPACITY - count, arrows.getCount());
                        arrows.decrement(amount);
                        count += amount;
                        added += amount;
                    }
                }
                quiver.getOrCreateNbt().putInt(ARROWS_KEY, count);
                player.sendMessage(Text.translatable("message.old_content.store", added, count), true);
            }
            player.getInventory().markDirty();
        }
        return TypedActionResult.success(quiver, world.isClient);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return stored(stack) > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0F * stored(stack) / CAPACITY);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0xB98A52;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.old_content.quiver.count", stored(stack)).formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.old_content.quiver.store").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.old_content.quiver.withdraw").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.old_content.quiver.bow").formatted(Formatting.DARK_GRAY));
    }
}
