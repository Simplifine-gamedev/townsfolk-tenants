package com.townsfolk.signs.world;

import com.townsfolk.signs.RoomType;
import com.townsfolk.signs.TownsfolkSignsMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class RoomScanner {
    private static final int MAX_FLOOD_FILL = 500;
    
    public static class ScanResult {
        public boolean valid;
        public String invalidReason;
        public BlockPos anchorPos;
        public Set<BlockPos> roomVolume;
        
        public static ScanResult invalid(String reason) {
            ScanResult result = new ScanResult();
            result.valid = false;
            result.invalidReason = reason;
            return result;
        }
        
        public static ScanResult success(BlockPos anchorPos, Set<BlockPos> roomVolume) {
            ScanResult result = new ScanResult();
            result.valid = true;
            result.anchorPos = anchorPos;
            result.roomVolume = roomVolume;
            return result;
        }
    }
    
    public static ScanResult scanRoom(World world, BlockPos signPos, Direction scanDirection, RoomType roomType) {
        // The sign is on the OUTSIDE of the wall
        // scanDirection points INTO the building (opposite of sign facing)
        // signPos.offset(scanDirection) = the wall block the sign is attached to
        // signPos.offset(scanDirection, 2) = inside the room (past the wall)
        
        BlockPos wallPos = signPos.offset(scanDirection);
        BlockPos startPos = signPos.offset(scanDirection, 2);
        
        TownsfolkSignsMod.LOGGER.info("Scanning room: sign at {}, wall at {}, scanning {} into room at {}", 
            signPos, wallPos, scanDirection, startPos);
        TownsfolkSignsMod.LOGGER.info("Block at wall: {}, block at start: {}", 
            world.getBlockState(wallPos).getBlock(), 
            world.getBlockState(startPos).getBlock());
        
        if (roomType.requiresEnclosure()) {
            // First, flood fill from inside the room to find the room volume
            FloodFillResult floodResult = floodFillRoom(world, startPos);
            
            if (!floodResult.enclosed) {
                TownsfolkSignsMod.LOGGER.info("Room not enclosed (flood fill reached {} blocks, max {})", floodResult.volume.size(), MAX_FLOOD_FILL);
                return ScanResult.invalid("Room not enclosed (too large or has gaps)");
            }
            
            TownsfolkSignsMod.LOGGER.info("Found enclosed room with {} air blocks", floodResult.volume.size());
            
            // Now find an anchor block INSIDE the room volume
            BlockPos anchorPos = findAnchorInVolume(world, floodResult.volume, roomType);
            if (anchorPos == null) {
                TownsfolkSignsMod.LOGGER.info("No {} anchor block found INSIDE the room", roomType.displayName());
                return ScanResult.invalid("No " + roomType.displayName().toLowerCase() + " anchor (e.g., bed) found inside the room");
            }
            
            TownsfolkSignsMod.LOGGER.info("Found anchor block at {} inside room", anchorPos);
            return ScanResult.success(anchorPos, floodResult.volume);
        } else {
            // For non-enclosed rooms, just find nearest anchor
            BlockPos anchorPos = findNearestAnchor(world, startPos, roomType, 8);
            if (anchorPos == null) {
                return ScanResult.invalid("No " + roomType.displayName().toLowerCase() + " anchor block found nearby");
            }
            Set<BlockPos> volume = new HashSet<>();
            volume.add(anchorPos);
            return ScanResult.success(anchorPos, volume);
        }
    }
    
    /**
     * Find an anchor block that is inside or adjacent to the room volume
     */
    private static BlockPos findAnchorInVolume(World world, Set<BlockPos> roomVolume, RoomType roomType) {
        // Check all positions in the room volume and their immediate neighbors
        Set<BlockPos> checkPositions = new HashSet<>(roomVolume);
        
        // Also check blocks adjacent to the room (beds might be on the floor)
        for (BlockPos pos : roomVolume) {
            checkPositions.add(pos.down());
            for (Direction dir : Direction.Type.HORIZONTAL) {
                checkPositions.add(pos.offset(dir));
                checkPositions.add(pos.offset(dir).down());
            }
        }
        
        for (BlockPos checkPos : checkPositions) {
            Block block = world.getBlockState(checkPos).getBlock();
            if (roomType.isAnchorBlock(block)) {
                return checkPos;
            }
        }
        return null;
    }
    
    /**
     * Find nearest anchor within a small radius (for non-enclosed rooms)
     */
    private static BlockPos findNearestAnchor(World world, BlockPos startPos, RoomType roomType, int radius) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos checkPos = startPos.add(dx, dy, dz);
                    Block block = world.getBlockState(checkPos).getBlock();
                    if (roomType.isAnchorBlock(block)) {
                        double dist = startPos.getSquaredDistance(checkPos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = checkPos;
                        }
                    }
                }
            }
        }
        return nearest;
    }
    
    private static class FloodFillResult {
        boolean enclosed;
        Set<BlockPos> volume;
        
        FloodFillResult(boolean enclosed, Set<BlockPos> volume) {
            this.enclosed = enclosed;
            this.volume = volume;
        }
    }
    
    /**
     * Flood fill from a starting position to find the room volume.
     * Returns enclosed=true if the room is properly enclosed (didn't hit max blocks).
     */
    private static FloodFillResult floodFillRoom(World world, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        // Only start if the starting position is passable
        if (!canPassThrough(world, startPos)) {
            // Try one block up or down
            if (canPassThrough(world, startPos.up())) {
                startPos = startPos.up();
            } else if (canPassThrough(world, startPos.down())) {
                startPos = startPos.down();
            } else {
                return new FloodFillResult(false, visited);
            }
        }
        
        queue.add(startPos);
        visited.add(startPos);
        
        while (!queue.isEmpty() && visited.size() < MAX_FLOOD_FILL) {
            BlockPos current = queue.poll();
            
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);
                if (visited.contains(neighbor)) continue;
                
                if (canPassThrough(world, neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        boolean enclosed = visited.size() < MAX_FLOOD_FILL;
        return new FloodFillResult(enclosed, visited);
    }
    
    /**
     * Check if a position can be passed through (air, non-solid blocks, etc.)
     */
    private static boolean canPassThrough(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        
        if (state.isAir()) return true;
        
        if (state.getFluidState().isIn(FluidTags.WATER)) return true;
        
        if (state.isReplaceable()) return true;
        
        // Non-solid blocks like torches, flowers, etc.
        if (!state.isSolidBlock(world, pos)) {
            // But blocks with collision (like fences, glass panes) should block
            if (!state.getCollisionShape(world, pos).isEmpty()) {
                return false;
            }
            return true;
        }
        
        return false;
    }
}
