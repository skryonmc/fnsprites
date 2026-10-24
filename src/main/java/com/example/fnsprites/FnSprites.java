package com.example.fnsprites;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Arrays;

public class FnSprites implements ModInitializer {
    public static final String MOD_ID = "fnsprites";
    public static final int N = SpriteType.values().length;

    public static final SoundEvent[] SPRITE_SOUNDS = new SoundEvent[N];
    public static SoundEvent LEVELUP_SOUND, SHRINE_SOUND, BOON_SOUND;
    public static final Item[][] ITEMS = new Item[N][Finish.values().length];
    public static EntityType<SpriteEntity> SPRITE_ENTITY;
    public static Block SHRINE_BLOCK;
    public static Item SHRINE_ITEM;

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static SoundEvent sound(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id(name), SoundEvent.createVariableRangeEvent(id(name)));
    }

    @Override
    public void onInitialize() {
        // Sonidos: uno distinto por cada espíritu, para reconocerlos al oído
        for (SpriteType t : SpriteType.values()) SPRITE_SOUNDS[t.ordinal()] = sound("sprite_" + t.id);
        LEVELUP_SOUND = sound("sprite_levelup");
        SHRINE_SOUND = sound("shrine_awaken");
        BOON_SOUND = sound("shrine_boon");

        // 16 espíritus x 3 acabados = 48 objetos
        for (SpriteType t : SpriteType.values()) {
            for (Finish f : Finish.values()) {
                Item.Properties p = new Item.Properties().stacksTo(1).rarity(t.tier.vanilla);
                ITEMS[t.ordinal()][f.ordinal()] = Registry.register(BuiltInRegistries.ITEM,
                        id(t.id + "_" + f.id), new SpriteItem(p, t, f));
            }
        }

        // Entidad que vaga por el mundo
        SPRITE_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("sprite"),
                EntityType.Builder.of(SpriteEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 0.6f).clientTrackingRange(10).build("sprite"));
        FabricDefaultAttributeRegistry.register(SPRITE_ENTITY, SpriteEntity.createAttributes());

        // Santuario
        SHRINE_BLOCK = Registry.register(BuiltInRegistries.BLOCK, id("shrine"),
                new ShrineBlock(BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.STONE).noOcclusion()
                        .lightLevel(s -> s.getValue(ShrineBlock.ACTIVE) ? 12 : 0)));
        SHRINE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("shrine"),
                new BlockItem(SHRINE_BLOCK, new Item.Properties()));

        // Pestaña creativa
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("sprites"), FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.fnsprites"))
                .icon(() -> new ItemStack(ITEMS[SpriteType.SONIC.ordinal()][0]))
                .displayItems((params, out) -> {
                    out.accept(SHRINE_ITEM);
                    for (SpriteType t : SpriteType.values())
                        for (Finish f : Finish.values()) out.accept(ITEMS[t.ordinal()][f.ordinal()]);
                }).build());

        SpriteAbilities.init();
        SpriteSpawner.init();
        registerCommands();
    }

    private static int spawnCmd(CommandContext<CommandSourceStack> ctx, Finish finish) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        SpriteType t = SpriteType.byId(StringArgumentType.getString(ctx, "sprite"));
        if (t == null) { ctx.getSource().sendFailure(Component.literal("Espíritu desconocido")); return 0; }
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        SpriteSpawner.spawn(p.serverLevel(), p.getX(), p.getY(), p.getZ(), t, finish);
        return 1;
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) ->
            dispatcher.register(Commands.literal("fnsprites").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn")
                    .then(Commands.argument("sprite", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(SpriteType.values()).map(t -> t.id), b))
                        .executes(c -> spawnCmd(c, Finish.BASE))
                        .then(Commands.literal("gold").executes(c -> spawnCmd(c, Finish.GOLD)))
                        .then(Commands.literal("cheat_master").executes(c -> spawnCmd(c, Finish.CHEAT)))))
                .then(Commands.literal("shrine").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    SpriteSpawner.buildShrine(p.serverLevel(), p.blockPosition());
                    return 1;
                }))));
    }
}
