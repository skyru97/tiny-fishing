package com.tinyfishing.command;

import com.tinyfishing.TinyFishingPlugin;
import com.tinyfishing.component.FishingPlayerDataComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class TinyFishingCommand extends AbstractCommandCollection {
    public TinyFishingCommand(TinyFishingPlugin plugin) {
        super("tf", "Tiny Fishing commands");
        addAliases("tinyfishing");
        addSubCommand(new TinyFishingCodexCommand(plugin));
        addSubCommand(new TinyFishingResetCommand(plugin));
    }

    private static final class TinyFishingCodexCommand extends AbstractPlayerCommand {
        private final TinyFishingPlugin plugin;

        private TinyFishingCodexCommand(TinyFishingPlugin plugin) {
            super("codex", "Open fishdex");
            this.plugin = plugin;
        }

        @Override
        protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> senderRef, PlayerRef playerRef, com.hypixel.hytale.server.core.universe.world.World world) {
            plugin.openCodexPage(senderRef, store);
        }
    }

    private static final class TinyFishingResetCommand extends AbstractPlayerCommand {
        private final TinyFishingPlugin plugin;

        private TinyFishingResetCommand(TinyFishingPlugin plugin) {
            super("reset", "Reset Tiny Fishing codex progress");
            this.plugin = plugin;
        }

        @Override
        protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> senderRef, PlayerRef playerRef, com.hypixel.hytale.server.core.universe.world.World world) {
            FishingPlayerDataComponent data = store.ensureAndGetComponent(senderRef, plugin.getFishingPlayerDataType());
            data.setDiscoveredFishIds(new String[0]);
            data.setBestCatchQualities(new String[0]);
            store.putComponent(senderRef, plugin.getFishingPlayerDataType(), data);
            plugin.getFishingService().clearSessionForPlayer(playerRef);
        }
    }
}
