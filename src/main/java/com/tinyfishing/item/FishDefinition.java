package com.tinyfishing.item;

public record FishDefinition(
    String id,
    String itemId,
    String displayName,
    String rarity,
    int xp,
    String codexEntryId
) {
}
