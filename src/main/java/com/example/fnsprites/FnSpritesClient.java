package com.example.fnsprites;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class FnSpritesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(FnSprites.SPRITE_ENTITY, SpriteRenderer::new);
    }
}
