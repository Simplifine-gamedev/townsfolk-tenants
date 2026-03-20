package com.townsfolk.signs;

import com.townsfolk.signs.world.RoomData;
import com.townsfolk.signs.world.RoomManager;
import com.townsfolk.signs.world.RoomScanner;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallSignBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TownsfolkSignsMod {
    public static final String MOD_ID = "townsfolk";
    public static final Logger LOGGER = LoggerFactory.getLogger("townsfolk/signs");

    public static void init() {
        LOGGER.info("Townsfolk Signs initializing...");
        
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RoomManager.getServerState(server);
            LOGGER.info("Townsfolk Signs room manager initialized");
        });
        
        // Handle block breaking
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && world instanceof ServerWorld serverWorld) {
                RoomManager manager = RoomManager.getServerState(serverWorld);
                
                // Handle sign breaking
                if (state.getBlock() instanceof WallSignBlock) {
                    if (manager.hasRoom(pos)) {
                        RoomData removedRoom = manager.getRoom(pos);
                        String roomTypeId = removedRoom.getRoomTypeId();
                        
                        // Evict tenant if present
                        if (removedRoom.hasTenant()) {
                            removedRoom.clearTenant();
                            LOGGER.info("Evicted tenant from room #{} (sign broken)", removedRoom.getRoomNumber());
                        }
                        
                        manager.removeRoom(pos);
                        LOGGER.info("Room at {} unregistered (sign broken)", pos);
                        
                        // Renumber remaining rooms of this type
                        Map<Integer, Integer> renumberMap = manager.renumberRooms(roomTypeId);
                        
                        // Update signs for all renumbered rooms
                        if (!renumberMap.isEmpty()) {
                            RoomType roomType = RoomTypeRegistry.getType(roomTypeId);
                            String typeName = roomType != null ? roomType.displayName() : "Room";
                            
                            for (RoomData room : manager.getRoomsByType(roomTypeId)) {
                                SignTextUpdater.updateRoomStatus(
                                    serverWorld, 
                                    room.getSignPos(), 
                                    typeName, 
                                    room.getRoomNumber(), 
                                    room.isValid(), 
                                    room.hasTenant()
                                );
                            }
                            LOGGER.info("Renumbered {} rooms after removal", renumberMap.size());
                        }
                    }
                }
                
                // Handle bed breaking - invalidate any room using this bed as anchor
                if (state.getBlock() instanceof BedBlock) {
                    RoomData room = manager.getRoomByAnchor(pos);
                    if (room != null) {
                        LOGGER.info("Bed broken at {} - invalidating room #{}", pos, room.getRoomNumber());
                        
                        // Evict tenant
                        if (room.hasTenant()) {
                            room.clearTenant();
                            LOGGER.info("Evicted tenant from room #{} (bed broken)", room.getRoomNumber());
                        }
                        
                        // Mark room as invalid
                        room.setValid(false);
                        room.setAnchorPos(null);
                        manager.updateRoom(room);
                        
                        // Update sign to show invalid (red)
                        RoomType roomType = RoomTypeRegistry.getType(room.getRoomTypeId());
                        String typeName = roomType != null ? roomType.displayName() : "Room";
                        SignTextUpdater.updateRoomStatus(
                            serverWorld,
                            room.getSignPos(),
                            typeName,
                            room.getRoomNumber(),
                            false,
                            false
                        );
                    }
                }
            }
            return true;
        });
        
        // Handle bed placement - check if any invalid rooms can be re-validated
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }
            
            // Check if player is placing a bed
            var stack = player.getStackInHand(hand);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
                return ActionResult.PASS;
            }
            
            Block block = blockItem.getBlock();
            if (!(block instanceof BedBlock)) {
                return ActionResult.PASS;
            }
            
            // Schedule a check for next tick (after the bed is actually placed)
            serverWorld.getServer().execute(() -> {
                checkInvalidRoomsForNewBed(serverWorld);
            });
            
            return ActionResult.PASS;
        });
        
        LOGGER.info("Townsfolk Signs initialized! Right-click wall signs to declare rooms.");
    }
    
    /**
     * Check all invalid rooms to see if they now have a valid anchor (bed)
     */
    private static void checkInvalidRoomsForNewBed(ServerWorld world) {
        RoomManager manager = RoomManager.getServerState(world);
        
        for (RoomData room : manager.getAllRooms().values()) {
            if (room.isValid()) continue; // Skip already valid rooms
            if (room.getAnchorPos() != null) continue; // Skip rooms that still have an anchor
            
            // Get the room type to check for anchor
            RoomType roomType = RoomTypeRegistry.getType(room.getRoomTypeId());
            if (roomType == null) continue;
            
            // Get the sign's facing direction to scan the room
            BlockPos signPos = room.getSignPos();
            BlockState signState = world.getBlockState(signPos);
            
            if (!(signState.getBlock() instanceof WallSignBlock)) {
                continue; // Sign was removed
            }
            
            Direction signFacing = signState.get(Properties.HORIZONTAL_FACING);
            
            // Re-scan the room
            RoomScanner.ScanResult result = RoomScanner.scanRoom(world, signPos, signFacing, roomType);
            
            if (result.valid && result.anchorPos != null) {
                // Check if this anchor is already used by another room
                if (manager.isAnchorAssigned(result.anchorPos)) {
                    continue;
                }
                
                // Room is now valid!
                room.setValid(true);
                room.setAnchorPos(result.anchorPos);
                room.setRoomVolume(result.roomVolume);
                manager.updateRoom(room);
                
                // Update sign to show valid (yellow/vacant)
                String typeName = roomType.displayName();
                SignTextUpdater.updateRoomStatus(
                    world,
                    room.getSignPos(),
                    typeName,
                    room.getRoomNumber(),
                    true,
                    room.hasTenant()
                );
                
                LOGGER.info("Room #{} re-validated with new bed at {}", room.getRoomNumber(), result.anchorPos);
            }
        }
    }
}
