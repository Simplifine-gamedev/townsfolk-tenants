package com.townsfolk.signs;

import java.util.*;

public class RoomTypeRegistry {
    private static final Map<String, RoomType> TYPES = new LinkedHashMap<>();

    public static void register(String id, RoomType roomType) {
        if (TYPES.containsKey(id)) {
            throw new IllegalStateException("Room type '" + id + "' already registered by " + TYPES.get(id).ownerModId());
        }
        TYPES.put(id, roomType);
        TownsfolkSignsMod.LOGGER.info("Registered room type '{}' from mod '{}'", id, roomType.ownerModId());
    }

    public static Collection<RoomType> getAllTypes() {
        return Collections.unmodifiableCollection(TYPES.values());
    }

    public static RoomType getType(String id) {
        return TYPES.get(id);
    }
    
    public static boolean hasType(String id) {
        return TYPES.containsKey(id);
    }
    
    public static int getTypeCount() {
        return TYPES.size();
    }
}
