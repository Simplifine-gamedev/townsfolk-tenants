package com.townsfolk.tenants.entity;

import com.townsfolk.signs.world.RoomData;
import com.townsfolk.signs.world.RoomManager;
import com.townsfolk.tenants.TownsfolkTenantsMod;
import com.townsfolk.tenants.block.TipChestBlock;
import com.townsfolk.tenants.block.entity.TipChestBlockEntity;
import com.townsfolk.tenants.world.TenantSpawner;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;

import java.util.List;

public class TenantEntity extends VillagerEntity {
    private static final int TIP_CHEST_SEARCH_RADIUS = 64;
    
    private BlockPos signPos = null;
    private BlockPos homePos = null;
    private long lastPaymentDay = -1;
    private int satisfactionCheckCounter = 0;
    private int satisfaction = 50;
    private int daysOccupied = 0;
    
    public TenantEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
        this.setVillagerData(this.getVillagerData()
            .withType(VillagerType.PLAINS)
            .withProfession(VillagerProfession.NITWIT)
            .withLevel(1));
        this.setPersistent();
    }
    
    @Override
    public boolean cannotDespawn() {
        return true;
    }
    
    @Override
    public boolean isPersistent() {
        return true;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!getWorld().isClient && homePos != null && signPos != null) {
            long currentDay = getWorld().getTimeOfDay() / 24000;
            int timeOfDay = (int) (getWorld().getTimeOfDay() % 24000);
            
            // Pay rent in the morning (dawn) - go to tip chest
            if (timeOfDay >= 0 && timeOfDay < 1000 && currentDay != lastPaymentDay) {
                tryPayRent();
                lastPaymentDay = currentDay;
            }
            
            // Update satisfaction every 10 seconds
            satisfactionCheckCounter++;
            if (satisfactionCheckCounter >= 200) {
                satisfactionCheckCounter = 0;
                updateSatisfaction();
            }
            
            // Teleport back if too far from home
            double distToHome = squaredDistanceTo(homePos.getX(), homePos.getY(), homePos.getZ());
            if (distToHome > 2500) { // 50 blocks
                refreshPositionAndAngles(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5, getYaw(), getPitch());
            }
            
            checkForLeaving();
        }
    }
    
    private void tryPayRent() {
        if (!(getWorld() instanceof ServerWorld serverWorld)) return;
        if (signPos == null) return;
        
        RoomManager manager = RoomManager.getServerState(serverWorld);
        RoomData room = manager.getRoom(signPos);
        if (room == null) return;
        
        // Find nearest tip chest
        BlockPos tipChestPos = findNearestTipChest(serverWorld);
        if (tipChestPos == null) {
            TownsfolkTenantsMod.LOGGER.debug("Tenant in room {} couldn't find tip chest", room.getRoomNumber());
            satisfaction = Math.max(0, satisfaction - 3);
            return;
        }
        
        if (serverWorld.getBlockEntity(tipChestPos) instanceof TipChestBlockEntity tipChest) {
            int payment = getDailyPayment(room.getQuality());
            
            // Bonus tip for high satisfaction
            if (satisfaction >= 80) {
                payment = (int) (payment * 1.25);
            }
            
            boolean success = tipChest.insertPayment(payment);
            if (!success) {
                satisfaction = Math.max(0, satisfaction - 5);
            }
            
            daysOccupied++;
            
            TownsfolkTenantsMod.LOGGER.debug("Tenant paid {} emeralds for room #{}", payment, room.getRoomNumber());
        }
    }
    
    /**
     * Force the tenant to pay rent immediately. Called by /hotel payday command.
     * @return The amount paid, or 0 if payment failed
     */
    public int forcePayRent() {
        if (!(getWorld() instanceof ServerWorld serverWorld)) return 0;
        if (signPos == null) return 0;
        
        RoomManager manager = RoomManager.getServerState(serverWorld);
        RoomData room = manager.getRoom(signPos);
        if (room == null) return 0;
        
        // Find nearest tip chest
        BlockPos tipChestPos = findNearestTipChest(serverWorld);
        if (tipChestPos == null) {
            TownsfolkTenantsMod.LOGGER.info("Tenant {} couldn't find tip chest for payday", getCustomName() != null ? getCustomName().getString() : "Unknown");
            return 0;
        }
        
        if (serverWorld.getBlockEntity(tipChestPos) instanceof TipChestBlockEntity tipChest) {
            int payment = getDailyPayment(room.getQuality());
            
            // Bonus tip for high satisfaction
            if (satisfaction >= 80) {
                payment = (int) (payment * 1.25);
            }
            
            boolean success = tipChest.insertPayment(payment);
            if (success) {
                TownsfolkTenantsMod.LOGGER.info("Tenant {} paid {} emeralds (payday)", 
                    getCustomName() != null ? getCustomName().getString() : "Unknown", payment);
                return payment;
            }
        }
        
        return 0;
    }
    
    private BlockPos findNearestTipChest(ServerWorld world) {
        BlockPos nearestChest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        BlockPos center = homePos != null ? homePos : getBlockPos();
        
        for (int x = -TIP_CHEST_SEARCH_RADIUS; x <= TIP_CHEST_SEARCH_RADIUS; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -TIP_CHEST_SEARCH_RADIUS; z <= TIP_CHEST_SEARCH_RADIUS; z++) {
                    BlockPos checkPos = center.add(x, y, z);
                    if (world.getBlockState(checkPos).getBlock() instanceof TipChestBlock) {
                        double distance = center.getSquaredDistance(checkPos);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestChest = checkPos;
                        }
                    }
                }
            }
        }
        
        return nearestChest;
    }
    
    private void updateSatisfaction() {
        if (!(getWorld() instanceof ServerWorld serverWorld)) return;
        
        RoomManager manager = RoomManager.getServerState(serverWorld);
        RoomData room = manager.getRoom(signPos);
        if (room == null) return;
        
        int quality = room.getQuality();
        
        // High quality rooms increase satisfaction (quality is 1-5)
        if (quality >= 4) {
            satisfaction = Math.min(100, satisfaction + (quality - 3));
        }
        
        // Invalid room decreases satisfaction
        if (!room.isValid()) {
            satisfaction = Math.max(0, satisfaction - 10);
        }
        
        // Hostile mobs nearby decrease satisfaction
        Box roomBox = new Box(
            homePos.getX() - 8, homePos.getY() - 4, homePos.getZ() - 8,
            homePos.getX() + 8, homePos.getY() + 4, homePos.getZ() + 8
        );
        List<HostileEntity> hostiles = serverWorld.getEntitiesByClass(HostileEntity.class, roomBox, e -> true);
        if (!hostiles.isEmpty()) {
            satisfaction = Math.max(0, satisfaction - 15);
        }
        
        // Getting rained on decreases satisfaction
        if (serverWorld.isRaining() && serverWorld.isSkyVisible(getBlockPos())) {
            satisfaction = Math.max(0, satisfaction - 8);
        }
    }
    
    private void checkForLeaving() {
        if (!(getWorld() instanceof ServerWorld serverWorld)) return;
        
        RoomManager manager = RoomManager.getServerState(serverWorld);
        RoomData room = manager.getRoom(signPos);
        if (room == null) {
            this.discard();
            return;
        }
        
        // Leave immediately if satisfaction hits 0
        if (satisfaction <= 0) {
            TownsfolkTenantsMod.LOGGER.info("Tenant leaving room #{} due to 0 satisfaction", room.getRoomNumber());
            room.clearTenant();
            manager.updateRoom(room);
            TenantSpawner.updateSignColor(serverWorld, room);
            this.discard();
        } 
        // Leave after 2 days if satisfaction stays below 20
        else if (satisfaction <= 20 && daysOccupied > 2) {
            TownsfolkTenantsMod.LOGGER.info("Tenant leaving room #{} due to low satisfaction", room.getRoomNumber());
            room.clearTenant();
            manager.updateRoom(room);
            TenantSpawner.updateSignColor(serverWorld, room);
            this.discard();
        }
    }
    
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient) {
            if (!(getWorld() instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }
            
            RoomManager manager = RoomManager.getServerState(serverWorld);
            RoomData room = signPos != null ? manager.getRoom(signPos) : null;
            
            if (room != null) {
                String tierName = getTierName(room.getQuality());
                int payment = getDailyPayment(room.getQuality());
                
                player.sendMessage(Text.literal("=== " + getName().getString() + " ===").formatted(Formatting.GOLD), false);
                player.sendMessage(Text.literal("Room #" + room.getRoomNumber() + " (" + tierName + ")").formatted(Formatting.AQUA), false);
                player.sendMessage(Text.literal("Satisfaction: " + satisfaction + "%").formatted(
                    satisfaction >= 80 ? Formatting.GREEN : 
                    satisfaction >= 50 ? Formatting.YELLOW : Formatting.RED), false);
                player.sendMessage(Text.literal("Days stayed: " + daysOccupied).formatted(Formatting.GRAY), false);
                player.sendMessage(Text.literal("Daily rate: " + payment + " emeralds").formatted(Formatting.GREEN), false);
            } else {
                player.sendMessage(Text.literal("Hello! I'm looking for a room.").formatted(Formatting.YELLOW), false);
            }
            
            return ActionResult.SUCCESS;
        }
        return ActionResult.success(getWorld().isClient);
    }
    
    public static String getTierName(int quality) {
        return switch (quality) {
            case 1 -> "Budget";
            case 2 -> "Standard";
            case 3 -> "Comfort";
            case 4 -> "Luxury";
            case 5 -> "Presidential";
            default -> "Unknown";
        };
    }
    
    public static int getDailyPayment(int quality) {
        return switch (quality) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            case 4 -> 7;
            case 5 -> 12;
            default -> 1;
        };
    }
    
    public BlockPos getSignPos() { return signPos; }
    public void setSignPos(BlockPos pos) { this.signPos = pos; }
    
    public BlockPos getHomePos() { return homePos; }
    public void setHomePos(BlockPos pos) { this.homePos = pos; }
    
    public int getSatisfaction() { return satisfaction; }
    public int getDaysOccupied() { return daysOccupied; }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (signPos != null) {
            nbt.putLong("tenantSignPos", signPos.asLong());
        }
        if (homePos != null) {
            nbt.putLong("tenantHomePos", homePos.asLong());
        }
        nbt.putLong("tenantLastPaymentDay", lastPaymentDay);
        nbt.putInt("tenantSatisfaction", satisfaction);
        nbt.putInt("tenantDaysOccupied", daysOccupied);
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("tenantSignPos")) {
            signPos = BlockPos.fromLong(nbt.getLong("tenantSignPos"));
        }
        if (nbt.contains("tenantHomePos")) {
            homePos = BlockPos.fromLong(nbt.getLong("tenantHomePos"));
        }
        lastPaymentDay = nbt.getLong("tenantLastPaymentDay");
        satisfaction = nbt.getInt("tenantSatisfaction");
        daysOccupied = nbt.getInt("tenantDaysOccupied");
    }
}
