package com.townsfolk.signs;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;

import java.util.function.Predicate;

public record RoomType(
    String id,
    String displayName,
    Predicate<Block> anchorBlockPredicate,
    boolean requiresEnclosure,
    String ownerModId
) {
    public static RoomType ofBlock(String id, String displayName, Block anchorBlock, boolean requiresEnclosure, String ownerModId) {
        return new RoomType(id, displayName, block -> block == anchorBlock, requiresEnclosure, ownerModId);
    }
    
    public static RoomType ofTag(String id, String displayName, TagKey<Block> anchorTag, boolean requiresEnclosure, String ownerModId) {
        return new RoomType(id, displayName, block -> {
            // Check if the block is in the tag using registry lookup
            var entry = Registries.BLOCK.getEntry(block);
            return entry.isIn(anchorTag);
        }, requiresEnclosure, ownerModId);
    }
    
    public boolean isAnchorBlock(Block block) {
        return anchorBlockPredicate.test(block);
    }
}
