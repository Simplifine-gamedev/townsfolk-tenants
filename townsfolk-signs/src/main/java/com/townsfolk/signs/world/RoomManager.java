package com.townsfolk.signs.world;

import com.townsfolk.signs.TownsfolkSignsMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.*;

public class RoomManager extends PersistentState {
    private int nextRoomNumber = 1;
    private final Map<BlockPos, RoomData> rooms = new HashMap<>();
    private final Map<BlockPos, BlockPos> anchorToSign = new HashMap<>();
    private final TreeSet<Integer> availableNumbers = new TreeSet<>();
    
    public RoomManager() {
        super();
    }
    
    public int getNextRoomNumber() {
        int num;
        if (!availableNumbers.isEmpty()) {
            num = availableNumbers.pollFirst();
        } else {
            num = nextRoomNumber;
            nextRoomNumber++;
        }
        markDirty();
        return num;
    }
    
    public void registerRoom(RoomData room) {
        rooms.put(room.getSignPos(), room);
        if (room.getAnchorPos() != null) {
            anchorToSign.put(room.getAnchorPos(), room.getSignPos());
        }
        availableNumbers.remove(room.getRoomNumber());
        markDirty();
    }
    
    public void removeRoom(BlockPos signPos) {
        RoomData room = rooms.remove(signPos);
        if (room != null) {
            if (room.getAnchorPos() != null) {
                anchorToSign.remove(room.getAnchorPos());
            }
        }
        
        // If no rooms left, reset the counter
        if (rooms.isEmpty()) {
            nextRoomNumber = 1;
            availableNumbers.clear();
            TownsfolkSignsMod.LOGGER.info("All rooms removed, reset room counter to 1");
        }
        
        markDirty();
    }
    
    /**
     * Renumber all rooms of a given type to be sequential starting from 1.
     * Returns a map of old room numbers to new room numbers for sign updates.
     */
    public Map<Integer, Integer> renumberRooms(String roomTypeId) {
        List<RoomData> roomsOfType = getRoomsByType(roomTypeId);
        
        // If no rooms of this type, reset counter if no rooms at all
        if (roomsOfType.isEmpty()) {
            if (rooms.isEmpty()) {
                nextRoomNumber = 1;
                availableNumbers.clear();
            }
            return Collections.emptyMap();
        }
        
        // Sort by current room number to maintain relative order
        roomsOfType.sort(Comparator.comparingInt(RoomData::getRoomNumber));
        
        Map<Integer, Integer> renumberMap = new HashMap<>();
        int newNumber = 1;
        
        for (RoomData room : roomsOfType) {
            int oldNumber = room.getRoomNumber();
            if (oldNumber != newNumber) {
                renumberMap.put(oldNumber, newNumber);
                room.setRoomNumber(newNumber);
            }
            newNumber++;
        }
        
        // Update nextRoomNumber to be after the highest room number
        // Find max across ALL room types
        int maxNumber = 0;
        for (RoomData room : rooms.values()) {
            if (room.getRoomNumber() > maxNumber) {
                maxNumber = room.getRoomNumber();
            }
        }
        nextRoomNumber = maxNumber + 1;
        
        // Clear available numbers since we've renumbered
        availableNumbers.clear();
        
        if (!renumberMap.isEmpty()) {
            markDirty();
            TownsfolkSignsMod.LOGGER.info("Renumbered rooms, next room number is now {}", nextRoomNumber);
        }
        
        return renumberMap;
    }
    
    public boolean hasRoom(BlockPos signPos) {
        return rooms.containsKey(signPos);
    }
    
    public RoomData getRoom(BlockPos signPos) {
        return rooms.get(signPos);
    }
    
    public RoomData getRoomByNumber(int roomNumber) {
        for (RoomData room : rooms.values()) {
            if (room.getRoomNumber() == roomNumber) {
                return room;
            }
        }
        return null;
    }
    
    public boolean isAnchorAssigned(BlockPos anchorPos) {
        return anchorToSign.containsKey(anchorPos);
    }
    
    public RoomData getRoomByAnchor(BlockPos anchorPos) {
        BlockPos signPos = anchorToSign.get(anchorPos);
        return signPos != null ? rooms.get(signPos) : null;
    }
    
    public Map<BlockPos, RoomData> getAllRooms() {
        return new HashMap<>(rooms);
    }
    
    public List<RoomData> getRoomsByType(String roomTypeId) {
        List<RoomData> result = new ArrayList<>();
        for (RoomData room : rooms.values()) {
            if (room.getRoomTypeId().equals(roomTypeId)) {
                result.add(room);
            }
        }
        return result;
    }
    
    public void updateRoom(RoomData room) {
        rooms.put(room.getSignPos(), room);
        markDirty();
    }
    
    public void resetAll() {
        rooms.clear();
        anchorToSign.clear();
        availableNumbers.clear();
        nextRoomNumber = 1;
        markDirty();
    }
    
    public void resetRoomCounter() {
        resetAll();
    }
    
    public boolean evictTenant(ServerWorld world, int roomNumber) {
        RoomData room = getRoomByNumber(roomNumber);
        if (room == null || !room.hasTenant()) {
            return false;
        }
        room.clearTenant();
        markDirty();
        return true;
    }
    
    public int evictTenantsByType(ServerWorld world, String roomTypeId) {
        int count = 0;
        for (RoomData room : rooms.values()) {
            if (room.getRoomTypeId().equals(roomTypeId) && room.hasTenant()) {
                room.clearTenant();
                count++;
            }
        }
        if (count > 0) {
            markDirty();
        }
        return count;
    }
    
    public int[] fillVacantRoomsByType(ServerWorld world, String roomTypeId) {
        int spawned = 0;
        int invalid = 0;
        
        for (RoomData room : rooms.values()) {
            if (!room.getRoomTypeId().equals(roomTypeId)) continue;
            
            if (!room.isValid()) {
                invalid++;
                continue;
            }
            if (room.hasTenant()) {
                continue;
            }
            
            // Mark as having a tenant (actual spawning is done by the building mod)
            room.setTenant(java.util.UUID.randomUUID(), "Tenant", "Guest");
            spawned++;
        }
        
        if (spawned > 0) {
            markDirty();
        }
        
        return new int[]{spawned, invalid};
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("nextRoomNumber", nextRoomNumber);
        
        int[] availableArray = availableNumbers.stream().mapToInt(Integer::intValue).toArray();
        nbt.putIntArray("availableNumbers", availableArray);
        
        NbtList roomsList = new NbtList();
        for (RoomData room : rooms.values()) {
            NbtCompound roomNbt = new NbtCompound();
            roomNbt.putString("roomTypeId", room.getRoomTypeId());
            roomNbt.putInt("roomNumber", room.getRoomNumber());
            roomNbt.putLong("signPos", room.getSignPos().asLong());
            if (room.getAnchorPos() != null) {
                roomNbt.putLong("anchorPos", room.getAnchorPos().asLong());
            }
            roomNbt.putInt("quality", room.getQuality());
            roomNbt.putBoolean("valid", room.isValid());
            
            if (room.hasTenant()) {
                roomNbt.putUuid("tenantUuid", room.getTenantUuid());
                roomNbt.putString("tenantName", room.getTenantName());
                roomNbt.putString("tenantType", room.getTenantType());
            }
            
            roomsList.add(roomNbt);
        }
        nbt.put("rooms", roomsList);
        
        return nbt;
    }
    
    public static RoomManager createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        RoomManager manager = new RoomManager();
        manager.nextRoomNumber = nbt.getInt("nextRoomNumber");
        
        int[] availableArray = nbt.getIntArray("availableNumbers");
        for (int num : availableArray) {
            manager.availableNumbers.add(num);
        }
        
        NbtList roomsList = nbt.getList("rooms", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < roomsList.size(); i++) {
            NbtCompound roomNbt = roomsList.getCompound(i);
            
            String roomTypeId = roomNbt.getString("roomTypeId");
            int roomNumber = roomNbt.getInt("roomNumber");
            BlockPos signPos = BlockPos.fromLong(roomNbt.getLong("signPos"));
            
            RoomData room = new RoomData(roomTypeId, roomNumber, signPos);
            
            if (roomNbt.contains("anchorPos")) {
                room.setAnchorPos(BlockPos.fromLong(roomNbt.getLong("anchorPos")));
            }
            room.setQuality(roomNbt.getInt("quality"));
            room.setValid(roomNbt.getBoolean("valid"));
            
            if (roomNbt.contains("tenantUuid")) {
                room.setTenant(
                    roomNbt.getUuid("tenantUuid"),
                    roomNbt.getString("tenantName"),
                    roomNbt.getString("tenantType")
                );
            }
            
            manager.rooms.put(signPos, room);
            if (room.getAnchorPos() != null) {
                manager.anchorToSign.put(room.getAnchorPos(), signPos);
            }
        }
        
        return manager;
    }
    
    private static final Type<RoomManager> TYPE = new Type<>(
        RoomManager::new,
        RoomManager::createFromNbt,
        null
    );
    
    public static RoomManager getServerState(MinecraftServer server) {
        PersistentStateManager stateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return stateManager.getOrCreate(TYPE, TownsfolkSignsMod.MOD_ID + "_rooms");
    }
    
    public static RoomManager getServerState(ServerWorld world) {
        return getServerState(world.getServer());
    }
}
