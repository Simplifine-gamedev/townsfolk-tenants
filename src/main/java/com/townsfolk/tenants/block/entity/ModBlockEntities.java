package com.townsfolk.tenants.block.entity;

import com.townsfolk.tenants.TownsfolkTenantsMod;
import com.townsfolk.tenants.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    
    public static final BlockEntityType<TipChestBlockEntity> TIP_CHEST_BLOCK_ENTITY = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Identifier.of(TownsfolkTenantsMod.MOD_ID, "tip_chest_block_entity"),
        FabricBlockEntityTypeBuilder.create(TipChestBlockEntity::new, ModBlocks.TIP_CHEST).build()
    );
    
    public static void registerBlockEntities() {
        TownsfolkTenantsMod.LOGGER.info("Registering block entities for " + TownsfolkTenantsMod.MOD_ID);
    }
}
