package com.tinyfishing;

import com.tinyfishing.command.TinyFishingCommand;
import com.tinyfishing.component.FishingBobberComponent;
import com.tinyfishing.component.FishingPlayerDataComponent;
import com.tinyfishing.config.TinyFishingConfig;
import com.tinyfishing.config.TinyFishingConfigLoader;
import com.tinyfishing.fishing.CodexService;
import com.tinyfishing.fishing.FishingService;
import com.tinyfishing.fishing.LootTableService;
import com.tinyfishing.interaction.FishingRodInteraction;
import com.tinyfishing.system.FishingBobberSystem;
import com.tinyfishing.ui.FishingCodexPage;
import com.tinyfishing.world.FishingContextResolver;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Objects;

public final class TinyFishingPlugin extends JavaPlugin {
    private static TinyFishingPlugin instance;
    private ComponentType<EntityStore, FishingBobberComponent> fishingBobberType;
    private ComponentType<EntityStore, FishingPlayerDataComponent> fishingPlayerDataType;
    private TinyFishingConfigLoader configLoader;
    private RuntimeContext runtime;

    public TinyFishingPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static TinyFishingPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        this.fishingBobberType = getEntityStoreRegistry().registerComponent(
            FishingBobberComponent.class,
            FishingBobberComponent::new
        );
        this.fishingPlayerDataType = getEntityStoreRegistry().registerComponent(
            FishingPlayerDataComponent.class,
            "tiny_fishing_player_data",
            FishingPlayerDataComponent.CODEC
        );
        getEntityStoreRegistry().registerSystem(new FishingBobberSystem());

        this.configLoader = new TinyFishingConfigLoader(getLogger());
        this.runtime = loadRuntimeContext();
        getCodecRegistry(com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.CODEC)
            .register("TinyFishingFish", FishingRodInteraction.class, FishingRodInteraction.CODEC);

        getCommandRegistry().registerCommand(new TinyFishingCommand(this));

        EventRegistry eventRegistry = getEventRegistry();
        eventRegistry.registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        eventRegistry.registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    @Override
    protected void shutdown() {
        if (runtime != null) {
            runtime.fishingService().shutdown();
            runtime = null;
        }
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        Store<EntityStore> store = event.getPlayerRef().getStore();
        FishingPlayerDataComponent data = store.ensureAndGetComponent(event.getPlayerRef(), fishingPlayerDataType);
        Player player = store.getComponent(event.getPlayerRef(), Player.getComponentType());
        PlayerRef playerRef = store.getComponent(event.getPlayerRef(), PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

        runtime.fishingService().cleanupOrphanedBobbers(store);
        store.putComponent(event.getPlayerRef(), fishingPlayerDataType, data);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        runtime.fishingService().clearSessionForPlayer(event.getPlayerRef());
    }

    public ComponentType<EntityStore, FishingPlayerDataComponent> getFishingPlayerDataType() {
        return fishingPlayerDataType;
    }

    public ComponentType<EntityStore, FishingBobberComponent> getFishingBobberType() {
        return fishingBobberType;
    }

    public TinyFishingConfig getConfig() {
        return runtime.config();
    }

    public FishingService getFishingService() {
        return runtime.fishingService();
    }

    public FishingCodexPage createCodexPage(PlayerRef playerRef, Ref<EntityStore> playerEntityRef, Store<EntityStore> store) {
        return new FishingCodexPage(playerRef, playerEntityRef, store, fishingPlayerDataType, runtime.config());
    }

    public void openCodexPage(Ref<EntityStore> playerEntityRef, Store<EntityStore> store) {
        if (playerEntityRef == null || store == null || !playerEntityRef.isValid()) {
            return;
        }

        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

        player.getPageManager().openCustomPage(playerEntityRef, store, createCodexPage(playerRef, playerEntityRef, store));
    }

    private RuntimeContext loadRuntimeContext() {
        TinyFishingConfig config = Objects.requireNonNull(configLoader.loadBundled(), "Tiny Fishing config must load.");
        CodexService codexService = new CodexService(config.codexEntries());
        FishingContextResolver fishingContextResolver = new FishingContextResolver(config.rods(), config.fishingRegions());
        LootTableService lootTableService = new LootTableService(config.fishDefinitions(), config.fishingRegions(), config.prizeItems());
        FishingService fishingService = new FishingService(
            getLogger(),
            fishingBobberType,
            fishingPlayerDataType,
            config,
            codexService,
            fishingContextResolver,
            lootTableService
        );
        return new RuntimeContext(config, codexService, fishingContextResolver, lootTableService, fishingService);
    }

    private record RuntimeContext(
        TinyFishingConfig config,
        CodexService codexService,
        FishingContextResolver fishingContextResolver,
        LootTableService lootTableService,
        FishingService fishingService
    ) {
    }
}
