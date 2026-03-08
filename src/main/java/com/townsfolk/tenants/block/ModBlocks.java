package com.townsfolk.tenants.block;

import com.townsfolk.tenants.TownsfolkTenantsMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    
    public static final Block TIP_CHEST = registerBlock("tip_chest",
        new TipChestBlock(AbstractBlock.Settings.create()
            .strength(2.5f)
            .sounds(BlockSoundGroup.WOOD)));
    
    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(TownsfolkTenantsMod.MOD_ID, name), block);
    }
    
    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(TownsfolkTenantsMod.MOD_ID, name),
            new BlockItem(block, new Item.Settings()));
    }
    
    public static void registerBlocks() {
        TownsfolkTenantsMod.LOGGER.info("Registering blocks for " + TownsfolkTenantsMod.MOD_ID);
        
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(TIP_CHEST);
        });
    }
}
