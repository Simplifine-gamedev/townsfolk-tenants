package com.townsfolk.tenants.item;

import com.townsfolk.tenants.TownsfolkTenantsMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    
    public static final Item GUEST_LEDGER = registerItem("guest_ledger",
        new GuestLedgerItem(new Item.Settings().maxCount(1)));
    
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(TownsfolkTenantsMod.MOD_ID, name), item);
    }
    
    public static void registerItems() {
        TownsfolkTenantsMod.LOGGER.info("Registering items for " + TownsfolkTenantsMod.MOD_ID);
        
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(GUEST_LEDGER);
        });
    }
}
