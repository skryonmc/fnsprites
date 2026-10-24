package com.example.fnsprites;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Santuario de espíritus. Dormido: ofrécele un espíritu (clic derecho) y te da una Bendición (efecto)
 * y hace aparecer espíritus cerca. Después queda activo 5 minutos y vuelve a dormirse.
 */
public class ShrineBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);
    private static final int COOLDOWN = 6000;

    public ShrineBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return simpleCodec(ShrineBlock::new); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ACTIVE); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof SpriteItem si)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(ACTIVE)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.fnsprites.shrine_cooldown"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level instanceof ServerLevel sl) {
            stack.shrink(1);
            int mult = si.finish == Finish.GOLD ? 2 : 1;
            int amp = si.type.boonAmp + (si.finish == Finish.CHEAT ? 1 : 0);
            player.addEffect(new MobEffectInstance(si.type.boon, si.type.boonTicks * mult, amp));
            sl.setBlock(pos, state.setValue(ACTIVE, true), 3);
            sl.scheduleTick(pos, this, COOLDOWN);
            sl.playSound(null, pos, FnSprites.SHRINE_SOUND, SoundSource.BLOCKS, 1.0f, 1.0f);
            sl.playSound(null, pos, FnSprites.BOON_SOUND, SoundSource.PLAYERS, 1.0f, 1.0f);
            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.5, 0.6, 0.5, 0.05);
            SpriteSpawner.spawnAround(sl, pos, 4, 1 + sl.random.nextInt(2));
            player.displayClientMessage(Component.translatable("message.fnsprites.shrine_boon",
                    Component.translatable("sprite.fnsprites." + si.type.id)), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide)
            player.displayClientMessage(Component.translatable(state.getValue(ACTIVE)
                    ? "message.fnsprites.shrine_cooldown" : "message.fnsprites.shrine_need"), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, false), 3);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
