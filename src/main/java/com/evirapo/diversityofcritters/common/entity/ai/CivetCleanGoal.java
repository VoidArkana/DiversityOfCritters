package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CivetCleanGoal extends Goal {

    private final CivetEntity entity;
    private int cleanTimer;
    private final int CLEAN_DURATION = 100;

    public CivetCleanGoal(CivetEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (entity.isSleeping() || entity.isPreparingSleep() || entity.isAwakeing() ||
                entity.isAttacking() || entity.isOrderedToSit() || entity.isIdleLocked()) {
            return false;
        }

        if (entity.getHygiene() >= entity.maxHygiene()) {
            return false;
        }

        float chance = entity.hasLowHygiene() ? 0.02f : 0.001f;
        return entity.getRandom().nextFloat() < chance;
    }

    @Override
    public boolean canContinueToUse() {
        if (entity.isSleeping() || entity.isPreparingSleep() || entity.isAwakeing() ||
                entity.isAttacking() || entity.isOrderedToSit()) {
            return false;
        }

        return cleanTimer > 0 && entity.getHygiene() < entity.maxHygiene();
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
            int recovery = entity.hasLowHygiene() ? 600 : 400;
            this.entity.setHygiene(Math.min(this.entity.maxHygiene(), this.entity.getHygiene() + recovery));
        }
    }
}