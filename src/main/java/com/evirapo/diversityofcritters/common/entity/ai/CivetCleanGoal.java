package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CivetCleanGoal extends Goal {

    private final CivetEntity entity;
    private int cleanTimer;
    private final int CLEAN_DURATION = 80;

    public CivetCleanGoal(CivetEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (entity.isNewborn()) {
            return false;
        }

        if (entity.isSleeping() || entity.isPreparingSleep() || entity.isAwakeing() ||
                entity.isAttacking() || entity.isOrderedToSit() || entity.isIdleLocked()) {
            return false;
        }

        if (!entity.hasLowHygiene()) {
            return false;
        }

        return entity.getRandom().nextFloat() < 0.005f;
    }

    @Override
    public boolean canContinueToUse() {
        if (entity.isNewborn() || entity.isSleeping() || entity.isPreparingSleep() || entity.isAwakeing() ||
                entity.isAttacking() || entity.isOrderedToSit()) {
            return false;
        }

        return cleanTimer > 0;
    }

    @Override
    public void start() {
        this.cleanTimer = CLEAN_DURATION;
        this.entity.getNavigation().stop();
        this.entity.setCleaning(true);
    }

    @Override
    public void stop() {
        this.cleanTimer = 0;
        this.entity.setCleaning(false);
    }

    @Override
    public void tick() {
        this.cleanTimer--;
        if (this.cleanTimer == 1) {
            this.entity.setHygiene(this.entity.maxHygiene());
        }
    }
}