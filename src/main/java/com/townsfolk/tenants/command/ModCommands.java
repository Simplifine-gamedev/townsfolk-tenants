package com.townsfolk.tenants.command;

import com.townsfolk.signs.world.RoomData;
import com.townsfolk.signs.world.RoomManager;
import com.townsfolk.tenants.TownsfolkTenantsMod;
import com.townsfolk.tenants.entity.TenantEntity;
import com.townsfolk.tenants.world.TenantSpawner;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ModCommands {
    
    public static void registerCommands() {
        TownsfolkTenantsMod.LOGGER.info("Registering commands for " + TownsfolkTenantsMod.MOD_ID);
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("hotel")
                .then(CommandManager.literal("info")
                    .executes(context -> executeInfo(context.getSource()))
                )
                .then(CommandManager.literal("fill")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> executeFill(context.getSource()))
                )
                .then(CommandManager.literal("evict")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("room", IntegerArgumentType.integer(1))
                        .executes(context -> executeEvict(context.getSource(), 
                            IntegerArgumentType.getInteger(context, "room")))
                    )
                )
                .then(CommandManager.literal("evictall")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> executeEvictAll(context.getSource()))
                )
                .then(CommandManager.literal("payday")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> executePayday(context.getSource()))
                )
                .then(CommandManager.literal("reset")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> executeReset(context.getSource()))
                )
            );
        });
    }
    
    private static int executeInfo(ServerCommandSource source) {
        RoomManager manager = RoomManager.getServerState(source.getServer());
        List<RoomData> hotelRooms = manager.getRoomsByType("hotel");
        
        if (hotelRooms.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No hotel rooms registered.").formatted(Formatting.YELLOW), false);
            return 1;
        }
        
        source.sendFeedback(() -> Text.literal("=== Hotel Rooms ===").formatted(Formatting.GOLD), false);
        for (RoomData room : hotelRooms) {
            String status = room.isValid() ? 
                (room.hasTenant() ? "Occupied" : "Vacant") : "Invalid";
            Formatting color = room.isValid() ? 
                (room.hasTenant() ? Formatting.GREEN : Formatting.YELLOW) : Formatting.RED;
            
            String info = room.hasTenant() ? room.getTenantName() : TenantEntity.getTierName(room.getQuality());
            
            source.sendFeedback(() -> Text.literal(String.format(
                "#%d: %s | Quality: %d | %s", 
                room.getRoomNumber(), status, room.getQuality(), info
            )).formatted(color), false);
        }
        
        return 1;
    }
    
    private static int executeFill(ServerCommandSource source) {
        int[] counts = TenantSpawner.fillVacantRooms(source.getWorld());
        
        source.sendFeedback(() -> Text.literal(String.format(
            "Spawned tenants in %d rooms. %d rooms invalid.", counts[0], counts[1]
        )).formatted(Formatting.GREEN), true);
        
        return 1;
    }
    
    private static int executeEvict(ServerCommandSource source, int roomNumber) {
        RoomManager manager = RoomManager.getServerState(source.getServer());
        
        // Find room by number in hotel rooms
        List<RoomData> hotelRooms = manager.getRoomsByType("hotel");
        RoomData targetRoom = null;
        for (RoomData room : hotelRooms) {
            if (room.getRoomNumber() == roomNumber) {
                targetRoom = room;
                break;
            }
        }
        
        if (targetRoom == null || !targetRoom.hasTenant()) {
            source.sendFeedback(() -> Text.literal("Room #" + roomNumber + " not found or has no tenant")
                .formatted(Formatting.RED), false);
            return 0;
        }
        
        targetRoom.clearTenant();
        manager.updateRoom(targetRoom);
        
        source.sendFeedback(() -> Text.literal("Evicted tenant from room #" + roomNumber)
            .formatted(Formatting.YELLOW), true);
        
        return 1;
    }
    
    private static int executeEvictAll(ServerCommandSource source) {
        int count = TenantSpawner.evictAllTenants(source.getWorld());
        
        source.sendFeedback(() -> Text.literal("Evicted " + count + " tenants")
            .formatted(Formatting.YELLOW), true);
        
        return 1;
    }
    
    private static int executePayday(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        
        int paidCount = 0;
        int totalPayment = 0;
        
        // Iterate through all loaded entities and find tenants
        for (var entity : world.iterateEntities()) {
            if (entity instanceof TenantEntity tenant) {
                int payment = tenant.forcePayRent();
                if (payment > 0) {
                    paidCount++;
                    totalPayment += payment;
                }
            }
        }
        
        final int finalPaidCount = paidCount;
        final int finalTotalPayment = totalPayment;
        
        source.sendFeedback(() -> Text.literal(String.format(
            "Payday! %d tenants paid a total of %d emeralds.", finalPaidCount, finalTotalPayment
        )).formatted(Formatting.GREEN), true);
        
        return 1;
    }
    
    private static int executeReset(ServerCommandSource source) {
        RoomManager manager = RoomManager.getServerState(source.getServer());
        manager.resetAll();
        
        source.sendFeedback(() -> Text.literal("All rooms reset. Place wall signs to create new rooms.")
            .formatted(Formatting.GREEN), true);
        
        return 1;
    }
}
