package com.evirapo.diversityofcritters.common.entity.util.sleep;

import net.minecraft.world.entity.EntityType;

import java.util.Set;

public interface ISleepingEntity {

    boolean isSleeping();
    void setSleeping(boolean sleeping);

    boolean isPreparingSleep();
    void setPreparingSleep(boolean preparing);

    boolean isAwakeing();               // ← nuevo
    void setAwakeing(boolean awakeing);// ← nuevo

    Set<EntityType<?>> getInterruptingEntityTypes();
}
