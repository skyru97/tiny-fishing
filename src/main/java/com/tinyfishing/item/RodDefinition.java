package com.tinyfishing.item;

import java.util.List;

public record RodDefinition(
    String id,
    String displayName,
    String itemId,
    double castRange,
    double biteWindowSeconds,
    double minBiteDelaySeconds,
    double maxBiteDelaySeconds,
    List<String> tags
) {
    public RodDefinition {
        tags = List.copyOf(tags);
    }
}
