package com.tinyfishing.item;

import com.tinyfishing.loot.CatchType;

public record CatchReward(
    CatchType catchType,
    String rewardItemId,
    String rewardItemState,
    String fishId,
    String displayName,
    String regionId
) {
}
