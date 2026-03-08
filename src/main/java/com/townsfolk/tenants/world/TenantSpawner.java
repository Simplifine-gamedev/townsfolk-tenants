package com.townsfolk.tenants.world;

import com.townsfolk.signs.RoomType;
import com.townsfolk.signs.RoomTypeRegistry;
import com.townsfolk.signs.SignTextUpdater;
import com.townsfolk.signs.world.RoomData;
import com.townsfolk.signs.world.RoomManager;
import com.townsfolk.tenants.TownsfolkTenantsMod;
import com.townsfolk.tenants.entity.ModEntities;
import com.townsfolk.tenants.entity.TenantEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class TenantSpawner {
    private static final String HOTEL_ROOM_TYPE = "hotel";
    
    public static void tick(ServerWorld world) {
        RoomManager manager = RoomManager.getServerState(world);
        List<RoomData> hotelRooms = manager.getRoomsByType(HOTEL_ROOM_TYPE);
        
        long currentTick = world.getTime();
        
        for (RoomData room : hotelRooms) {
            if (!room.isValid()) continue;
            if (room.hasTenant()) continue;
            
            // Schedule spawn if not already scheduled
            if (room.getSpawnTenantAtTick() <= 0) {
                room.setSpawnTenantAtTick(currentTick + 24000); // 1 day delay
                manager.updateRoom(room);
                continue;
            }
            
            // Check if it's time to spawn
            if (currentTick >= room.getSpawnTenantAtTick()) {
                spawnTenant(world, room, manager);
            }
        }
    }
    
    private static void spawnTenant(ServerWorld world, RoomData room, RoomManager manager) {
        BlockPos anchorPos = room.getAnchorPos();
        if (anchorPos == null) {
            TownsfolkTenantsMod.LOGGER.warn("Room #{} has no anchor position", room.getRoomNumber());
            return;
        }
        
        TownsfolkTenantsMod.LOGGER.info("Attempting to spawn tenant for room #{} at anchor {}", room.getRoomNumber(), anchorPos);
        
        // Find spawn position near the bed
        BlockPos spawnPos = findSpawnPosition(world, anchorPos);
        if (spawnPos == null) {
            TownsfolkTenantsMod.LOGGER.warn("Could not find spawn position for room #{} near anchor {}", room.getRoomNumber(), anchorPos);
            // Still mark as having tenant but log the issue
            String name = generateTenantName();
            room.setTenant(java.util.UUID.randomUUID(), name + " (no spawn)", "Guest");
            room.clearSpawnSchedule();
            manager.updateRoom(room);
            updateSignColor(world, room);
            TownsfolkTenantsMod.LOGGER.warn("Room #{} marked occupied but tenant could not spawn - room may be too small", room.getRoomNumber());
            return;
        }
        
        TownsfolkTenantsMod.LOGGER.info("Found spawn position {} for room #{}", spawnPos, room.getRoomNumber());
        
        TenantEntity tenant = ModEntities.TENANT.create(world);
        if (tenant == null) {
            TownsfolkTenantsMod.LOGGER.error("Failed to create TenantEntity for room #{}", room.getRoomNumber());
            return;
        }
        
        tenant.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        tenant.setSignPos(room.getSignPos());
        tenant.setHomePos(anchorPos);
        
        // Generate a name
        String name = generateTenantName();
        tenant.setCustomName(net.minecraft.text.Text.literal(name));
        
        TownsfolkTenantsMod.LOGGER.info("Spawning tenant '{}' at {} for room #{}", name, spawnPos, room.getRoomNumber());
        
        boolean spawned = world.spawnEntity(tenant);
        if (!spawned) {
            TownsfolkTenantsMod.LOGGER.error("world.spawnEntity returned false for room #{}", room.getRoomNumber());
            return;
        }
        
        // Update room data
        room.setTenant(tenant.getUuid(), name, "Guest");
        room.clearSpawnSchedule();
        manager.updateRoom(room);
        
        // Update sign color to green (occupied)
        updateSignColor(world, room);
        
        TownsfolkTenantsMod.LOGGER.info("Successfully spawned tenant '{}' in room #{} at {}", name, room.getRoomNumber(), spawnPos);
    }
    
    public static void updateSignColor(ServerWorld world, RoomData room) {
        RoomType type = RoomTypeRegistry.getType(room.getRoomTypeId());
        String typeName = type != null ? type.displayName() : "Room";
        SignTextUpdater.updateRoomStatus(world, room.getSignPos(), typeName, room.getRoomNumber(), room.isValid(), room.hasTenant());
    }
    
    private static BlockPos findSpawnPosition(ServerWorld world, BlockPos anchorPos) {
        // Try positions around the anchor (bed) - expanded search
        int[][] offsets = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},  // Adjacent
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}, // Diagonals
            {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2},   // 2 blocks away
            {0, 1, 0}  // On top of bed
        };
        
        for (int[] offset : offsets) {
            BlockPos checkPos = anchorPos.add(offset[0], offset[1], offset[2]);
            if (canSpawnAt(world, checkPos)) {
                return checkPos;
            }
        }
        
        // Try one block up from all positions
        for (int[] offset : offsets) {
            BlockPos checkPos = anchorPos.add(offset[0], offset[1] + 1, offset[2]);
            if (canSpawnAt(world, checkPos)) {
                return checkPos;
            }
        }
        
        // Last resort: try directly above the anchor
        BlockPos aboveAnchor = anchorPos.up();
        if (canSpawnAt(world, aboveAnchor)) {
            return aboveAnchor;
        }
        
        return null;
    }
    
    private static boolean canSpawnAt(ServerWorld world, BlockPos pos) {
        // Check if there's space for an entity (need 2 blocks of non-solid space)
        return !world.getBlockState(pos).isSolidBlock(world, pos) && 
               !world.getBlockState(pos.up()).isSolidBlock(world, pos.up());
    }
    
    private static final String[] FIRST_NAMES = {
        "Alex", "Steve", "Emma", "Oliver", "Sophia", "Liam", "Ava", "Noah",
        "Isabella", "Mason", "Mia", "Ethan", "Charlotte", "Lucas", "Amelia",
        "James", "Harper", "Benjamin", "Evelyn", "Henry", "Abigail", "Sebastian",
        "Emily", "Jack", "Elizabeth", "Aiden", "Sofia", "Owen", "Avery", "Samuel"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
        "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez",
        "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
        "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez"
    };
    
    private static String generateTenantName() {
        String firstName = FIRST_NAMES[(int) (Math.random() * FIRST_NAMES.length)];
        String lastName = LAST_NAMES[(int) (Math.random() * LAST_NAMES.length)];
        return firstName + " " + lastName;
    }
    
    public static int[] fillVacantRooms(ServerWorld world) {
        RoomManager manager = RoomManager.getServerState(world);
        List<RoomData> hotelRooms = manager.getRoomsByType(HOTEL_ROOM_TYPE);
        
        int spawned = 0;
        int invalid = 0;
        
        for (RoomData room : hotelRooms) {
            if (!room.isValid()) {
                invalid++;
                continue;
            }
            if (room.hasTenant()) continue;
            
            spawnTenant(world, room, manager);
            spawned++;
        }
        
        return new int[]{spawned, invalid};
    }
    
    public static int evictAllTenants(ServerWorld world) {
        RoomManager manager = RoomManager.getServerState(world);
        List<RoomData> hotelRooms = manager.getRoomsByType(HOTEL_ROOM_TYPE);
        int count = 0;
        
        for (RoomData room : hotelRooms) {
            if (room.hasTenant()) {
                room.clearTenant();
                manager.updateRoom(room);
                updateSignColor(world, room);
                count++;
            }
        }
        
        return count;
    }
}
