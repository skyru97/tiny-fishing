package com.tinyfishing.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public final class FishingPlayerDataComponent implements Component<EntityStore> {
    private static final KeyedCodec<String[]> DISCOVERED = new KeyedCodec<>(
        "DiscoveredFishIds",
        new ArrayCodec<>(Codec.STRING, String[]::new)
    );
    private static final KeyedCodec<String[]> BEST_QUALITIES = new KeyedCodec<>(
        "BestCatchQualities",
        new ArrayCodec<>(Codec.STRING, String[]::new)
    );

    public static final BuilderCodec<FishingPlayerDataComponent> CODEC = BuilderCodec.builder(
        FishingPlayerDataComponent.class,
        FishingPlayerDataComponent::new
    )
        .append(DISCOVERED, FishingPlayerDataComponent::setDiscoveredFishIds, FishingPlayerDataComponent::getDiscoveredFishIds).add()
        .append(BEST_QUALITIES, FishingPlayerDataComponent::setBestCatchQualities, FishingPlayerDataComponent::getBestCatchQualities).add()
        .build();

    private String[] discoveredFishIds = new String[0];
    private String[] bestCatchQualities = new String[0];

    public FishingPlayerDataComponent() {
    }

    public String[] getDiscoveredFishIds() {
        return Arrays.copyOf(discoveredFishIds, discoveredFishIds.length);
    }

    public void setDiscoveredFishIds(String[] discoveredFishIds) {
        if (discoveredFishIds == null || discoveredFishIds.length == 0) {
            this.discoveredFishIds = new String[0];
            return;
        }

        LinkedHashSet<String> canonicalIds = new LinkedHashSet<>();
        for (String discoveredFishId : discoveredFishIds) {
            if (discoveredFishId != null && !discoveredFishId.isBlank()) {
                canonicalIds.add(discoveredFishId);
            }
        }
        this.discoveredFishIds = canonicalIds.toArray(String[]::new);
    }

    public boolean hasDiscovered(String fishId) {
        if (fishId == null || fishId.isBlank()) {
            return false;
        }
        for (String discoveredFishId : discoveredFishIds) {
            if (discoveredFishId.equals(fishId)) {
                return true;
            }
        }
        return false;
    }

    public boolean discover(String fishId) {
        if (fishId == null || fishId.isBlank() || hasDiscovered(fishId)) {
            return false;
        }

        String[] next = Arrays.copyOf(discoveredFishIds, discoveredFishIds.length + 1);
        next[next.length - 1] = fishId;
        discoveredFishIds = next;
        return true;
    }

    public String[] getBestCatchQualities() {
        return Arrays.copyOf(bestCatchQualities, bestCatchQualities.length);
    }

    public void setBestCatchQualities(String[] bestCatchQualities) {
        if (bestCatchQualities == null || bestCatchQualities.length == 0) {
            this.bestCatchQualities = new String[0];
            return;
        }

        LinkedHashMap<String, String> canonicalQualities = new LinkedHashMap<>();
        for (String entry : bestCatchQualities) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            int separatorIndex = entry.indexOf('=');
            if (separatorIndex <= 0 || separatorIndex >= entry.length() - 1) {
                continue;
            }
            String fishId = entry.substring(0, separatorIndex).trim();
            String quality = entry.substring(separatorIndex + 1).trim();
            if (!fishId.isBlank() && !quality.isBlank()) {
                canonicalQualities.put(fishId, quality);
            }
        }
        this.bestCatchQualities = canonicalQualities.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }

    public String getBestCatchQuality(String fishId) {
        if (fishId == null || fishId.isBlank()) {
            return null;
        }
        for (String entry : bestCatchQualities) {
            int separatorIndex = entry.indexOf('=');
            if (separatorIndex <= 0 || separatorIndex >= entry.length() - 1) {
                continue;
            }
            if (fishId.equals(entry.substring(0, separatorIndex))) {
                return entry.substring(separatorIndex + 1);
            }
        }
        return null;
    }

    public boolean recordBestCatchQuality(String fishId, String quality) {
        if (fishId == null || fishId.isBlank() || quality == null || quality.isBlank()) {
            return false;
        }

        Map<String, String> qualitiesByFishId = new LinkedHashMap<>();
        for (String entry : bestCatchQualities) {
            int separatorIndex = entry.indexOf('=');
            if (separatorIndex <= 0 || separatorIndex >= entry.length() - 1) {
                continue;
            }
            qualitiesByFishId.put(entry.substring(0, separatorIndex), entry.substring(separatorIndex + 1));
        }

        String currentQuality = qualitiesByFishId.get(fishId);
        if (currentQuality != null && compareQualityRank(currentQuality, quality) >= 0) {
            return false;
        }

        qualitiesByFishId.put(fishId, quality);
        bestCatchQualities = qualitiesByFishId.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
        return true;
    }

    private static int compareQualityRank(String left, String right) {
        return Integer.compare(qualityRank(left), qualityRank(right));
    }

    private static int qualityRank(String quality) {
        if (quality == null) {
            return -1;
        }
        return switch (quality.toLowerCase()) {
            case "common" -> 0;
            case "uncommon" -> 1;
            case "rare" -> 2;
            case "epic" -> 3;
            case "legendary" -> 4;
            default -> -1;
        };
    }

    @Override
    public FishingPlayerDataComponent clone() {
        FishingPlayerDataComponent copy = new FishingPlayerDataComponent();
        copy.discoveredFishIds = Arrays.copyOf(discoveredFishIds, discoveredFishIds.length);
        copy.bestCatchQualities = Arrays.copyOf(bestCatchQualities, bestCatchQualities.length);
        return copy;
    }
}
