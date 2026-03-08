package com.townsfolk.tenants;

import com.townsfolk.tenants.entity.TenantEntityRenderer;
import com.townsfolk.tenants.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class TownsfolkTenantsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.TENANT, TenantEntityRenderer::new);
    }
}
