package com.example.fnsprites;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;

/**
 * Habilidades pasivas. Un espíritu está "equipado" cuando está en la mano secundaria.
 * Nivel 1-5: sube con eliminaciones (Klombo: curándote; Crown: derrotando jefes).
 */
public final class SpriteAbilities {
    private static final Map<UUID, PState> STATES = new HashMap<>();
    private static final Map<Item, Item> UPGRADES = new HashMap<>();

    private static class PState {
        int hover, cd, prevFood = -1;
        float prevHealth = -1f, healAcc;
        boolean prevSneak;
    }

    private static void up(Item a, Item b) { UPGRADES.put(a, b); }
    static {
        up(Items.WOODEN_SWORD, Items.STONE_SWORD); up(Items.STONE_SWORD, Items.IRON_SWORD); up(Items.IRON_SWORD, Items.DIAMOND_SWORD);
        up(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE); up(Items.STONE_PICKAXE, Items.IRON_PICKAXE); up(Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE);
        up(Items.WOODEN_AXE, Items.STONE_AXE); up(Items.STONE_AXE, Items.IRON_AXE); up(Items.IRON_AXE, Items.DIAMOND_AXE);
        up(Items.WOODEN_SHOVEL, Items.STONE_SHOVEL); up(Items.STONE_SHOVEL, Items.IRON_SHOVEL); up(Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL);
        up(Items.WOODEN_HOE, Items.STONE_HOE); up(Items.STONE_HOE, Items.IRON_HOE); up(Items.IRON_HOE, Items.DIAMOND_HOE);
        up(Items.LEATHER_HELMET, Items.IRON_HELMET); up(Items.LEATHER_CHESTPLATE, Items.IRON_CHESTPLATE);
        up(Items.LEATHER_LEGGINGS, Items.IRON_LEGGINGS); up(Items.LEATHER_BOOTS, Items.IRON_BOOTS);
        up(Items.IRON_HELMET, Items.DIAMOND_HELMET); up(Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE);
        up(Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS); up(Items.IRON_BOOTS, Items.DIAMOND_BOOTS);
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) tick(p, server.getTickCount());
        });
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
            if (entity instanceof ServerPlayer p) onKill(p, killed);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (entity instanceof ServerPlayer p && taken > 0) onHurt(p);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.getPlayer().getUUID()));
    }

    private static void sound(ServerPlayer p, SpriteType t) {
        p.serverLevel().playSound(null, p.blockPosition(), FnSprites.SPRITE_SOUNDS[t.ordinal()], SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    private static void particles(ServerPlayer p, net.minecraft.core.particles.ParticleOptions type, int n) {
        p.serverLevel().sendParticles(type, p.getX(), p.getY() + 0.2, p.getZ(), n, 0.4, 0.2, 0.4, 0.05);
    }

    // ------------------------------------------------------------------ TICK
    private static void tick(ServerPlayer p, int serverTick) {
        PState s = STATES.computeIfAbsent(p.getUUID(), k -> new PState());
        int food = p.getFoodData().getFoodLevel();
        float hp = p.getHealth();
        boolean ate = s.prevFood >= 0 && food > s.prevFood;
        float healed = s.prevHealth >= 0 ? hp - s.prevHealth : 0f;
        s.prevFood = food; s.prevHealth = hp;
        boolean sneakEdge = p.isShiftKeyDown() && !s.prevSneak;
        s.prevSneak = p.isShiftKeyDown();
        if (s.cd > 0) s.cd--;
        if (p.onGround()) s.hover = -1;

        ItemStack stack = p.getOffhandItem();
        if (!(stack.getItem() instanceof SpriteItem si)) return;
        int L = SpriteItem.getLevel(stack);
        ServerLevel sl = p.serverLevel();

        switch (si.type) {
            case EIGHT_BIT -> {
                if (!SpriteItem.getFlag(stack, "Given")) {
                    SpriteItem.setFlag(stack, "Given");
                    ItemStack gun = new ItemStack(Items.CROSSBOW);
                    if (!p.getInventory().add(gun)) p.drop(gun, false);
                    sound(p, si.type);
                }
            }
            case BUSH -> {
                if (serverTick % Math.max(100, 700 - 120 * L) == 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, true, false));
                    particles(p, ParticleTypes.HAPPY_VILLAGER, 20);
                    sound(p, si.type);
                }
            }
            case MEGA_MAN -> {
                if (p.isShiftKeyDown() && p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.0004) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, L + 1, true, false));
                    if (serverTick % 3 == 0) particles(p, ParticleTypes.CLOUD, 2);
                }
            }
            case ONIGIRI -> {
                if (ate) {
                    int dur = 100 + 40 * L;
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 1, true, true));
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, dur, 1, true, true));
                    sound(p, si.type);
                }
            }
            case OVERSHIELD -> {
                if (serverTick % 20 == 0 && !p.hasEffect(MobEffects.ABSORPTION))
                    p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, (L - 1) / 2, true, false));
            }
            case STORM_SCOUT -> {
                if (sl.isRainingAt(p.blockPosition())) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, L >= 3 ? 1 : 0, true, false));
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false));
                    if (L >= 5) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false));
                }
            }
            case SONIC -> {
                if (p.isSprinting()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, L, true, false));
            }
            case TAILS -> {
                if (!p.onGround() && p.isShiftKeyDown() && !p.getAbilities().flying) {
                    if (s.hover < 0) s.hover = 40 + 20 * L;
                    if (s.hover > 0) {
                        s.hover--;
                        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 6, 0, true, false));
                        p.fallDistance = 0;
                        if (serverTick % 4 == 0) particles(p, ParticleTypes.CLOUD, 2);
                        if (s.hover == 40 + 20 * L - 1) sound(p, si.type);
                    }
                }
            }
            case SHADOW -> {
                if (serverTick % Math.max(30, 130 - 20 * L) == 0) reloadCrossbow(p, si);
            }
            case KILLSWITCH -> {
                if (!p.onGround() && p.getDeltaMovement().y < -0.25 && s.cd <= 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 12 + 6 * L, 0, true, false));
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12 + 6 * L, 0, true, false));
                    s.cd = 160;
                    sound(p, si.type);
                    particles(p, ParticleTypes.ELECTRIC_SPARK, 15);
                }
            }
            case JACKRABBIT -> {
                if (sneakEdge && !p.onGround() && s.cd <= 0 && !p.getAbilities().flying) {
                    var look = p.getLookAngle();
                    p.setDeltaMovement(look.x * 0.3, 0.62, look.z * 0.3);
                    p.hurtMarked = true;
                    p.fallDistance = 0;
                    s.cd = Math.max(40, 220 - 40 * L);
                    sound(p, si.type);
                    particles(p, ParticleTypes.CLOUD, 12);
                }
            }
            case X_RAY -> {
                if (serverTick % Math.max(80, 300 - 40 * L) == 0) {
                    double r = 8 + 3 * L;
                    List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                            e -> e != p && (e instanceof Enemy || e instanceof Player));
                    if (!list.isEmpty()) {
                        for (LivingEntity e : list) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false));
                        sound(p, si.type);
                    }
                }
            }
            case KLOMBO -> {
                if (healed > 0) {
                    s.healAcc += healed;
                    if (s.healAcc >= 10f) { s.healAcc -= 10f; addXp(p, stack, si, need(si)); }
                }
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ EVENTOS
    private static void onHurt(ServerPlayer p) {
        ItemStack stack = p.getOffhandItem();
        if (stack.getItem() instanceof SpriteItem si && si.type == SpriteType.JONESY) {
            int L = SpriteItem.getLevel(stack);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40 + 20 * L, 1));
            if (L >= 3) p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            sound(p, si.type);
        }
    }

    private static void onKill(ServerPlayer p, LivingEntity killed) {
        ItemStack stack = p.getOffhandItem();
        if (!(stack.getItem() instanceof SpriteItem si)) return;
        int L = SpriteItem.getLevel(stack);
        switch (si.type) {
            case KLOMBO -> { }
            case CROWN -> {
                if (killed instanceof EnderDragon || killed instanceof WitherBoss) {
                    addXp(p, stack, si, need(si));
                    ItemStack gold = new ItemStack(Items.GOLD_BLOCK, Math.min(64, 2 * SpriteItem.getLevel(stack)));
                    if (!p.getInventory().add(gold)) p.drop(gold, false);
                    sound(p, si.type);
                }
            }
            default -> {
                addXp(p, stack, si, si.finish == Finish.GOLD ? 3 : 1);
                if (si.type == SpriteType.EIGHT_BIT) ExperienceOrb.award(p.serverLevel(), killed.position(), L * 2);
                if (si.type == SpriteType.BUSH && L >= 5) {
                    p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, true, false));
                    particles(p, ParticleTypes.HAPPY_VILLAGER, 20);
                }
            }
        }
    }

    // ------------------------------------------------------------------ NIVELES
    private static int need(SpriteItem si) { return si.finish == Finish.CHEAT ? 2 : 4; }

    private static void addXp(ServerPlayer p, ItemStack stack, SpriteItem si, int amount) {
        int L = SpriteItem.getLevel(stack);
        if (L >= SpriteItem.MAX_LEVEL) return;
        int xp = SpriteItem.getXp(stack) + amount, need = need(si);
        boolean leveled = false;
        while (xp >= need && L < SpriteItem.MAX_LEVEL) {
            xp -= need; L++; leveled = true;
            onLevelUp(p, si);
        }
        SpriteItem.setLevelXp(stack, L, xp);
        if (leveled) {
            p.displayClientMessage(Component.translatable("message.fnsprites.levelup", si.getName(stack), L), true);
            p.serverLevel().playSound(null, p.blockPosition(), FnSprites.LEVELUP_SOUND, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private static void onLevelUp(ServerPlayer p, SpriteItem si) {
        if (si.type == SpriteType.ADVENTURE) upgradeRandomItem(p);
        if (si.type == SpriteType.KLOMBO) randomGift(p);
    }

    private static void upgradeRandomItem(ServerPlayer p) {
        Inventory inv = p.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) if (UPGRADES.containsKey(inv.getItem(i).getItem())) slots.add(i);
        if (slots.isEmpty()) return;
        int slot = slots.get(p.getRandom().nextInt(slots.size()));
        ItemStack old = inv.getItem(slot);
        ItemStack ns = new ItemStack(UPGRADES.get(old.getItem()));
        ItemEnchantments ench = old.get(DataComponents.ENCHANTMENTS);
        if (ench != null) ns.set(DataComponents.ENCHANTMENTS, ench);
        inv.setItem(slot, ns);
    }

    private static final Item[] GIFTS = { Items.GOLDEN_APPLE, Items.COOKED_BEEF, Items.ARROW, Items.EXPERIENCE_BOTTLE,
            Items.IRON_INGOT, Items.GOLDEN_CARROT, Items.ENDER_PEARL, Items.DIAMOND };
    private static final int[] GIFT_N = { 1, 8, 16, 4, 6, 4, 2, 1 };

    private static void randomGift(ServerPlayer p) {
        int i = p.getRandom().nextInt(GIFTS.length);
        ItemStack g = new ItemStack(GIFTS[i], GIFT_N[i]);
        if (!p.getInventory().add(g)) p.drop(g, false);
    }

    private static void reloadCrossbow(ServerPlayer p, SpriteItem si) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack c = inv.getItem(i);
            if (c.getItem() instanceof CrossbowItem && !CrossbowItem.isCharged(c)) {
                for (int j = 0; j < inv.getContainerSize(); j++) {
                    ItemStack a = inv.getItem(j);
                    if (a.is(Items.ARROW)) {
                        a.shrink(1);
                        c.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(new ItemStack(Items.ARROW)));
                        sound(p, si.type);
                        return;
                    }
                }
                return;
            }
        }
    }
}
