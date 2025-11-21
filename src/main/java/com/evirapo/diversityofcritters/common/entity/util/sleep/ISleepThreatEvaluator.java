package com.evirapo.diversityofcritters.common.entity.util.sleep;

import net.minecraft.world.entity.LivingEntity;

public interface ISleepThreatEvaluator  {
    boolean shouldInterruptSleepDueTo(LivingEntity nearby);
}
