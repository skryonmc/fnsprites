package com.example.fnsprites;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Dibuja al espíritu usando el mismo modelo 3D de su objeto (flotando y con vaivén). */
public class SpriteRenderer extends EntityRenderer<SpriteEntity> {
    private static final ItemStack[][] CACHE = new ItemStack[FnSprites.N][Finish.values().length];

    public SpriteRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.25f;
    }

    @Override
    public ResourceLocation getTextureLocation(SpriteEntity e) {
        return FnSprites.id("textures/item/sonic_base.png"); // no se usa, el modelo trae su textura
    }

    @Override
    public void render(SpriteEntity e, float yaw, float partialTick, PoseStack ps, MultiBufferSource buf, int light) {
        int t = e.getSpriteType().ordinal(), f = e.getFinish().ordinal();
        if (CACHE[t][f] == null) CACHE[t][f] = new ItemStack(FnSprites.ITEMS[t][f]);

        ps.pushPose();
        float bob = Mth.sin((e.tickCount + partialTick) * 0.12f) * 0.05f;
        ps.translate(0.0, 0.4 + bob, 0.0);
        ps.mulPose(Axis.YP.rotationDegrees(180.0f - yaw)); // si lo ves de espaldas, cambia 180.0f por 0.0f
        ps.scale(1.25f, 1.25f, 1.25f);
        Minecraft.getInstance().getItemRenderer().renderStatic(CACHE[t][f], ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, ps, buf, e.level(), e.getId());
        ps.popPose();
        super.render(e, yaw, partialTick, ps, buf, light);
    }
}
