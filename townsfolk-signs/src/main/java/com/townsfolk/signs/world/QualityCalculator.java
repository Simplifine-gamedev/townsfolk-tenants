package com.townsfolk.signs.world;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

public class QualityCalculator {
    
    public static int calculateQuality(ServerWorld world, Set<BlockPos> roomVolume) {
        if (roomVolume == null || roomVolume.isEmpty()) {
            return 1;
        }
        
        int score = 0;
        int maxScore = 100;
        
        int size = roomVolume.size();
        if (size >= 50) score += 20;
        else if (size >= 30) score += 15;
        else if (size >= 15) score += 10;
        else if (size >= 8) score += 5;
        
        int lightLevel = 0;
        int lightCount = 0;
        for (BlockPos pos : roomVolume) {
            if (world.isAir(pos)) {
                lightLevel += world.getLightLevel(pos);
                lightCount++;
            }
        }
        if (lightCount > 0) {
            int avgLight = lightLevel / lightCount;
            if (avgLight >= 12) score += 15;
            else if (avgLight >= 8) score += 10;
            else if (avgLight >= 5) score += 5;
        }
        
        int decorations = 0;
        int furniture = 0;
        int windows = 0;
        
        for (BlockPos pos : roomVolume) {
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            
            if (block instanceof FlowerPotBlock || 
                block instanceof AbstractBannerBlock ||
                state.isIn(net.minecraft.registry.tag.BlockTags.FLOWER_POTS) ||
                block instanceof CarpetBlock) {
                decorations++;
            }
            
            if (block instanceof ChestBlock ||
                block instanceof BarrelBlock ||
                block instanceof CraftingTableBlock ||
                block == Blocks.BOOKSHELF ||
                block == Blocks.LECTERN ||
                block == Blocks.ENCHANTING_TABLE ||
                block == Blocks.BREWING_STAND ||
                block == Blocks.ANVIL ||
                block == Blocks.JUKEBOX) {
                furniture++;
            }
            
            if (block instanceof StainedGlassBlock ||
                block instanceof StainedGlassPaneBlock ||
                block == Blocks.GLASS ||
                block == Blocks.GLASS_PANE) {
                windows++;
            }
        }
        
        if (decorations >= 5) score += 15;
        else if (decorations >= 3) score += 10;
        else if (decorations >= 1) score += 5;
        
        if (furniture >= 5) score += 15;
        else if (furniture >= 3) score += 10;
        else if (furniture >= 1) score += 5;
        
        if (windows >= 4) score += 10;
        else if (windows >= 2) score += 5;
        
        int quality;
        if (score >= 60) quality = 5;
        else if (score >= 45) quality = 4;
        else if (score >= 30) quality = 3;
        else if (score >= 15) quality = 2;
        else quality = 1;
        
        return quality;
    }
}
