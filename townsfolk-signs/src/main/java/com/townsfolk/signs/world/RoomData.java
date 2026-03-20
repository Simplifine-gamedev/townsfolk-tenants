package com.townsfolk.signs.world;

import net.minecraft.util.math.BlockPos;
import java.util.Set;
import java.util.UUID;

public class RoomData {
    private String roomTypeId;
    private int roomNumber;
    private BlockPos signPos;
    private BlockPos anchorPos;
    private Set<BlockPos> roomVolume;
    private int quality;
    private boolean valid;
    
    private UUID tenantUuid;
    private String tenantName;
    private String tenantType;
    private long spawnTenantAtTick;
    
    public RoomData(String roomTypeId, int roomNumber, BlockPos signPos) {
        this.roomTypeId = roomTypeId;
        this.roomNumber = roomNumber;
        this.signPos = signPos;
        this.valid = false;
    }
    
    public String getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(String roomTypeId) { this.roomTypeId = roomTypeId; }
    
    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }
    
    public BlockPos getSignPos() { return signPos; }
    public void setSignPos(BlockPos signPos) { this.signPos = signPos; }
    
    public BlockPos getAnchorPos() { return anchorPos; }
    public void setAnchorPos(BlockPos anchorPos) { this.anchorPos = anchorPos; }
    
    public Set<BlockPos> getRoomVolume() { return roomVolume; }
    public void setRoomVolume(Set<BlockPos> roomVolume) { this.roomVolume = roomVolume; }
    
    public int getQuality() { return quality; }
    public void setQuality(int quality) { this.quality = Math.max(1, Math.min(5, quality)); }
    
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public UUID getTenantUuid() { return tenantUuid; }
    public String getTenantName() { return tenantName; }
    public String getTenantType() { return tenantType; }
    
    public boolean hasTenant() { return tenantUuid != null; }
    
    public void setTenant(UUID uuid, String name, String type) {
        this.tenantUuid = uuid;
        this.tenantName = name;
        this.tenantType = type;
    }
    
    public void clearTenant() {
        this.tenantUuid = null;
        this.tenantName = null;
        this.tenantType = null;
    }
    
    public long getSpawnTenantAtTick() { return spawnTenantAtTick; }
    public void setSpawnTenantAtTick(long tick) { this.spawnTenantAtTick = tick; }
    public void clearSpawnSchedule() { this.spawnTenantAtTick = 0; }
    
    public String getTierName() {
        return switch (quality) {
            case 1 -> "Budget";
            case 2 -> "Standard";
            case 3 -> "Comfort";
            case 4 -> "Luxury";
            case 5 -> "Presidential";
            default -> "Unknown";
        };
    }
}
