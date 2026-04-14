package com.tinyfishing.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Arrays;
import java.util.LinkedHashSet;

public final class FishingPlayerDataComponent implements Component<EntityStore> {
    private static final KeyedCodec<String[]> DISCOVERED = new KeyedCodec<>(
        "DiscoveredFishIds",
        new ArrayCodec<>(Codec.STRING, String[]::new)
    );

    public static final BuilderCodec<FishingPlayerDataComponent> CODEC = BuilderCodec.builder(
        FishingPlayerDataComponent.class,
        FishingPlayerDataComponent::new
    )
        .append(DISCOVERED, FishingPlayerDataComponent::setDiscoveredFishIds, FishingPlayerDataComponent::getDiscoveredFishIds).add()
        .build();

    private String[] discoveredFishIds = new String[0];

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

    @Override
    public FishingPlayerDataComponent clone() {
        FishingPlayerDataComponent copy = new FishingPlayerDataComponent();
        copy.discoveredFishIds = Arrays.copyOf(discoveredFishIds, discoveredFishIds.length);
        return copy;
    }
}
