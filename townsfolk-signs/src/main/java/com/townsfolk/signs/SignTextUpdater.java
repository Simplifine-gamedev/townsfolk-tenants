package com.townsfolk.signs;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

public class SignTextUpdater {
    
    public enum RoomStatus {
        VACANT,     // Valid but empty - Yellow
        OCCUPIED,   // Has tenant - Green
        INVALID     // Invalid room - Red
    }
    
    public static void setRoomLabel(ServerWorld world, BlockPos signPos, String roomType, int roomNumber) {
        setRoomLabel(world, signPos, roomType, roomNumber, RoomStatus.VACANT);
    }
    
    public static void setRoomLabel(ServerWorld world, BlockPos signPos, String roomType, int roomNumber, RoomStatus status) {
        BlockEntity be = world.getBlockEntity(signPos);
        if (be instanceof SignBlockEntity sign) {
            DyeColor color = switch (status) {
                case OCCUPIED -> DyeColor.GREEN;
                case INVALID -> DyeColor.RED;
                case VACANT -> DyeColor.YELLOW;
            };
            
            String statusText = switch (status) {
                case OCCUPIED -> "Occupied";
                case INVALID -> "Invalid";
                case VACANT -> "Vacant";
            };
            
            SignText newText = sign.getFrontText()
                .withMessage(0, Text.literal(roomType))
                .withMessage(1, Text.literal("Room #" + roomNumber))
                .withMessage(2, Text.literal(statusText))
                .withMessage(3, Text.empty())
                .withColor(color);
            
            sign.setText(newText, true);
            sign.markDirty();
            
            world.updateListeners(signPos, world.getBlockState(signPos), world.getBlockState(signPos), 3);
        }
    }
    
    public static void updateRoomStatus(ServerWorld world, BlockPos signPos, String roomType, int roomNumber, boolean isValid, boolean hasTenant) {
        RoomStatus status;
        if (!isValid) {
            status = RoomStatus.INVALID;
        } else if (hasTenant) {
            status = RoomStatus.OCCUPIED;
        } else {
            status = RoomStatus.VACANT;
        }
        setRoomLabel(world, signPos, roomType, roomNumber, status);
    }
    
    public static void clearRoomLabel(ServerWorld world, BlockPos signPos) {
        BlockEntity be = world.getBlockEntity(signPos);
        if (be instanceof SignBlockEntity sign) {
            SignText newText = sign.getFrontText()
                .withMessage(0, Text.empty())
                .withMessage(1, Text.empty())
                .withMessage(2, Text.empty())
                .withMessage(3, Text.empty())
                .withColor(DyeColor.BLACK);
            
            sign.setText(newText, true);
            sign.markDirty();
            
            world.updateListeners(signPos, world.getBlockState(signPos), world.getBlockState(signPos), 3);
        }
    }
}
