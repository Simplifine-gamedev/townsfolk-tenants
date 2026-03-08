package com.townsfolk.tenants.entity;

import com.townsfolk.tenants.TownsfolkTenantsMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    
    public static final EntityType<TenantEntity> TENANT = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(TownsfolkTenantsMod.MOD_ID, "tenant"),
        EntityType.Builder.<TenantEntity>create(TenantEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.6f, 1.95f)
            .build()
    );
    
    public static void registerEntities() {
        TownsfolkTenantsMod.LOGGER.info("Registering entities for " + TownsfolkTenantsMod.MOD_ID);
        FabricDefaultAttributeRegistry.register(TENANT, VillagerEntity.createVillagerAttributes());
    }
}
