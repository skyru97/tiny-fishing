package com.tinyfishing.fishing;

import com.tinyfishing.item.CatchReward;
import com.tinyfishing.item.FishDefinition;
import com.tinyfishing.item.FishingRegionDefinition;
import com.tinyfishing.item.WeightedFishingEntry;
import com.tinyfishing.loot.CatchType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class LootTableService {
    private final Map<String, FishDefinition> fishById;
    private final Map<String, FishingRegionDefinition> regionsById;
    private final List<String> regionIds;
    private final Random random = new Random();

    public LootTableService(
        List<FishDefinition> fishDefinitions,
        List<FishingRegionDefinition> regions
    ) {
        Map<String, FishDefinition> fishById = new HashMap<>();
        for (FishDefinition fishDefinition : fishDefinitions) {
            fishById.put(fishDefinition.id(), fishDefinition);
        }

        Map<String, FishingRegionDefinition> regionsById = new HashMap<>();
        for (FishingRegionDefinition region : regions) {
            regionsById.put(region.regionId(), region);
        }

        this.fishById = Map.copyOf(fishById);
        this.regionsById = Map.copyOf(regionsById);
        this.regionIds = List.copyOf(this.regionsById.keySet().stream().sorted().toList());
    }

    public CatchReward rollReward(String regionId, CatchType catchType) {
        return rollReward(regionId, catchType, random);
    }

    public CatchReward rollReward(String regionId, CatchType catchType, Random randomSource) {
        requireKnownRegion(regionId);
        FishingRegionDefinition region = regionsById.get(regionId);

        return switch (catchType) {
            case TRASH -> buildItemReward(region, region.trashEntries(), catchType, randomSource);
            case PRIZE -> buildItemReward(region, region.prizeEntries(), catchType, randomSource);
            case FISH -> buildFishReward(region, randomSource);
        };
    }

    private CatchReward buildItemReward(FishingRegionDefinition region, List<WeightedFishingEntry> entries, CatchType catchType, Random randomSource) {
        WeightedFishingEntry entry = choose(entries, randomSource);
        String displayName = entry.itemId() == null ? catchType.name() : entry.itemId();
        return new CatchReward(catchType, entry.itemId(), null, null, displayName, region.regionId());
    }

    private CatchReward buildFishReward(FishingRegionDefinition region, Random randomSource) {
        WeightedFishingEntry chosen = choose(region.fishEntries(), randomSource);
        FishDefinition fishDefinition = fishById.get(chosen.targetId());
        if (fishDefinition == null) {
            throw new IllegalStateException("Unknown fish id: " + chosen.targetId());
        }

        return new CatchReward(
            CatchType.FISH,
            fishDefinition.itemId(),
            null,
            fishDefinition.id(),
            fishDefinition.displayName(),
            region.regionId()
        );
    }

    private WeightedFishingEntry choose(List<WeightedFishingEntry> entries, Random randomSource) {
        if (entries.isEmpty()) {
            throw new IllegalStateException("Cannot choose from empty fishing entries");
        }

        int totalWeight = 0;
        for (WeightedFishingEntry entry : entries) {
            totalWeight += entry.weight();
        }

        int roll = randomSource.nextInt(totalWeight);
        int running = 0;
        for (WeightedFishingEntry entry : entries) {
            running += entry.weight();
            if (roll < running) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }

    private void requireKnownRegion(String regionId) {
        if (!regionIds.contains(regionId)) {
            throw new IllegalStateException("Unknown fishing region: " + regionId);
        }
    }
}
