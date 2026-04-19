package com.tinyfishing.fishing;

import com.tinyfishing.component.FishingBobberComponent;
import com.tinyfishing.component.FishingPlayerDataComponent;
import com.tinyfishing.config.TinyFishingConfig;
import com.tinyfishing.item.CatchReward;
import com.tinyfishing.item.CodexEntryDefinition;
import com.tinyfishing.item.FishDefinition;
import com.tinyfishing.loot.CatchType;
import com.tinyfishing.world.FishingContextResolver;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.SoundCategory;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class FishingService {
    private static final String CAST_SPLASH_PARTICLE_ID = "Water_Can_Splash";
    private static final String BOBBER_ITEM_ID = "TinyFishing_Bobber_Item";
    private static final double BOBBER_WATERLINE_OFFSET = 0.18;
    private static final float MAX_BOBBER_SINK_DEPTH = 0.45f;
    private static final float BOBBER_PICKUP_DELAY_SECONDS = 60.0f;
    private static final float LOOT_VISUAL_TRAVEL_SECONDS = 0.45f;
    private static final float LOOT_VISUAL_SCALE = 1.75f;
    private static final float BOBBER_CAST_SOUND_VOLUME = 0.78f;
    private static final int TRASH_WEIGHT = 20;
    private static final int FISH_WEIGHT = 70;
    private static final int PRIZE_WEIGHT = 10;
    private static final String BOBBER_CAST_SOUND_EVENT_ID = "SFX_TinyFishing_BiteSplash";
    private static final String[] BITE_SOUND_EVENT_IDS = {"SFX_TinyFishing_BiteSplash", "SFX_Water_MoveIn", "SFX_Water_MoveOut", "SFX_Tool_Watering_Can_Water"};
    private static final String[] DISCOVERY_SOUND_EVENT_IDS = {"SFX_Discovery_Z1_Medium", "SFX_Discovery_Z1_Short", "SFX_Discovery_Z4_Medium"};
    private static final String NEW_FISH_DISCOVERY_TITLE = "New Fish Discovered";
    private static final String NEW_QUALITY_DISCOVERY_TITLE = "New Best Quality";
    private final HytaleLogger logger;
    private final ComponentType<EntityStore, FishingBobberComponent> bobberType;
    private final ComponentType<EntityStore, FishingPlayerDataComponent> dataType;
    private final CodexService codexService;
    private final FishingContextResolver contextResolver;
    private final LootTableService lootTableService;
    private final Set<String> configuredRodItemIds;
    private final Map<String, String> codexEntryIdsByFishId;
    private final Map<String, CodexEntryDefinition> codexEntriesById;
    private final Map<UUID, FishingSession> sessionsByPlayerId = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> biteTasksByPlayerId = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> expiryTasksByPlayerId = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new SchedulerThreadFactory());

    public FishingService(
        HytaleLogger logger,
        ComponentType<EntityStore, FishingBobberComponent> bobberType,
        ComponentType<EntityStore, FishingPlayerDataComponent> dataType,
        TinyFishingConfig config,
        CodexService codexService,
        FishingContextResolver contextResolver,
        LootTableService lootTableService
    ) {
        this.logger = logger;
        this.bobberType = bobberType;
        this.dataType = dataType;
        this.codexService = codexService;
        this.contextResolver = contextResolver;
        this.lootTableService = lootTableService;
        this.configuredRodItemIds = buildConfiguredRodItemIds(config);
        this.codexEntryIdsByFishId = buildCodexEntryIdsByFishId(config);
        this.codexEntriesById = buildCodexEntriesById(config);
    }

    public void shutdown() {
        biteTasksByPlayerId.values().forEach(task -> task.cancel(false));
        expiryTasksByPlayerId.values().forEach(task -> task.cancel(false));
        sessionsByPlayerId.values().forEach(this::cleanupSessionVisuals);
        biteTasksByPlayerId.clear();
        expiryTasksByPlayerId.clear();
        sessionsByPlayerId.clear();
        scheduler.shutdownNow();
    }

    public void handleCastInteraction(
        Ref<EntityStore> playerEntityRef,
        String heldItemId,
        Vector3i targetBlock,
        Vector3d preciseTargetPosition
    ) {
        Store<EntityStore> store = playerEntityRef.getStore();
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

        FishingSession session = sessionsByPlayerId.get(playerRef.getUuid());
        if (session == null) {
            handleInteractionCast(playerEntityRef, heldItemId, targetBlock, preciseTargetPosition, player, playerRef, store);
            return;
        }

        triggerBiteIfNeeded(store, player, playerRef, session);
        if (session.getState() == FishingSessionState.WAITING_FOR_BITE) {
            return;
        }

        if (session.isInTensionWindow(Instant.now())) {
            completeCatch(playerEntityRef, player, playerRef, session, store);
            return;
        }

        clearSession(store, session);
    }

    private void handleInteractionCast(
        Ref<EntityStore> playerEntityRef,
        String heldItemId,
        Vector3i targetBlock,
        Vector3d preciseTargetPosition,
        Player player,
        PlayerRef playerRef,
        Store<EntityStore> store
    ) {
        Optional<FishingCastContext> context = contextResolver.resolveContext(playerEntityRef, heldItemId, targetBlock, preciseTargetPosition);
        if (context.isEmpty()) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        cleanupOrphanedBobbers(store);
        clearSession(store, sessionsByPlayerId.get(playerId));

        FishingCastContext castContext = context.get();
        Instant now = Instant.now();
        double biteDelaySeconds = castContext.rod().minBiteDelaySeconds()
            + (random.nextDouble() * (castContext.rod().maxBiteDelaySeconds() - castContext.rod().minBiteDelaySeconds()));
        double tensionWindowSeconds = castContext.rod().biteWindowSeconds();
        FishingSession session = new FishingSession(
            playerId,
            playerEntityRef,
            castContext,
            now.plusMillis((long) (biteDelaySeconds * 1000.0)),
            now.plusMillis((long) ((biteDelaySeconds + tensionWindowSeconds) * 1000.0))
        );

        spawnBobber(store, session);
        sessionsByPlayerId.put(playerId, session);
        scheduleBite(store, session);
        scheduleExpiry(store, session);
    }

    public boolean hasActiveSession(UUID playerUuid) {
        return sessionsByPlayerId.containsKey(playerUuid);
    }

    public Vector3i resolveTargetWater(com.hypixel.hytale.component.CommandBuffer<EntityStore> commandBuffer, World world, Ref<EntityStore> playerEntityRef, float distance) {
        return TargetUtil.getTargetBlock(
            world,
            (blockId, fluidId) -> {
                var fluid = com.hypixel.hytale.server.core.asset.type.fluid.Fluid.getAssetMap().getAsset(fluidId);
                return fluid != null && fluid.getId() != null && fluid.getId().startsWith("Water");
            },
            com.hypixel.hytale.server.core.util.TargetUtil.getLook(playerEntityRef, commandBuffer).getPosition().x,
            com.hypixel.hytale.server.core.util.TargetUtil.getLook(playerEntityRef, commandBuffer).getPosition().y,
            com.hypixel.hytale.server.core.util.TargetUtil.getLook(playerEntityRef, commandBuffer).getPosition().z,
            com.hypixel.hytale.server.core.util.TargetUtil.getLook(playerEntityRef, commandBuffer).getDirection().x,
            com.hypixel.hytale.server.core.util.TargetUtil.getLook(playerEntityRef, commandBuffer).getDirection().y,
            com.hypixel.hytale.server.core.util.TargetUtil.getLook(playerEntityRef, commandBuffer).getDirection().z,
            distance
        );
    }

    public void forceBite(Ref<EntityStore> playerEntityRef) {
        FishingSession session = getSession(playerEntityRef);
        if (session != null) {
            Store<EntityStore> store = playerEntityRef.getStore();
            Player player = store.getComponent(playerEntityRef, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
            if (player != null && playerRef != null) {
                activateBite(store, player, playerRef, session);
            }
        }
    }

    public FishingSession getSession(Ref<EntityStore> playerRef) {
        UUID playerId = resolvePlayerId(playerRef);
        return playerId == null ? null : sessionsByPlayerId.get(playerId);
    }

    public void clearSessionForPlayer(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }

        FishingSession session = sessionsByPlayerId.get(playerRef.getUuid());
        if (session != null && session.getPlayerEntityRef().isValid()) {
            Store<EntityStore> store = session.getPlayerEntityRef().getStore();
            runOnWorld(store, session, () -> clearSession(store, session));
            return;
        }

        cancelTask(biteTasksByPlayerId.remove(playerRef.getUuid()));
        cancelTask(expiryTasksByPlayerId.remove(playerRef.getUuid()));
        if (session != null) {
            sessionsByPlayerId.remove(playerRef.getUuid());
            session.setState(FishingSessionState.EXPIRED);
        }
    }

    public void cleanupOrphanedBobbers(Store<EntityStore> store) {
        if (store == null) {
            return;
        }

        Set<Ref<EntityStore>> activeBobberRefs = new HashSet<>();
        for (FishingSession session : sessionsByPlayerId.values()) {
            if (session.getBobberRef() != null && session.getBobberRef().isValid()) {
                activeBobberRefs.add(session.getBobberRef());
            }
        }

        store.forEachChunk((java.util.function.BiConsumer<com.hypixel.hytale.component.ArchetypeChunk<EntityStore>, com.hypixel.hytale.component.CommandBuffer<EntityStore>>) (chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                ItemComponent itemComponent = chunk.getComponent(entityIndex, ItemComponent.getComponentType());
                if (itemComponent == null || itemComponent.getItemStack() == null) {
                    continue;
                }

                if (!BOBBER_ITEM_ID.equals(itemComponent.getItemStack().getItemId())) {
                    continue;
                }

                Ref<EntityStore> bobberRef = chunk.getReferenceTo(entityIndex);
                if (activeBobberRefs.contains(bobberRef)) {
                    continue;
                }

                commandBuffer.tryRemoveEntity(bobberRef, RemoveReason.REMOVE);
            }
        });
    }

    private CatchType rollCatchType() {
        int totalWeight = TRASH_WEIGHT + FISH_WEIGHT + PRIZE_WEIGHT;
        int roll = random.nextInt(totalWeight);
        if (roll < TRASH_WEIGHT) {
            return CatchType.TRASH;
        }
        if (roll < TRASH_WEIGHT + FISH_WEIGHT) {
            return CatchType.FISH;
        }
        return CatchType.PRIZE;
    }

    private void triggerBiteIfNeeded(Store<EntityStore> store, Player player, PlayerRef playerRef, FishingSession session) {
        if (!session.shouldTriggerBite(Instant.now())) {
            return;
        }

        activateBite(store, player, playerRef, session);
    }

    private void activateBite(Store<EntityStore> store, Player player, PlayerRef playerRef, FishingSession session) {
        if (session.getState() != FishingSessionState.WAITING_FOR_BITE) {
            return;
        }

        session.markBiteTriggered();
        cancelTask(biteTasksByPlayerId.remove(session.getPlayerUuid()));
        syncBobberToSession(store, session);
    }

    private void scheduleBite(Store<EntityStore> store, FishingSession session) {
        UUID playerId = session.getPlayerUuid();
        cancelTask(biteTasksByPlayerId.remove(playerId));
        long delayMillis = Math.max(0L, session.getBiteAt().toEpochMilli() - Instant.now().toEpochMilli());
        ScheduledFuture<?> future = scheduler.schedule(() -> runOnWorld(store, session, () -> {
            FishingSession current = sessionsByPlayerId.get(playerId);
            if (current != session) {
                return;
            }
            Player player = store.getComponent(session.getPlayerEntityRef(), Player.getComponentType());
            PlayerRef playerRef = store.getComponent(session.getPlayerEntityRef(), PlayerRef.getComponentType());
            if (player == null || playerRef == null) {
                clearSession(store, session);
                return;
            }
            triggerBiteIfNeeded(store, player, playerRef, session);
        }), delayMillis, TimeUnit.MILLISECONDS);
        biteTasksByPlayerId.put(playerId, future);
    }

    private void scheduleExpiry(Store<EntityStore> store, FishingSession session) {
        UUID playerId = session.getPlayerUuid();
        cancelTask(expiryTasksByPlayerId.remove(playerId));
        long delayMillis = Math.max(0L, session.getReelWindowEndsAt().toEpochMilli() - Instant.now().toEpochMilli());
        ScheduledFuture<?> future = scheduler.schedule(() -> runOnWorld(store, session, () -> {
            FishingSession current = sessionsByPlayerId.get(playerId);
            if (current != session) {
                return;
            }
            clearSession(store, session);
        }), delayMillis, TimeUnit.MILLISECONDS);
        expiryTasksByPlayerId.put(playerId, future);
    }

    private void runOnWorld(Store<EntityStore> store, FishingSession session, Runnable action) {
        try {
            World world = store.getExternalData().getWorld();
            world.execute(action);
        } catch (Throwable throwable) {
            expireSessionWithoutWorldAccess(session);
            logger.at(Level.WARNING).withCause(throwable).log("Scheduled fishing task failed for %s", session.getPlayerEntityRef());
        }
    }

    private void spawnBobber(Store<EntityStore> store, FishingSession session) {
        try {
            Vector3d bobberPosition = getBobberPosition(session);
            var holder = ItemComponent.generateItemDrop(
                store,
                new ItemStack(BOBBER_ITEM_ID, 1),
                bobberPosition,
                Vector3f.ZERO,
                0f,
                0f,
                0f
            );
            if (holder == null) {
                logger.at(Level.WARNING).log("Failed to create bobber item-drop holder");
                return;
            }

            var itemComponent = holder.getComponent(ItemComponent.getComponentType());
            if (itemComponent != null) {
                itemComponent.setPickupDelay(BOBBER_PICKUP_DELAY_SECONDS);
            }
            holder.addComponent(bobberType, new FishingBobberComponent(bobberPosition, session.getPlayerEntityRef()));
            session.setBobberRef(store.addEntity(holder, AddReason.SPAWN));
            ParticleUtil.spawnParticleEffect(CAST_SPLASH_PARTICLE_ID, bobberPosition, store);
            playBobberCastSound(store, bobberPosition);
        } catch (Throwable throwable) {
            logger.at(Level.WARNING).withCause(throwable).log("Failed to spawn bobber visual");
        }
    }

    private void clearSession(Store<EntityStore> store, FishingSession session) {
        if (session == null) {
            return;
        }
        UUID playerId = session.getPlayerUuid();
        sessionsByPlayerId.remove(playerId, session);
        cancelTask(biteTasksByPlayerId.remove(playerId));
        cancelTask(expiryTasksByPlayerId.remove(playerId));
        setBobberBiting(store, session, false);
        removeBobber(store, session);
        session.setState(FishingSessionState.EXPIRED);
    }

    private void cleanupSessionVisuals(FishingSession session) {
        if (session == null) {
            return;
        }

        try {
            Store<EntityStore> store = session.getPlayerEntityRef().getStore();
            runOnWorld(store, session, () -> removeBobber(store, session));
        } catch (Throwable throwable) {
            logger.at(Level.WARNING).withCause(throwable).log("Failed to clean up fishing session visuals for %s", session.getPlayerEntityRef());
        }
        session.setState(FishingSessionState.EXPIRED);
    }

    private void removeBobber(Store<EntityStore> store, FishingSession session) {
        if (session.getBobberRef() != null && session.getBobberRef().isValid()) {
            store.removeEntity(session.getBobberRef(), RemoveReason.REMOVE);
            session.setBobberRef(null);
        }
    }

    private void setBobberBiting(Store<EntityStore> store, FishingSession session, boolean biting) {
        if (session.getBobberRef() == null || !session.getBobberRef().isValid()) {
            return;
        }

        FishingBobberComponent bobber = store.getComponent(session.getBobberRef(), bobberType);
        if (bobber == null) {
            return;
        }

        bobber.setBiting(biting);
        if (!biting) {
            bobber.setBiteAnimationTime(0.0f);
            bobber.setSplashCooldown(0.0f);
            bobber.setSinkDepth(0.0f);
        }
        store.putComponent(session.getBobberRef(), bobberType, bobber);
    }

    private void syncBobberToSession(Store<EntityStore> store, FishingSession session) {
        if (session.getBobberRef() == null || !session.getBobberRef().isValid()) {
            return;
        }

        FishingBobberComponent bobber = store.getComponent(session.getBobberRef(), bobberType);
        if (bobber == null) {
            return;
        }

        bobber.setBiting(session.getState() == FishingSessionState.TENSION_ACTIVE);
        bobber.setSinkDepth(session.getState() == FishingSessionState.TENSION_ACTIVE ? MAX_BOBBER_SINK_DEPTH * 0.6f : 0.0f);
        store.putComponent(session.getBobberRef(), bobberType, bobber);
    }

    private void giveRewardWithVisual(
        Ref<EntityStore> playerEntityRef,
        Player player,
        CatchReward reward,
        Vector3d startPosition,
        Store<EntityStore> store
    ) {
        ItemStack rewardStack = new ItemStack(reward.rewardItemId(), 1);
        if (reward.rewardItemState() != null && !reward.rewardItemState().isBlank()) {
            rewardStack = rewardStack.withState(reward.rewardItemState());
        }
        var transaction = player.giveItem(rewardStack, playerEntityRef, store);
        if (!transaction.succeeded()) {
            ItemUtils.interactivelyPickupItem(playerEntityRef, rewardStack, startPosition, store);
            return;
        }

        ItemStack addedStack = rewardStack;
        ItemStack remainder = transaction.getRemainder();
        if (remainder != null && !remainder.isEmpty()) {
            int addedQuantity = rewardStack.getQuantity() - remainder.getQuantity();
            if (addedQuantity <= 0) {
                ItemUtils.interactivelyPickupItem(playerEntityRef, rewardStack, startPosition, store);
                return;
            }
            addedStack = rewardStack.withQuantity(addedQuantity);
            ItemUtils.dropItem(playerEntityRef, remainder, store);
        }

        player.notifyPickupItem(playerEntityRef, addedStack, null, store);
        spawnPickupVisual(playerEntityRef, addedStack, startPosition, store);
    }

    private void spawnPickupVisual(
        Ref<EntityStore> playerEntityRef,
        ItemStack itemStack,
        Vector3d startPosition,
        Store<EntityStore> store
    ) {
        var holder = ItemComponent.generatePickedUpItem(itemStack, startPosition.clone().add(0.0, 0.2, 0.0), store, playerEntityRef);
        if (holder == null) {
            return;
        }

        var pickupComponent = holder.getComponent(PickupItemComponent.getComponentType());
        if (pickupComponent != null) {
            pickupComponent.setInitialLifeTime(LOOT_VISUAL_TRAVEL_SECONDS);
        }
        holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(LOOT_VISUAL_SCALE));
        store.addEntity(holder, AddReason.SPAWN);
    }

    private Vector3d getBobberPosition(FishingSession session) {
        return new Vector3d(session.getCastContext().targetPosition()).add(0.0, BOBBER_WATERLINE_OFFSET, 0.0);
    }

    private void completeCatch(
        Ref<EntityStore> playerEntityRef,
        Player player,
        PlayerRef playerRef,
        FishingSession session,
        Store<EntityStore> store
    ) {
        FishingPlayerDataComponent data = store.ensureAndGetComponent(playerEntityRef, dataType);
        CatchType catchType = rollCatchType();
        CatchReward reward = lootTableService.rollReward(session.getCastContext().region().regionId(), catchType);

        giveRewardWithVisual(playerEntityRef, player, reward, getBobberPosition(session), store);

        String codexEntryId = null;
        boolean discoveredNewFish = false;
        boolean improvedCatchQuality = false;
        if (reward.fishId() != null) {
            codexEntryId = configFishCodexEntryId(reward.fishId());
            discoveredNewFish = codexService.discover(data, codexEntryId);
            improvedCatchQuality = data.recordBestCatchQuality(codexEntryId, reward.quality());
        }
        store.putComponent(playerEntityRef, dataType, data);

        clearSession(store, session);
        notifyCatchMilestone(player, playerRef, reward, codexEntryId, discoveredNewFish, improvedCatchQuality);
    }

    private void cancelTask(ScheduledFuture<?> task) {
        if (task != null) {
            task.cancel(false);
        }
    }

    private void notifyCatchMilestone(Player player, PlayerRef playerRef, CatchReward reward, String codexEntryId, boolean discoveredNewFish, boolean improvedCatchQuality) {
        if (player == null || playerRef == null || reward.fishId() == null) {
            return;
        }

        String fishDisplayName = resolveCodexDisplayName(codexEntryId, reward.displayName());

        if (discoveredNewFish) {
            showCatchMilestoneTitle(
                playerRef,
                com.hypixel.hytale.server.core.Message.raw(NEW_FISH_DISCOVERY_TITLE),
                com.hypixel.hytale.server.core.Message.raw(fishDisplayName)
            );
            return;
        }

        if (improvedCatchQuality && reward.quality() != null && !reward.quality().isBlank()) {
            showCatchMilestoneTitle(
                playerRef,
                com.hypixel.hytale.server.core.Message.raw(NEW_QUALITY_DISCOVERY_TITLE),
                com.hypixel.hytale.server.core.Message.raw(toTitleCase(reward.quality()) + " " + fishDisplayName)
            );
        }
    }

    private void showCatchMilestoneTitle(
        PlayerRef playerRef,
        com.hypixel.hytale.server.core.Message title,
        com.hypixel.hytale.server.core.Message subtitle
    ) {
        EventTitleUtil.showEventTitleToPlayer(playerRef, title, subtitle, true);

        int soundId = resolveDiscoverySoundId();
        if (soundId == SoundEvent.EMPTY_ID) {
            return;
        }

        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundId, SoundCategory.UI);
    }

    private String toTitleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    private String resolveCodexDisplayName(String codexEntryId, String fallbackDisplayName) {
        CodexEntryDefinition codexEntry = codexEntriesById.get(codexEntryId);
        if (codexEntry == null || codexEntry.displayName() == null || codexEntry.displayName().isBlank()) {
            return fallbackDisplayName;
        }
        return codexEntry.displayName();
    }

    private boolean isConfiguredRod(String itemId) {
        return itemId != null && configuredRodItemIds.contains(itemId);
    }

    private String configFishCodexEntryId(String fishId) {
        return codexEntryIdsByFishId.getOrDefault(fishId, fishId);
    }

    private UUID resolvePlayerId(Ref<EntityStore> playerEntityRef) {
        Store<EntityStore> store = playerEntityRef.getStore();
        PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
        return playerRef == null ? null : playerRef.getUuid();
    }

    private int resolveBiteSoundId() {
        for (String soundEventId : BITE_SOUND_EVENT_IDS) {
            int soundId = SoundEvent.getAssetMap().getIndexOrDefault(soundEventId, SoundEvent.EMPTY_ID);
            if (soundId != SoundEvent.EMPTY_ID) {
                return soundId;
            }
        }
        return SoundEvent.EMPTY_ID;
    }

    private int resolveDiscoverySoundId() {
        for (String soundEventId : DISCOVERY_SOUND_EVENT_IDS) {
            int soundId = SoundEvent.getAssetMap().getIndexOrDefault(soundEventId, SoundEvent.EMPTY_ID);
            if (soundId != SoundEvent.EMPTY_ID) {
                return soundId;
            }
        }
        return SoundEvent.EMPTY_ID;
    }

    private void playBobberCastSound(Store<EntityStore> store, Vector3d bobberPosition) {
        int soundId = SoundEvent.getAssetMap().getIndexOrDefault(BOBBER_CAST_SOUND_EVENT_ID, SoundEvent.EMPTY_ID);
        if (soundId == SoundEvent.EMPTY_ID) {
            soundId = resolveBiteSoundId();
        }
        if (soundId == SoundEvent.EMPTY_ID) {
            return;
        }

        SoundUtil.playSoundEvent3d(
            soundId,
            SoundCategory.SFX,
            bobberPosition.getX(),
            bobberPosition.getY(),
            bobberPosition.getZ(),
            BOBBER_CAST_SOUND_VOLUME,
            1.0f,
            store
        );
    }

    private void expireSessionWithoutWorldAccess(FishingSession session) {
        UUID playerId = session.getPlayerUuid();
        sessionsByPlayerId.remove(playerId, session);
        cancelTask(biteTasksByPlayerId.remove(playerId));
        cancelTask(expiryTasksByPlayerId.remove(playerId));
        session.setState(FishingSessionState.EXPIRED);
    }

    private Set<String> buildConfiguredRodItemIds(TinyFishingConfig config) {
        Set<String> itemIds = new HashSet<>();
        for (var rodDefinition : config.rods()) {
            itemIds.add(rodDefinition.itemId());
        }
        return Set.copyOf(itemIds);
    }

    private Map<String, String> buildCodexEntryIdsByFishId(TinyFishingConfig config) {
        Map<String, String> entryIdsByFishId = new HashMap<>();
        for (FishDefinition fishDefinition : config.fishDefinitions()) {
            entryIdsByFishId.put(fishDefinition.id(), fishDefinition.codexEntryId());
        }
        return Map.copyOf(entryIdsByFishId);
    }

    private Map<String, CodexEntryDefinition> buildCodexEntriesById(TinyFishingConfig config) {
        Map<String, CodexEntryDefinition> entriesById = new HashMap<>();
        for (CodexEntryDefinition codexEntry : config.codexEntries()) {
            entriesById.put(codexEntry.id(), codexEntry);
        }
        return Map.copyOf(entriesById);
    }

    private static final class SchedulerThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "tiny-fishing-scheduler");
            thread.setDaemon(true);
            return thread;
        }
    }
}
