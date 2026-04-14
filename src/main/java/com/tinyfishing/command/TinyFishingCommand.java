package com.tinyfishing.command;

import com.tinyfishing.TinyFishingPlugin;
import com.tinyfishing.component.FishingPlayerDataComponent;
import com.tinyfishing.fishing.FishingSession;
import com.tinyfishing.item.RodDefinition;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class TinyFishingCommand extends AbstractCommandCollection {
    public TinyFishingCommand(TinyFishingPlugin plugin) {
        super("tf", "Tiny Fishing commands");
        addAliases("tinyfishing");
        addSubCommand(new TinyFishingStatusCommand(plugin));
        addSubCommand(new TinyFishingCodexCommand(plugin));
        addSubCommand(new TinyFishingGiveRodCommand(plugin));
    }

    private static final class TinyFishingStatusCommand extends AbstractPlayerCommand {
        private final TinyFishingPlugin plugin;

        private TinyFishingStatusCommand(TinyFishingPlugin plugin) {
            super("status", "Show Tiny Fishing status");
            this.plugin = plugin;
        }

        @Override
        protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> senderRef, PlayerRef playerRef, com.hypixel.hytale.server.core.universe.world.World world) {
            FishingPlayerDataComponent data = store.ensureAndGetComponent(senderRef, plugin.getFishingPlayerDataType());
            FishingSession session = plugin.getFishingService().getSession(senderRef);
            String sessionText = session == null ? "no active cast" : ("active cast in " + session.getCastContext().region().regionId());
            context.sendMessage(com.hypixel.hytale.server.core.Message.raw(plugin.getFishingService().buildStatusLine(data) + " | " + sessionText));
        }
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

    private static final class TinyFishingGiveRodCommand extends AbstractPlayerCommand {
        private final TinyFishingPlugin plugin;

        private TinyFishingGiveRodCommand(TinyFishingPlugin plugin) {
            super("giverod", "Give the Tiny Fishing rod");
            this.plugin = plugin;
        }

        @Override
        protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> senderRef, PlayerRef playerRef, com.hypixel.hytale.server.core.universe.world.World world) {
            Player player = store.getComponent(senderRef, Player.getComponentType());
            if (player == null || plugin.getConfig().rods().isEmpty()) {
                return;
            }

            RodDefinition rod = plugin.getConfig().rods().getFirst();
            player.giveItem(new ItemStack(rod.itemId(), 1), senderRef, store);
            context.sendMessage(com.hypixel.hytale.server.core.Message.raw("Given 1x " + rod.displayName() + "."));
        }
    }
}
