package com.tinyfishing.item;

public record WeightedFishingEntry(
    String targetId,
    String itemId,
    int weight
) {
    public WeightedFishingEntry {
        if (weight <= 0) {
            throw new IllegalArgumentException("Fishing entry weights must be positive.");
        }
        if (hasText(targetId) == hasText(itemId)) {
            throw new IllegalArgumentException("Fishing entries must define exactly one of targetId or itemId.");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
