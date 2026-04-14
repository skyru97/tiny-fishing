package com.tinyfishing.fishing;

import com.tinyfishing.item.FishingRegionDefinition;
import com.tinyfishing.item.RodDefinition;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;

public record FishingCastContext(
    RodDefinition rod,
    FishingRegionDefinition region,
    Vector3i targetBlock,
    Vector3d targetPosition,
    String environmentId
) {
}
