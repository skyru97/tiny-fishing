package com.tinyfishing.interaction;

import com.tinyfishing.TinyFishingPlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;

public final class FishingRodInteraction extends SimpleInstantInteraction {
    private static final String CAST_EFFECT_ROOT_ID = "TinyFishing_Rod_Cast";
    private static final String REEL_EFFECT_ROOT_ID = "TinyFishing_Rod_Reel";

    public static final BuilderCodec<FishingRodInteraction> CODEC = BuilderCodec.builder(
        FishingRodInteraction.class,
        FishingRodInteraction::new,
        SimpleInstantInteraction.CODEC
    ).documentation("Starts or reels Tiny Fishing when the rod is used.").build();

    public FishingRodInteraction() {
    }

    @Override
    protected void firstRun(InteractionType interactionType, InteractionContext context, CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> playerEntityRef = context.getEntity();
        PlayerRef playerRef = commandBuffer.getComponent(playerEntityRef, PlayerRef.getComponentType());
        Player player = commandBuffer.getComponent(playerEntityRef, Player.getComponentType());
        if (playerRef == null || player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        String heldItemId = context.getHeldItem() == null ? null : context.getHeldItem().getItemId();
        if (heldItemId == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        var fishingService = TinyFishingPlugin.get().getFishingService();

        if (fishingService.hasActiveSession(playerRef.getUuid())) {
            playEffect(context, REEL_EFFECT_ROOT_ID);
            world.execute(() -> fishingService.handleInteractionReel(playerEntityRef));
            context.getState().state = InteractionState.Finished;
            return;
        }

        Vector3i targetBlock = fishingService.resolveTargetWater(commandBuffer, world, playerEntityRef, 12.0f);
        playEffect(context, CAST_EFFECT_ROOT_ID);
        world.execute(() -> fishingService.handleInteractionCast(playerEntityRef, heldItemId, targetBlock));
        context.getState().state = InteractionState.Finished;
    }

    @Override
    protected void simulateFirstRun(InteractionType interactionType, InteractionContext context, CooldownHandler cooldownHandler) {
        // The server execution owns the cast/reel side effects and one-shot visuals.
    }

    private static void playEffect(InteractionContext context, String rootInteractionId) {
        context.execute(RootInteraction.getRootInteractionOrUnknown(rootInteractionId));
    }
}
