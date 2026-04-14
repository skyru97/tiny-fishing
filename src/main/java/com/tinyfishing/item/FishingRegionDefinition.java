package com.tinyfishing.item;

import java.util.List;

public record FishingRegionDefinition(
    String regionId,
    String displayName,
    List<String> environmentIds,
    List<WeightedFishingEntry> fishEntries,
    List<WeightedFishingEntry> trashEntries,
    List<WeightedFishingEntry> prizeEntries
) {
    public FishingRegionDefinition {
        environmentIds = List.copyOf(environmentIds);
        fishEntries = List.copyOf(fishEntries);
        trashEntries = List.copyOf(trashEntries);
        prizeEntries = List.copyOf(prizeEntries);
    }
}
