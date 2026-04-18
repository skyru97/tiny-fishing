package com.tinyfishing.system;

import com.tinyfishing.TinyFishingPlugin;
import com.tinyfishing.component.FishingBobberComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.SoundCategory;

public final class FishingBobberSystem extends EntityTickingSystem<EntityStore> {
    private static final double POSITION_EPSILON = 0.0001;
    private static final double IDLE_BOB_HEIGHT = 0.018;
    private static final float IDLE_BOB_FREQUENCY = 2.6f;
    private static final double BITE_BOB_HEIGHT = 0.08;
    private static final float BITE_SWAY_FREQUENCY = 10.0f;
    private static final float BITE_SPLASH_PERIOD_SECONDS = 0.95f;
    private static final float BITE_SPLASH_SOUND_VOLUME = 0.78f;
    private static final String BITE_SPLASH_PARTICLE_ID = "Water_Can_Splash";
    private static final String[] BITE_SPLASH_SOUND_EVENT_IDS = {"SFX_TinyFishing_BiteSplash", "SFX_Water_MoveIn", "SFX_Water_MoveOut", "SFX_Tool_Watering_Can_Water"};

    @Override
    public void tick(
        float deltaSeconds,
        int entityIndex,
        ArchetypeChunk<EntityStore> archetypeChunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> bobberRef = archetypeChunk.getReferenceTo(entityIndex);
        FishingBobberComponent bobber = store.getComponent(bobberRef, TinyFishingPlugin.get().getFishingBobberType());
        if (bobber == null) {
            return;
        }

        float previousBiteAnimationTime = bobber.getBiteAnimationTime();
        float previousSplashCooldown = bobber.getSplashCooldown();

        TransformComponent transform = store.getComponent(bobberRef, TransformComponent.getComponentType());
        if (transform != null) {
            Vector3d targetPosition = computeTargetPosition(bobber, deltaSeconds);
            if (transform.getPosition().distanceSquaredTo(targetPosition) > POSITION_EPSILON) {
                transform.teleportPosition(targetPosition);
            }

            Vector3f targetRotation = computeTargetRotation(bobber);
            if (transform.getRotation().distanceSquaredTo(targetRotation) > POSITION_EPSILON) {
                transform.teleportRotation(targetRotation);
            }

            if (bobber.isBiting()) {
                bobber.setSplashCooldown(Math.max(0.0f, bobber.getSplashCooldown() - deltaSeconds));
                if (bobber.getSplashCooldown() == 0.0f) {
                    spawnBiteSplash(bobber, targetPosition, store);
                    bobber.setSplashCooldown(BITE_SPLASH_PERIOD_SECONDS);
                }
            }
        }

        if (previousBiteAnimationTime != bobber.getBiteAnimationTime() || previousSplashCooldown != bobber.getSplashCooldown()) {
            commandBuffer.putComponent(bobberRef, TinyFishingPlugin.get().getFishingBobberType(), bobber);
        }

        Velocity velocity = store.getComponent(bobberRef, Velocity.getComponentType());
        if (velocity != null) {
            velocity.setZero();
            velocity.setClient(0.0, 0.0, 0.0);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return TinyFishingPlugin.get().getFishingBobberType();
    }

    private static Vector3d computeTargetPosition(FishingBobberComponent bobber, float deltaSeconds) {
        float nextTime = bobber.getBiteAnimationTime() + deltaSeconds;
        bobber.setBiteAnimationTime(nextTime);
        Vector3d targetPosition = bobber.getAnchorPosition().clone().add(0.0, -bobber.getSinkDepth(), 0.0);
        if (!bobber.isBiting()) {
            bobber.setSplashCooldown(0.0f);
            double bobOffset = Math.sin(nextTime * IDLE_BOB_FREQUENCY) * IDLE_BOB_HEIGHT;
            targetPosition.add(0.0, bobOffset, 0.0);
            return targetPosition;
        }

        double bobOffset = Math.sin(nextTime * BITE_SWAY_FREQUENCY) * BITE_BOB_HEIGHT;
        targetPosition.add(0.0, bobOffset, 0.0);
        return targetPosition;
    }

    private static Vector3f computeTargetRotation(FishingBobberComponent bobber) {
        return Vector3f.ZERO;
    }

    private static void spawnBiteSplash(FishingBobberComponent bobber, Vector3d position, Store<EntityStore> store) {
        ParticleUtil.spawnParticleEffect(BITE_SPLASH_PARTICLE_ID, position, store);
        int biteSplashSoundId = resolveBiteSplashSoundId();
        if (biteSplashSoundId == SoundEvent.EMPTY_ID) {
            return;
        }

        SoundUtil.playSoundEvent3d(
            biteSplashSoundId,
            SoundCategory.SFX,
            position.getX(),
            position.getY(),
            position.getZ(),
            BITE_SPLASH_SOUND_VOLUME,
            1.0f,
            store
        );
    }

    private static int resolveBiteSplashSoundId() {
        for (String soundEventId : BITE_SPLASH_SOUND_EVENT_IDS) {
            int soundId = SoundEvent.getAssetMap().getIndexOrDefault(soundEventId, SoundEvent.EMPTY_ID);
            if (soundId != SoundEvent.EMPTY_ID) {
                return soundId;
            }
        }
        return SoundEvent.EMPTY_ID;
    }
}
