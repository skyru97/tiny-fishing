package com.tinyfishing.config;

import com.tinyfishing.item.CodexEntryDefinition;
import com.tinyfishing.item.FishDefinition;
import com.tinyfishing.item.FishingRegionDefinition;
import com.tinyfishing.item.RodDefinition;
import java.util.List;

public record TinyFishingConfig(
    List<RodDefinition> rods,
    List<FishDefinition> fishDefinitions,
    List<CodexEntryDefinition> codexEntries,
    List<FishingRegionDefinition> fishingRegions
) {
    public TinyFishingConfig {
        rods = List.copyOf(rods);
        fishDefinitions = List.copyOf(fishDefinitions);
        codexEntries = List.copyOf(codexEntries);
        fishingRegions = List.copyOf(fishingRegions);
    }
}
