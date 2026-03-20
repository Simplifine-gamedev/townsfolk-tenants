package com.townsfolk.signs;

import com.townsfolk.signs.world.QualityCalculator;
import com.townsfolk.signs.world.RoomData;
import com.townsfolk.signs.world.RoomManager;
import com.townsfolk.signs.world.RoomScanner;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Collection;

public class TownsfolkSignHandler {
    
    public static void handleSignInteraction(PlayerEntity player, BlockPos signPos, Direction signFacing) {
        if (player.getWorld().isClient) return;
        
        ServerWorld world = (ServerWorld) player.getWorld();
        RoomManager manager = RoomManager.getServerState(world);
        
        if (manager.hasRoom(signPos)) {
            showRoomInfo(player, manager.getRoom(signPos));
        } else {
            tryAssignRoom(player, world, signPos, signFacing, manager);
        }
    }
    
    private static void showRoomInfo(PlayerEntity player, RoomData room) {
        RoomType type = RoomTypeRegistry.getType(room.getRoomTypeId());
        String typeName = type != null ? type.displayName() : room.getRoomTypeId();
        
        player.sendMessage(Text.literal("=== " + typeName + " Room #" + room.getRoomNumber() + " ===").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("Quality: " + room.getQuality() + "/5 (" + room.getTierName() + ")").formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("Status: " + (room.isValid() ? "Valid" : "Invalid")).formatted(room.isValid() ? Formatting.GREEN : Formatting.RED), false);
        
        if (room.hasTenant()) {
            player.sendMessage(Text.literal("Tenant: " + room.getTenantName()).formatted(Formatting.AQUA), false);
        } else {
            player.sendMessage(Text.literal("Tenant: Vacant").formatted(Formatting.GRAY), false);
        }
        
        player.sendMessage(Text.literal("(Break sign to unregister room)").formatted(Formatting.DARK_GRAY), false);
    }
    
    private static void tryAssignRoom(PlayerEntity player, ServerWorld world, BlockPos signPos, Direction signFacing, RoomManager manager) {
        Collection<RoomType> types = RoomTypeRegistry.getAllTypes();
        
        if (types.isEmpty()) {
            player.sendMessage(Text.literal("No room types registered. Install a Townsfolk building mod!").formatted(Formatting.RED), true);
            return;
        }
        
        if (types.size() == 1) {
            RoomType type = types.iterator().next();
            assignRoomType(player, world, signPos, signFacing, manager, type);
        } else {
            player.sendMessage(Text.literal("=== Select Room Type ===").formatted(Formatting.GOLD), false);
            int index = 1;
            for (RoomType type : types) {
                player.sendMessage(Text.literal(index + ". " + type.displayName() + " (from " + type.ownerModId() + ")").formatted(Formatting.YELLOW), false);
                index++;
            }
            player.sendMessage(Text.literal("Right-click again to cycle through types, or sneak+click to edit sign text.").formatted(Formatting.GRAY), false);
            
            RoomType firstType = types.iterator().next();
            assignRoomType(player, world, signPos, signFacing, manager, firstType);
        }
    }
    
    private static void assignRoomType(PlayerEntity player, ServerWorld world, BlockPos signPos, Direction signFacing, RoomManager manager, RoomType roomType) {
        // Wall sign's HORIZONTAL_FACING points outward from the wall
        // If the sign is on the OUTSIDE of a building, the room is BEHIND the sign
        // So we need to scan in the OPPOSITE direction (through the wall, into the room)
        Direction scanDirection = signFacing.getOpposite();
        
        // The scan starts from 2 blocks in (to get past the wall the sign is on)
        RoomScanner.ScanResult result = RoomScanner.scanRoom(world, signPos, scanDirection, roomType);
        
        if (!result.valid) {
            player.sendMessage(Text.literal("Invalid room: " + result.invalidReason).formatted(Formatting.RED), true);
            return;
        }
        
        if (manager.isAnchorAssigned(result.anchorPos)) {
            RoomData existingRoom = manager.getRoomByAnchor(result.anchorPos);
            player.sendMessage(Text.literal("This anchor is already assigned to Room #" + existingRoom.getRoomNumber()).formatted(Formatting.RED), true);
            return;
        }
        
        int roomNumber = manager.getNextRoomNumber();
        RoomData room = new RoomData(roomType.id(), roomNumber, signPos);
        room.setAnchorPos(result.anchorPos);
        room.setRoomVolume(result.roomVolume);
        room.setValid(true);
        
        int quality = QualityCalculator.calculateQuality(world, result.roomVolume);
        room.setQuality(quality);
        
        manager.registerRoom(room);
        
        SignTextUpdater.setRoomLabel(world, signPos, roomType.displayName(), roomNumber, SignTextUpdater.RoomStatus.VACANT);
        
        player.sendMessage(Text.literal(roomType.displayName() + " Room #" + roomNumber + " registered! Quality: " + quality + "/5 (" + room.getTierName() + ")").formatted(Formatting.GREEN), true);
        
        TownsfolkSignsMod.LOGGER.info("{} Room #{} registered at {} with quality {}", roomType.displayName(), roomNumber, signPos, quality);
    }
}
