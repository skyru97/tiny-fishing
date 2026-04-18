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
        List<CatalogEntry> displayEntries = new ArrayList<>(entries);
        displayEntries.sort((left, right) -> Boolean.compare(data.hasDiscovered(right.codexEntryId()), data.hasDiscovered(left.codexEntryId())));

        uiCommandBuilder.append(PAGE_PATH);
        uiCommandBuilder.clear("#FishList");
        uiCommandBuilder.set("#Summary.Text", buildSummaryText(data));

        for (int index = 0; index < displayEntries.size(); index++) {
            CatalogEntry entry = displayEntries.get(index);
            boolean discovered = data.hasDiscovered(entry.codexEntryId());
            String selector = "#FishList[" + index + "]";

            uiCommandBuilder.append("#FishList", CARD_PATH);
            uiCommandBuilder.set(selector + " #Name.Text", discovered ? entry.displayName() : UNKNOWN_NAME);
            uiCommandBuilder.set(selector + " #Details.Visible", true);
            uiCommandBuilder.set(selector + " #UnknownPlate.Visible", !discovered);
            uiCommandBuilder.set(selector + " #Icon.Visible", discovered);
            uiCommandBuilder.set(selector + " #QuestionMark.Visible", !discovered);
            uiCommandBuilder.set(selector + " #AccentUnknown.Visible", !discovered);
            uiCommandBuilder.set(selector + " #AccentCommon.Visible", discovered && rarityMatches(entry.rarity(), "common"));
            uiCommandBuilder.set(selector + " #AccentUncommon.Visible", discovered && rarityMatches(entry.rarity(), "uncommon"));
            uiCommandBuilder.set(selector + " #AccentRare.Visible", discovered && rarityMatches(entry.rarity(), "rare"));

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

        List<CatalogEntry> orderedEntries = new ArrayList<>();
        for (FishDefinition fishDefinition : config.fishDefinitions()) {
            CodexEntryDefinition codexEntry = codexById.get(fishDefinition.codexEntryId());
            String displayName = codexEntry == null || !hasText(codexEntry.displayName()) ? fishDefinition.displayName() : codexEntry.displayName();
            String iconPath = codexEntry == null ? null : codexEntry.iconPath();
            orderedEntries.add(new CatalogEntry(fishDefinition.codexEntryId(), displayName, iconPath, fishDefinition.rarity()));
        }
        return List.copyOf(orderedEntries);
    }

    private boolean rarityMatches(String rarity, String expected) {
        return hasText(rarity) && rarity.equalsIgnoreCase(expected);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CatalogEntry(String codexEntryId, String displayName, String iconPath, String rarity) {
    }
}
