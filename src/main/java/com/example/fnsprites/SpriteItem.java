package com.example.fnsprites;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/** Un espíritu equipable. Se "equipa" poniéndolo en la mano secundaria (la espalda en Fortnite). */
public class SpriteItem extends Item {
    public static final int MAX_LEVEL = 5;
    public final SpriteType type;
    public final Finish finish;

    public SpriteItem(Properties props, SpriteType type, Finish finish) {
        super(props);
        this.type = type;
        this.finish = finish;
    }

    // ---- datos guardados en el objeto ----
    private static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static int getLevel(ItemStack s) { CompoundTag t = tag(s); return t.contains("SpriteLevel") ? Math.max(1, t.getInt("SpriteLevel")) : 1; }
    public static int getXp(ItemStack s) { return tag(s).getInt("SpriteXp"); }
    public static boolean getFlag(ItemStack s, String k) { return tag(s).getBoolean(k); }
    public static void setFlag(ItemStack s, String k) { CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putBoolean(k, true)); }
    public static void setLevelXp(ItemStack s, int level, int xp) {
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> { t.putInt("SpriteLevel", level); t.putInt("SpriteXp", xp); });
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent name = Component.translatable("sprite.fnsprites." + type.id).withStyle(type.tier.color);
        if (finish != Finish.BASE) name.append(" ").append(Component.translatable("finish.fnsprites." + finish.id));
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tier.fnsprites." + type.tier.name().toLowerCase()).withStyle(type.tier.color));
        tip.add(Component.translatable("ability.fnsprites." + type.id).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.fnsprites.level", getLevel(stack), MAX_LEVEL).withStyle(ChatFormatting.YELLOW));
        tip.add(Component.translatable("finish_desc.fnsprites." + finish.id).withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatable("tooltip.fnsprites.equip").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            // el sonido identifica al espíritu
            sl.playSound(null, player.blockPosition(), FnSprites.SPRITE_SOUNDS[type.ordinal()], SoundSource.PLAYERS, 1.0f, 1.0f);
            if (hand == InteractionHand.MAIN_HAND) {
                ItemStack off = player.getOffhandItem();
                if (off.isEmpty() || off.getItem() instanceof SpriteItem) {
                    player.setItemInHand(InteractionHand.OFF_HAND, stack.copy());
                    player.setItemInHand(InteractionHand.MAIN_HAND, off.copy());
                    player.displayClientMessage(Component.translatable("message.fnsprites.equipped", getName(stack)), true);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
