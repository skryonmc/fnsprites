package com.example.fnsprites;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Rarity;

/** Los 16 Sprites base de Fortnite Capítulo 7 Temporada 4 (Override). */
public enum SpriteType {
    EIGHT_BIT("eight_bit", Tier.RARE, MobEffects.LUCK, 0, 6000),
    ADVENTURE("adventure", Tier.RARE, MobEffects.DIG_SPEED, 0, 6000),
    BUSH("bush", Tier.RARE, MobEffects.INVISIBILITY, 0, 600),
    JONESY("jonesy", Tier.RARE, MobEffects.REGENERATION, 0, 3000),
    MEGA_MAN("mega_man", Tier.RARE, MobEffects.DAMAGE_RESISTANCE, 0, 6000),
    ONIGIRI("onigiri", Tier.RARE, MobEffects.SATURATION, 0, 600),
    OVERSHIELD("overshield", Tier.RARE, MobEffects.ABSORPTION, 2, 6000),
    STORM_SCOUT("storm_scout", Tier.RARE, MobEffects.WATER_BREATHING, 0, 6000),
    SONIC("sonic", Tier.EPIC, MobEffects.MOVEMENT_SPEED, 1, 6000),
    TAILS("tails", Tier.EPIC, MobEffects.SLOW_FALLING, 0, 6000),
    SHADOW("shadow", Tier.EPIC, MobEffects.DAMAGE_BOOST, 0, 6000),
    KILLSWITCH("killswitch", Tier.EPIC, MobEffects.JUMP, 1, 6000),
    JACKRABBIT("jackrabbit", Tier.LEGENDARY, MobEffects.JUMP, 2, 4000),
    X_RAY("x_ray", Tier.LEGENDARY, MobEffects.NIGHT_VISION, 0, 6000),
    KLOMBO("klombo", Tier.MYTHIC, MobEffects.HEALTH_BOOST, 2, 6000),
    CROWN("crown", Tier.MYTHIC, MobEffects.HERO_OF_THE_VILLAGE, 0, 6000);

    public enum Tier {
        RARE(8, ChatFormatting.AQUA, Rarity.RARE),
        EPIC(4, ChatFormatting.LIGHT_PURPLE, Rarity.EPIC),
        LEGENDARY(2, ChatFormatting.GOLD, Rarity.EPIC),
        MYTHIC(1, ChatFormatting.RED, Rarity.EPIC);
        public final int weight; public final ChatFormatting color; public final Rarity vanilla;
        Tier(int w, ChatFormatting c, Rarity r) { weight = w; color = c; vanilla = r; }
    }

    public final String id;
    public final Tier tier;
    public final Holder<MobEffect> boon;
    public final int boonAmp, boonTicks;

    SpriteType(String id, Tier tier, Holder<MobEffect> boon, int amp, int ticks) {
        this.id = id; this.tier = tier; this.boon = boon; this.boonAmp = amp; this.boonTicks = ticks;
    }

    public static SpriteType byId(String id) {
        for (SpriteType t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
