package com.example.fnsprites;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Aparición natural de espíritus y de santuarios (todo cerca de los jugadores, en el Overworld). */
public final class SpriteSpawner {
    private static final List<BlockPos> SHRINES = new ArrayList<>();

    private SpriteSpawner() {}

    public static void init() { ServerTickEvents.END_SERVER_TICK.register(SpriteSpawner::tick); }

    private static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        if (t % 200 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerLevel level = p.serverLevel();
            if (level.dimension() != Level.OVERWORLD) continue;
            RandomSource r = level.random;
            if (r.nextInt(100) < 35) trySpawnNear(level, p, r);
            if (t % 1200 == 0 && r.nextInt(100) < 12) tryBuildShrine(level, p, r);
        }
    }

    public static SpriteType pickType(RandomSource r) {
        int total = 0;
        for (SpriteType t : SpriteType.values()) total += t.tier.weight;
        int roll = r.nextInt(total);
        for (SpriteType t : SpriteType.values()) { roll -= t.tier.weight; if (roll < 0) return t; }
        return SpriteType.SONIC;
    }

    public static Finish pickFinish(RandomSource r) {
        int roll = r.nextInt(100);
        return roll < 8 ? Finish.GOLD : roll < 20 ? Finish.CHEAT : Finish.BASE;
    }

    public static SpriteEntity spawn(ServerLevel level, double x, double y, double z, SpriteType t, Finish f) {
        SpriteEntity e = FnSprites.SPRITE_ENTITY.create(level);
        if (e == null) return null;
        e.moveTo(x, y, z, level.random.nextFloat() * 360f, 0f);
        e.setSprite(t, f);
        level.addFreshEntity(e);
        return e;
    }

    private static boolean groundOk(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockPos below = pos.below();
        return level.getBlockState(pos).isAir()
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.getFluidState(below).isEmpty();
    }

    private static boolean spawnAt(ServerLevel level, int x, int z) {
        BlockPos probe = new BlockPos(x, 64, z);
        if (!level.hasChunkAt(probe)) return false;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!groundOk(level, pos)) return false;
        spawn(level, x + 0.5, y, z + 0.5, pickType(level.random), pickFinish(level.random));
        return true;
    }

    private static void trySpawnNear(ServerLevel level, ServerPlayer p, RandomSource r) {
        if (level.getEntitiesOfClass(SpriteEntity.class, p.getBoundingBox().inflate(48)).size() >= 3) return;
        double ang = r.nextDouble() * Math.PI * 2, dist = 16 + r.nextInt(17);
        spawnAt(level, (int) (p.getX() + Math.cos(ang) * dist), (int) (p.getZ() + Math.sin(ang) * dist));
    }

    /** Hace aparecer espíritus alrededor de un punto (lo usa el santuario). */
    public static void spawnAround(ServerLevel level, BlockPos center, int radius, int count) {
        for (int i = 0, tries = 0; i < count && tries < 20; tries++) {
            int x = center.getX() + level.random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + level.random.nextInt(radius * 2 + 1) - radius;
            if (spawnAt(level, x, z)) i++;
        }
    }

    // ---------- Santuarios ----------
    private static void tryBuildShrine(ServerLevel level, ServerPlayer p, RandomSource r) {
        double ang = r.nextDouble() * Math.PI * 2, dist = 28 + r.nextInt(20);
        int x = (int) (p.getX() + Math.cos(ang) * dist), z = (int) (p.getZ() + Math.sin(ang) * dist);
        if (!level.hasChunkAt(new BlockPos(x, 64, z))) return;
        for (BlockPos s : SHRINES) if (s.distSqr(new BlockPos(x, s.getY(), z)) < 96 * 96) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        // terreno razonablemente plano y sin agua
        for (int dx = -2; dx <= 2; dx += 2)
            for (int dz = -2; dz <= 2; dz += 2) {
                if (!level.hasChunkAt(pos.offset(dx, 0, dz))) return;
                int hy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                if (Math.abs(hy - y) > 1) return;
                if (!level.getFluidState(new BlockPos(x + dx, hy - 1, z + dz)).isEmpty()) return;
            }
        buildShrine(level, pos);
    }

    /** Construye un santuario pequeño (plataforma de ladrillo musgoso, 4 pilares con linterna y el bloque central dormido). */
    public static void buildShrine(ServerLevel level, BlockPos base) {
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos g = base.offset(dx, -1, dz);
                level.setBlockAndUpdate(g, (dx + dz) % 2 == 0 ? mossy : Blocks.STONE_BRICKS.defaultBlockState());
                for (int h = 0; h < 4; h++) level.setBlockAndUpdate(base.offset(dx, h, dz), Blocks.AIR.defaultBlockState());
            }
        for (int sx : new int[]{-2, 2})
            for (int sz : new int[]{-2, 2}) {
                level.setBlockAndUpdate(base.offset(sx, 0, sz), Blocks.STONE_BRICK_WALL.defaultBlockState());
                level.setBlockAndUpdate(base.offset(sx, 1, sz), Blocks.STONE_BRICK_WALL.defaultBlockState());
                level.setBlockAndUpdate(base.offset(sx, 2, sz), Blocks.LANTERN.defaultBlockState());
            }
        level.setBlockAndUpdate(base, FnSprites.SHRINE_BLOCK.defaultBlockState().setValue(ShrineBlock.ACTIVE, false));
        SHRINES.add(base.immutable());
    }
}
