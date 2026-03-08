package com.townsfolk.tenants;

import com.townsfolk.signs.RoomType;
import com.townsfolk.signs.RoomTypeRegistry;
import com.townsfolk.signs.world.RoomData;
import com.townsfolk.signs.world.RoomManager;
import com.townsfolk.tenants.block.ModBlocks;
import com.townsfolk.tenants.block.entity.ModBlockEntities;
import com.townsfolk.tenants.command.ModCommands;
import com.townsfolk.tenants.entity.ModEntities;
import com.townsfolk.tenants.item.ModItems;
import com.townsfolk.tenants.world.TenantSpawner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.tag.BlockTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TownsfolkTenantsMod implements ModInitializer {
    public static final String MOD_ID = "townsfolk_tenants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Townsfolk Tenants initializing...");
        
        // Register the Hotel room type with townsfolk_signs
        RoomTypeRegistry.register("hotel", RoomType.ofTag(
            "hotel",
            "Hotel",
            BlockTags.BEDS,
            true,
            MOD_ID
        ));
        
        // Register mod content
        ModBlocks.registerBlocks();
        ModBlockEntities.registerBlockEntities();
        ModItems.registerItems();
        ModEntities.registerEntities();
        ModCommands.registerCommands();
        
        // Server tick for tenant spawning
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getWorlds().forEach(world -> {
                TenantSpawner.tick(world);
            });
        });
        
        LOGGER.info("Townsfolk Tenants initialized! Place wall signs to create hotel rooms.");
    }
}
