package com.tinyfishing.ui;

import com.tinyfishing.component.FishingPlayerDataComponent;
import com.tinyfishing.config.TinyFishingConfig;
import com.tinyfishing.item.CodexEntryDefinition;
import com.tinyfishing.item.FishDefinition;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FishingCodexPage extends BasicCustomUIPage {
    private static final String PAGE_PATH = "TinyFishing/FishingCodexPage.ui";
    private static final String CARD_PATH = "TinyFishing/FishingCodexFishCard.ui";
    private static final String UNKNOWN_NAME = "Unknown";

    private final Ref<EntityStore> playerEntityRef;
    private final Store<EntityStore> store;
    private final ComponentType<EntityStore, FishingPlayerDataComponent> dataType;
    private final List<CatalogEntry> entries;

    public FishingCodexPage(
        PlayerRef playerRef,
        Ref<EntityStore> playerEntityRef,
        Store<EntityStore> store,
        ComponentType<EntityStore, FishingPlayerDataComponent> dataType,
        TinyFishingConfig config
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.playerEntityRef = playerEntityRef;
        this.store = store;
        this.dataType = dataType;
        this.entries = buildEntries(config);
    }

    @Override
    public void build(UICommandBuilder uiCommandBuilder) {
        FishingPlayerDataComponent data = store.ensureAndGetComponent(playerEntityRef, dataType);

        uiCommandBuilder.append(PAGE_PATH);
        uiCommandBuilder.clear("#FishList");
        uiCommandBuilder.set("#Summary.Text", buildSummaryText(data));

        for (int index = 0; index < entries.size(); index++) {
            CatalogEntry entry = entries.get(index);
            boolean discovered = data.hasDiscovered(entry.codexEntryId());
            String selector = "#FishList[" + index + "]";

            uiCommandBuilder.append("#FishList", CARD_PATH);
            uiCommandBuilder.set(selector + " #Name.Text", discovered ? entry.displayName() : UNKNOWN_NAME);
            uiCommandBuilder.set(selector + " #Details.Visible", true);
            uiCommandBuilder.set(selector + " #UnknownPlate.Visible", !discovered);
            uiCommandBuilder.set(selector + " #Icon.Visible", discovered);
            uiCommandBuilder.set(selector + " #QuestionMark.Visible", !discovered);
            String bestQuality = discovered ? normalizeQuality(data.getBestCatchQuality(entry.codexEntryId()), entry.rarity()) : null;
            uiCommandBuilder.set(selector + " #BorderUnknown.Visible", !discovered);
            uiCommandBuilder.set(selector + " #BorderCommon.Visible", discovered && qualityMatches(bestQuality, "common"));
            uiCommandBuilder.set(selector + " #BorderUncommon.Visible", discovered && qualityMatches(bestQuality, "uncommon"));
            uiCommandBuilder.set(selector + " #BorderRare.Visible", discovered && qualityMatches(bestQuality, "rare"));
            uiCommandBuilder.set(selector + " #BorderEpic.Visible", discovered && qualityMatches(bestQuality, "epic"));
            uiCommandBuilder.set(selector + " #BorderLegendary.Visible", discovered && qualityMatches(bestQuality, "legendary"));

            if (discovered && hasText(entry.iconPath())) {
                uiCommandBuilder.set(selector + " #Icon.AssetPath", entry.iconPath());
            } else {
                uiCommandBuilder.setNull(selector + " #Icon.AssetPath");
            }
        }
    }

    private String buildSummaryText(FishingPlayerDataComponent data) {
        int discovered = 0;
        for (CatalogEntry entry : entries) {
            if (data.hasDiscovered(entry.codexEntryId())) {
                discovered++;
            }
        }
        return discovered + " / " + entries.size();
    }

    private List<CatalogEntry> buildEntries(TinyFishingConfig config) {
        Map<String, CodexEntryDefinition> codexById = new LinkedHashMap<>();
        for (CodexEntryDefinition codexEntry : config.codexEntries()) {
            codexById.put(codexEntry.id(), codexEntry);
        }

        Map<String, FishDefinition> fishById = new LinkedHashMap<>();
        for (FishDefinition fishDefinition : config.fishDefinitions()) {
            fishById.put(fishDefinition.id(), fishDefinition);
        }

        Map<String, CatalogEntry> orderedEntries = new LinkedHashMap<>();
        for (var region : config.fishingRegions()) {
            for (var fishEntry : region.fishEntries()) {
                FishDefinition fishDefinition = fishById.get(fishEntry.targetId());
                if (fishDefinition == null) {
                    continue;
                }
                orderedEntries.putIfAbsent(fishDefinition.codexEntryId(), toCatalogEntry(fishDefinition, codexById));
            }
        }

        for (FishDefinition fishDefinition : config.fishDefinitions()) {
            orderedEntries.putIfAbsent(fishDefinition.codexEntryId(), toCatalogEntry(fishDefinition, codexById));
        }

        return List.copyOf(orderedEntries.values());
    }

    private CatalogEntry toCatalogEntry(FishDefinition fishDefinition, Map<String, CodexEntryDefinition> codexById) {
        CodexEntryDefinition codexEntry = codexById.get(fishDefinition.codexEntryId());
        String displayName = codexEntry == null || !hasText(codexEntry.displayName()) ? fishDefinition.displayName() : codexEntry.displayName();
        String iconPath = codexEntry == null ? null : codexEntry.iconPath();
        return new CatalogEntry(fishDefinition.codexEntryId(), displayName, iconPath, fishDefinition.rarity());
    }

    private boolean qualityMatches(String quality, String expected) {
        return hasText(quality) && quality.equalsIgnoreCase(expected);
    }

    private String normalizeQuality(String quality, String fallback) {
        if (hasText(quality)) {
            return quality.toLowerCase();
        }
        return hasText(fallback) ? fallback.toLowerCase() : "common";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CatalogEntry(String codexEntryId, String displayName, String iconPath, String rarity) {
    }
}
