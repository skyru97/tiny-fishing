package com.tinyfishing.config;

import com.tinyfishing.item.CodexEntryDefinition;
import com.tinyfishing.item.FishDefinition;
import com.tinyfishing.item.FishingRegionDefinition;
import com.tinyfishing.item.RodDefinition;
import com.tinyfishing.item.WeightedFishingEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TinyFishingConfigValidator {
    public void validate(TinyFishingConfig config) {
        require(!config.rods().isEmpty(), "Tiny Fishing requires at least one rod definition.");
        require(!config.fishDefinitions().isEmpty(), "Tiny Fishing requires at least one fish definition.");
        require(!config.codexEntries().isEmpty(), "Tiny Fishing requires at least one codex entry.");
        require(!config.fishingRegions().isEmpty(), "Tiny Fishing requires at least one fishing region.");

        validateRods(config.rods());

        Set<String> codexEntryIds = validateCodexEntries(config.codexEntries());
        Set<String> fishIds = validateFishDefinitions(config.fishDefinitions(), codexEntryIds);
        validateRegions(config.fishingRegions(), fishIds);
    }

    private void validateRods(List<RodDefinition> rods) {
        Set<String> rodIds = new HashSet<>();
        Set<String> itemIds = new HashSet<>();

        for (RodDefinition rod : rods) {
            require(hasText(rod.id()), "Rod ids must be non-empty.");
            require(hasText(rod.displayName()), "Rod '" + rod.id() + "' must have a displayName.");
            require(hasText(rod.itemId()), "Rod '" + rod.id() + "' must have an itemId.");
            require(rod.castRange() > 0.0, "Rod '" + rod.id() + "' must have a positive castRange.");
            require(rod.biteWindowSeconds() > 0.0, "Rod '" + rod.id() + "' must have a positive biteWindowSeconds.");
            require(rod.minBiteDelaySeconds() >= 0.0, "Rod '" + rod.id() + "' must have a non-negative minBiteDelaySeconds.");
            require(
                rod.maxBiteDelaySeconds() >= rod.minBiteDelaySeconds(),
                "Rod '" + rod.id() + "' must have maxBiteDelaySeconds >= minBiteDelaySeconds."
            );
            require(rodIds.add(rod.id()), "Duplicate rod id: " + rod.id());
            require(itemIds.add(rod.itemId()), "Duplicate rod itemId: " + rod.itemId());
        }
    }

    private Set<String> validateCodexEntries(List<CodexEntryDefinition> entries) {
        Set<String> entryIds = new HashSet<>();

        for (CodexEntryDefinition entry : entries) {
            require(hasText(entry.id()), "Codex entry ids must be non-empty.");
            require(hasText(entry.displayName()), "Codex entry '" + entry.id() + "' must have a displayName.");
            require(hasText(entry.iconPath()), "Codex entry '" + entry.id() + "' must have an iconPath.");
            require(hasText(entry.hint()), "Codex entry '" + entry.id() + "' must have a hint.");
            require(entryIds.add(entry.id()), "Duplicate codex entry id: " + entry.id());
        }

        return entryIds;
    }

    private Set<String> validateFishDefinitions(List<FishDefinition> fishDefinitions, Set<String> codexEntryIds) {
        Set<String> fishIds = new HashSet<>();

        for (FishDefinition fishDefinition : fishDefinitions) {
            require(hasText(fishDefinition.id()), "Fish ids must be non-empty.");
            require(hasText(fishDefinition.itemId()), "Fish '" + fishDefinition.id() + "' must have an itemId.");
            require(hasText(fishDefinition.displayName()), "Fish '" + fishDefinition.id() + "' must have a displayName.");
            require(hasText(fishDefinition.rarity()), "Fish '" + fishDefinition.id() + "' must have a rarity.");
            require(fishDefinition.xp() > 0, "Fish '" + fishDefinition.id() + "' must award positive XP.");
            require(hasText(fishDefinition.codexEntryId()), "Fish '" + fishDefinition.id() + "' must have a codexEntryId.");
            require(codexEntryIds.contains(fishDefinition.codexEntryId()), "Fish '" + fishDefinition.id() + "' references unknown codexEntryId '" + fishDefinition.codexEntryId() + "'.");
            require(fishIds.add(fishDefinition.id()), "Duplicate fish id: " + fishDefinition.id());
        }

        return fishIds;
    }

    private void validateRegions(List<FishingRegionDefinition> fishingRegions, Set<String> fishIds) {
        Set<String> regionIds = new HashSet<>();
        Map<String, String> regionIdsByEnvironmentId = new HashMap<>();

        for (FishingRegionDefinition region : fishingRegions) {
            require(hasText(region.regionId()), "Fishing region ids must be non-empty.");
            require(hasText(region.displayName()), "Fishing region '" + region.regionId() + "' must have a displayName.");
            require(regionIds.add(region.regionId()), "Duplicate fishing region id: " + region.regionId());
            require(!region.environmentIds().isEmpty(), "Fishing region '" + region.regionId() + "' must define at least one environment id.");

            for (String environmentId : region.environmentIds()) {
                require(hasText(environmentId), "Fishing region '" + region.regionId() + "' contains a blank environment id.");
                String previousRegionId = regionIdsByEnvironmentId.putIfAbsent(environmentId, region.regionId());
                require(
                    previousRegionId == null,
                    "Environment id '" + environmentId + "' is mapped to both region '" + previousRegionId + "' and region '" + region.regionId() + "'."
                );
            }

            validateFishEntries(region, fishIds);
            validateItemEntries(region.regionId(), "trash", region.trashEntries());
            validateItemEntries(region.regionId(), "prize", region.prizeEntries());
        }

    }

    private void validateFishEntries(FishingRegionDefinition region, Set<String> fishIds) {
        require(!region.fishEntries().isEmpty(), "Fishing region '" + region.regionId() + "' must define at least one fish entry.");

        for (WeightedFishingEntry entry : region.fishEntries()) {
            require(
                entry.targetId() != null && entry.itemId() == null,
                "Fish entries in region '" + region.regionId() + "' must use targetId only."
            );
            require(
                fishIds.contains(entry.targetId()),
                "Fishing region '" + region.regionId() + "' references unknown fish id '" + entry.targetId() + "'."
            );
        }
    }

    private void validateItemEntries(String regionId, String tableName, List<WeightedFishingEntry> entries) {
        require(!entries.isEmpty(), "Fishing region '" + regionId + "' must define at least one " + tableName + " entry.");

        for (WeightedFishingEntry entry : entries) {
            require(
                entry.itemId() != null && entry.targetId() == null,
                "" + tableName + " entries in region '" + regionId + "' must use itemId only."
            );
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
