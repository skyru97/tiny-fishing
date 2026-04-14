package com.tinyfishing.fishing;

import com.tinyfishing.component.FishingPlayerDataComponent;
import com.tinyfishing.item.CodexEntryDefinition;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CodexService {
    private final Set<String> entryIds;

    public CodexService(List<CodexEntryDefinition> entries) {
        Set<String> ids = new HashSet<>();
        for (CodexEntryDefinition entry : entries) {
            ids.add(entry.id());
        }
        this.entryIds = Set.copyOf(ids);
    }

    public boolean discover(FishingPlayerDataComponent data, String codexEntryId) {
        return codexEntryId != null && entryIds.contains(codexEntryId) && data.discover(codexEntryId);
    }

    public int getDiscoveredCount(FishingPlayerDataComponent data) {
        Set<String> discoveredEntryIds = new HashSet<>();
        for (String discoveredId : data.getDiscoveredFishIds()) {
            if (entryIds.contains(discoveredId)) {
                discoveredEntryIds.add(discoveredId);
            }
        }
        return discoveredEntryIds.size();
    }

    public int getTotalCount() {
        return entryIds.size();
    }
}
