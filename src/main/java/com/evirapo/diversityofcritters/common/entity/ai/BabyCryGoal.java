package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class BabyCryGoal extends Goal {
    private final DiverseCritter mob;

    public BabyCryGoal(DiverseCritter mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.mob.isNewborn() && this.mob.isCrying();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        this.mob.setDeltaMovement(0, this.mob.getDeltaMovement().y, 0);
    }

    @Override
    public void tick() {
        if (!this.mob.getNavigation().isDone()) {
            this.mob.getNavigation().stop();
        }

    }
}