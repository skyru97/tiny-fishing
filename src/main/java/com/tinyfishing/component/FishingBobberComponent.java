package com.tinyfishing.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class FishingBobberComponent implements Component<EntityStore> {
    private final Vector3d anchorPosition = new Vector3d();
    private Ref<EntityStore> ownerPlayerRef;
    private float biteAnimationTime;
    private float splashCooldown;
    private float sinkDepth;
    private boolean biting;

    public FishingBobberComponent() {
    }

    public FishingBobberComponent(Vector3d anchorPosition) {
        setAnchorPosition(anchorPosition);
    }

    public FishingBobberComponent(Vector3d anchorPosition, Ref<EntityStore> ownerPlayerRef) {
        setAnchorPosition(anchorPosition);
        this.ownerPlayerRef = ownerPlayerRef;
    }

    public Vector3d getAnchorPosition() {
        return anchorPosition;
    }

    public void setAnchorPosition(Vector3d anchorPosition) {
        this.anchorPosition.assign(anchorPosition);
    }

    public Ref<EntityStore> getOwnerPlayerRef() {
        return ownerPlayerRef;
    }

    public void setOwnerPlayerRef(Ref<EntityStore> ownerPlayerRef) {
        this.ownerPlayerRef = ownerPlayerRef;
    }

    public float getBiteAnimationTime() {
        return biteAnimationTime;
    }

    public void setBiteAnimationTime(float biteAnimationTime) {
        this.biteAnimationTime = biteAnimationTime;
    }

    public float getSplashCooldown() {
        return splashCooldown;
    }

    public void setSplashCooldown(float splashCooldown) {
        this.splashCooldown = splashCooldown;
    }

    public float getSinkDepth() {
        return sinkDepth;
    }

    public void setSinkDepth(float sinkDepth) {
        this.sinkDepth = sinkDepth;
    }

    public boolean isBiting() {
        return biting;
    }

    public void setBiting(boolean biting) {
        this.biting = biting;
    }

    @Override
    public FishingBobberComponent clone() {
        FishingBobberComponent clone = new FishingBobberComponent(anchorPosition, ownerPlayerRef);
        clone.biteAnimationTime = biteAnimationTime;
        clone.splashCooldown = splashCooldown;
        clone.sinkDepth = sinkDepth;
        clone.biting = biting;
        return clone;
    }
}
