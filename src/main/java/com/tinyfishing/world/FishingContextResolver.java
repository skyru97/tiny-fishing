package com.tinyfishing.world;

import com.tinyfishing.fishing.FishingCastContext;
import com.tinyfishing.item.FishingRegionDefinition;
import com.tinyfishing.item.RodDefinition;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FishingContextResolver {
    private static final String WATER_FLUID_ID = "Water";

    private final Map<String, RodDefinition> rodsByItemId;
    private final Map<String, FishingRegionDefinition> regionsByEnvironmentId;
    private final List<FishingRegionDefinition> fishingRegions;

    public FishingContextResolver(List<RodDefinition> rods, List<FishingRegionDefinition> fishingRegions) {
        Map<String, RodDefinition> rodsByItemId = new java.util.HashMap<>();
        for (RodDefinition rod : rods) {
            rodsByItemId.put(rod.itemId(), rod);
        }

        Map<String, FishingRegionDefinition> regionsByEnvironmentId = new java.util.HashMap<>();
        for (FishingRegionDefinition region : fishingRegions) {
            for (String environmentId : region.environmentIds()) {
                regionsByEnvironmentId.put(environmentId, region);
            }
        }

        this.rodsByItemId = Map.copyOf(rodsByItemId);
        this.regionsByEnvironmentId = Map.copyOf(regionsByEnvironmentId);
        this.fishingRegions = List.copyOf(fishingRegions);
    }

    public Optional<FishingCastContext> resolveContext(Ref<EntityStore> playerEntityRef, String heldItemId, Vector3i initialTargetBlock) {
        Store<EntityStore> store = playerEntityRef.getStore();
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            return Optional.empty();
        }

        RodDefinition rod = resolveHeldRod(player, heldItemId);
        if (rod == null) {
            return Optional.empty();
        }

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return Optional.empty();
        }

        return resolveContextAtBlock(player, rod, transform, initialTargetBlock);
    }

    private RodDefinition resolveHeldRod(Player player, String heldItemId) {
        if (heldItemId != null) {
            RodDefinition eventRod = rodsByItemId.get(heldItemId);
            if (eventRod != null) {
                return eventRod;
            }
        }

        var inventory = player.getInventory();
        if (inventory == null) {
            return null;
        }

        RodDefinition activeToolRod = resolveRod(inventory.getActiveToolItem());
        if (activeToolRod != null) {
            return activeToolRod;
        }

        RodDefinition toolsItemRod = resolveRod(inventory.getToolsItem());
        if (toolsItemRod != null) {
            return toolsItemRod;
        }

        RodDefinition itemInHandRod = resolveRod(inventory.getItemInHand());
        if (itemInHandRod != null) {
            return itemInHandRod;
        }

        RodDefinition hotbarRod = resolveRod(inventory.getActiveHotbarItem());
        if (hotbarRod != null) {
            return hotbarRod;
        }

        RodDefinition scannedToolRod = scanContainerForRod(inventory.getTools());
        if (scannedToolRod != null) {
            return scannedToolRod;
        }

        return scanContainerForRod(inventory.getHotbar());
    }

    private RodDefinition resolveRod(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }

        return rodsByItemId.get(itemStack.getItemId());
    }

    private RodDefinition scanContainerForRod(ItemContainer itemContainer) {
        if (itemContainer == null) {
            return null;
        }

        for (short slot = 0; slot < itemContainer.getCapacity(); slot++) {
            RodDefinition rod = resolveRod(itemContainer.getItemStack(slot));
            if (rod != null) {
                return rod;
            }
        }

        return null;
    }

    private Optional<FishingCastContext> resolveContextAtBlock(
        Player player,
        RodDefinition rod,
        TransformComponent transform,
        Vector3i targetBlock
    ) {
        if (targetBlock == null) {
            return Optional.empty();
        }

        if (transform.getPosition().distanceTo(targetBlock) > rod.castRange()) {
            return Optional.empty();
        }

        WorldChunk chunk = player.getWorld().getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(targetBlock.getX(), targetBlock.getZ()));
        if (chunk == null) {
            return Optional.empty();
        }

        int localX = ChunkUtil.localCoordinate(targetBlock.getX());
        int localZ = ChunkUtil.localCoordinate(targetBlock.getZ());
        int fluidId = player.getWorld().getFluidId(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidId);
        String fluidKey = fluid == null ? null : fluid.getId();
        if (fluidKey == null || !fluidKey.startsWith(WATER_FLUID_ID)) {
            return Optional.empty();
        }

        int environmentIndex = chunk.getBlockChunk().getEnvironment(localX, targetBlock.getY(), localZ);
        Environment environment = Environment.getAssetMap().getAsset(environmentIndex);
        String environmentId = environment == null ? null : environment.getId();
        FishingRegionDefinition region = resolveRegion(environmentId);
        if (region == null) {
            return Optional.empty();
        }

        Vector3d targetPosition = new Vector3d(targetBlock).add(0.5, 0.78, 0.5);
        return Optional.of(new FishingCastContext(rod, region, targetBlock, targetPosition, environmentId));
    }

    private FishingRegionDefinition resolveRegion(String environmentId) {
        FishingRegionDefinition exactRegion = regionsByEnvironmentId.get(environmentId);
        if (exactRegion != null) {
            return exactRegion;
        }

        if (environmentId != null) {
            String normalizedEnvironmentId = environmentId.toLowerCase();
            for (FishingRegionDefinition region : fishingRegions) {
                String regionId = region.regionId().toLowerCase();
                int separatorIndex = regionId.indexOf('_');
                String zonePrefix = separatorIndex >= 0 ? regionId.substring(0, separatorIndex) : regionId;
                if (normalizedEnvironmentId.contains(zonePrefix)) {
                    return region;
                }
            }
        }

        return fishingRegions.isEmpty() ? null : fishingRegions.getFirst();
    }
}
