package com.tinyfishing.fishing;

import com.tinyfishing.item.CatchReward;
import com.tinyfishing.item.FishDefinition;
import com.tinyfishing.item.FishingRegionDefinition;
import com.tinyfishing.item.WeightedFishingEntry;
import com.tinyfishing.loot.CatchType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class LootTableService {
    private static final Set<String> VANILLA_FISH_QUALITY_STATES = Set.of("Uncommon", "Rare", "Epic", "Legendary");
    private final Map<String, FishDefinition> fishById;
    private final Map<String, FishingRegionDefinition> regionsById;
    private final List<String> regionIds;
    private final List<String> prizeItems;
    private final Random random = new Random();

    public LootTableService(
        List<FishDefinition> fishDefinitions,
        List<FishingRegionDefinition> regions,
        List<String> prizeItems
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
        this.prizeItems = List.copyOf(prizeItems);
    }

    public CatchReward rollReward(String regionId, CatchType catchType) {
        return rollReward(regionId, catchType, random);
    }

    public CatchReward rollReward(String regionId, CatchType catchType, Random randomSource) {
        requireKnownRegion(regionId);
        FishingRegionDefinition region = regionsById.get(regionId);

        return switch (catchType) {
            case TRASH -> buildItemReward(region, region.trashEntries(), catchType, randomSource);
            case PRIZE -> buildPrizeReward(region, randomSource);
            case FISH -> buildFishReward(region, randomSource);
        };
    }

    private CatchReward buildPrizeReward(FishingRegionDefinition region, Random randomSource) {
        String itemId = prizeItems.get(randomSource.nextInt(prizeItems.size()));
        return new CatchReward(CatchType.PRIZE, itemId, null, null, resolveItemDisplayName(itemId), region.regionId(), null);
    }

    private CatchReward buildItemReward(FishingRegionDefinition region, List<WeightedFishingEntry> entries, CatchType catchType, Random randomSource) {
        WeightedFishingEntry entry = choose(entries, randomSource);
        String displayName = entry.itemId() == null ? catchType.name() : resolveItemDisplayName(entry.itemId());
        return new CatchReward(catchType, entry.itemId(), null, null, displayName, region.regionId(), null);
    }

    private CatchReward buildFishReward(FishingRegionDefinition region, Random randomSource) {
        WeightedFishingEntry chosen = choose(region.fishEntries(), randomSource);
        FishDefinition fishDefinition = fishById.get(chosen.targetId());
        if (fishDefinition == null) {
            throw new IllegalStateException("Unknown fish id: " + chosen.targetId());
        }

        FishCatchProfile catchProfile = resolveFishCatchProfile(fishDefinition, randomSource);
        return new CatchReward(CatchType.FISH, catchProfile.itemId(), catchProfile.state(), catchProfile.fishId(), catchProfile.displayName(), region.regionId(), catchProfile.quality());
    }

    private FishCatchProfile resolveFishCatchProfile(FishDefinition fishDefinition, Random randomSource) {
        Item item = resolveItemAsset(fishDefinition.itemId());
        if (item == null) {
            return new FishCatchProfile(fishDefinition.itemId(), null, fishDefinition.id(), fishDefinition.displayName(), normalizeQuality(fishDefinition.rarity()));
        }

        String state = rollVanillaFishState(item, randomSource);
        if (state == null) {
            return new FishCatchProfile(fishDefinition.itemId(), null, fishDefinition.id(), fishDefinition.displayName(), normalizeQuality(fishDefinition.rarity()));
        }

        return new FishCatchProfile(fishDefinition.itemId(), state, fishDefinition.id(), fishDefinition.displayName(), state.toLowerCase());
    }

    private String rollVanillaFishState(Item item, Random randomSource) {
        Set<String> availableStates = resolveAvailableStates(item);
        if (availableStates.isEmpty()) {
            return null;
        }

        List<WeightedState> weightedStates = new ArrayList<>();
        int commonWeight = 840;
        if (availableStates.contains("Uncommon")) {
            weightedStates.add(new WeightedState("Uncommon", 120));
        }
        if (availableStates.contains("Rare")) {
            weightedStates.add(new WeightedState("Rare", 30));
        }
        if (availableStates.contains("Epic")) {
            weightedStates.add(new WeightedState("Epic", 8));
        }
        if (availableStates.contains("Legendary")) {
            weightedStates.add(new WeightedState("Legendary", 2));
        }

        int totalWeight = commonWeight;
        for (WeightedState weightedState : weightedStates) {
            totalWeight += weightedState.weight();
        }

        int roll = randomSource.nextInt(totalWeight);
        if (roll < commonWeight) {
            return null;
        }

        int running = commonWeight;
        for (WeightedState weightedState : weightedStates) {
            running += weightedState.weight();
            if (roll < running) {
                return weightedState.state();
            }
        }

        return null;
    }

    private Set<String> resolveAvailableStates(Item item) {
        Set<String> availableStates = new HashSet<>();
        for (String state : VANILLA_FISH_QUALITY_STATES) {
            if (item.getItemForState(state) != null) {
                availableStates.add(state);
            }
        }
        return Set.copyOf(availableStates);
    }

    private Item resolveItemAsset(String itemId) {
        try {
            if (Item.getAssetStore() == null) {
                return null;
            }
            return Item.getAssetMap().getAsset(itemId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String normalizeQuality(String quality) {
        return quality == null ? "common" : quality.toLowerCase();
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

    private String resolveItemDisplayName(String itemId) {
        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null || item.getTranslationProperties() == null || item.getTranslationProperties().getName() == null) {
            return itemId;
        }

        String translationKey = item.getTranslationProperties().getName();
        String translated = I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, translationKey);
        return translated == null || translated.isBlank() ? itemId : translated;
    }

    private record FishCatchProfile(String itemId, String state, String fishId, String displayName, String quality) {
    }

    private record WeightedState(String state, int weight) {
    }
}
