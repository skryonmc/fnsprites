package com.example.fnsprites;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Espíritu salvaje: vaga por el mundo, hace su sonido y se recoge con clic derecho. */
public class SpriteEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(SpriteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FINISH = SynchedEntityData.defineId(SpriteEntity.class, EntityDataSerializers.INT);

    public SpriteEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, 0);
        builder.define(DATA_FINISH, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public SpriteType getSpriteType() { return SpriteType.values()[Math.floorMod(entityData.get(DATA_TYPE), FnSprites.N)]; }
    public Finish getFinish() { return Finish.values()[Math.floorMod(entityData.get(DATA_FINISH), Finish.values().length)]; }
    public void setSprite(SpriteType t, Finish f) { entityData.set(DATA_TYPE, t.ordinal()); entityData.set(DATA_FINISH, f.ordinal()); }

    @Override
    protected SoundEvent getAmbientSound() { return FnSprites.SPRITE_SOUNDS[getSpriteType().ordinal()]; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        return false; // no se les puede golpear: se recogen con clic derecho
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            ItemStack st = new ItemStack(FnSprites.ITEMS[getSpriteType().ordinal()][getFinish().ordinal()]);
            if (!player.getInventory().add(st)) player.drop(st, false);
            level().playSound(null, blockPosition(), FnSprites.SPRITE_SOUNDS[getSpriteType().ordinal()], SoundSource.NEUTRAL, 1.0f, 1.2f);
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            Finish f = getFinish();
            if (f == Finish.GOLD && random.nextInt(6) == 0)
                level().addParticle(ParticleTypes.END_ROD, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.02, 0);
            else if (f == Finish.CHEAT && random.nextInt(6) == 0)
                level().addParticle(ParticleTypes.HAPPY_VILLAGER, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpriteType", entityData.get(DATA_TYPE));
        tag.putInt("SpriteFinish", entityData.get(DATA_FINISH));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_TYPE, tag.getInt("SpriteType"));
        entityData.set(DATA_FINISH, tag.getInt("SpriteFinish"));
    }
}
