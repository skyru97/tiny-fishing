package com.tinyfishing.fishing;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.time.Instant;
import java.util.UUID;

public final class FishingSession {
    private final UUID playerUuid;
    private final Ref<EntityStore> playerEntityRef;
    private final FishingCastContext castContext;
    private final Instant biteAt;
    private final Instant reelWindowEndsAt;
    private Ref<EntityStore> bobberRef;
    private FishingSessionState state;

    public FishingSession(
        UUID playerUuid,
        Ref<EntityStore> playerEntityRef,
        FishingCastContext castContext,
        Instant biteAt,
        Instant reelWindowEndsAt
    ) {
        this.playerUuid = playerUuid;
        this.playerEntityRef = playerEntityRef;
        this.castContext = castContext;
        this.biteAt = biteAt;
        this.reelWindowEndsAt = reelWindowEndsAt;
        this.state = FishingSessionState.WAITING_FOR_BITE;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public Ref<EntityStore> getPlayerEntityRef() {
        return playerEntityRef;
    }

    public FishingCastContext getCastContext() {
        return castContext;
    }

    public Instant getBiteAt() {
        return biteAt;
    }

    public Instant getReelWindowEndsAt() {
        return reelWindowEndsAt;
    }

    public FishingSessionState getState() {
        return state;
    }

    public void setState(FishingSessionState state) {
        this.state = state;
    }

    public boolean shouldTriggerBite(Instant now) {
        return state == FishingSessionState.WAITING_FOR_BITE && !now.isBefore(biteAt);
    }

    public void markBiteTriggered() {
        this.state = FishingSessionState.REEL_WINDOW;
    }

    public boolean isInReelWindow(Instant now) {
        return state == FishingSessionState.REEL_WINDOW && !now.isAfter(reelWindowEndsAt);
    }

    public Ref<EntityStore> getBobberRef() {
        return bobberRef;
    }

    public void setBobberRef(Ref<EntityStore> bobberRef) {
        this.bobberRef = bobberRef;
    }
}
