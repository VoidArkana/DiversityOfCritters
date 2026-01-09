package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter.SleepState;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SleepBehaviorGoal extends Goal {
    private final DiverseCritter entity;
    private int timer;
    private int prepareDuration;
    private int wakeDuration;

    public SleepBehaviorGoal(DiverseCritter entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (entity.isInWater() || entity.isVehicle() || entity.isAttacking() || entity.isCleaning() || entity.isOrderedToSit()) {
            return false;
        }

        long time = entity.level().getDayTime() % 24000;
        boolean isNight = time >= 13000 && time < 23000;

        return entity.isDiurnal() ? isNight : !isNight;
    }

    @Override
    public boolean canContinueToUse() {
        if (entity.getSleepState() == SleepState.AWAKENING) {
            return timer < wakeDuration;
        }

        if (entity.hurtTime > 0 || entity.isInWater()) {
            startWaking();
            return true;
        }

        long time = entity.level().getDayTime() % 24000;
        boolean isNight = time >= 13000 && time < 23000;
        boolean shouldBeAsleep = entity.isDiurnal() ? isNight : !isNight;

        if (!shouldBeAsleep && entity.getSleepState() == SleepState.SLEEPING) {
            startWaking();
            return true;
        }

        return true;
    }

    @Override
    public void start() {
        entity.getNavigation().stop();
        this.prepareDuration = entity.getPreparingSleepDuration();
        this.wakeDuration = entity.getAwakeningDuration();

        entity.setSleepState(SleepState.PREPARING);
        this.timer = 0;
    }

    @Override
    public void stop() {
        entity.setSleepState(SleepState.AWAKE);
    }

    @Override
    public void tick() {
        timer++;

        SleepState current = entity.getSleepState();

        switch (current) {
            case PREPARING:
                if (timer >= prepareDuration) {
                    entity.setSleepState(SleepState.SLEEPING);
                    timer = 0;
                }
                break;

            case SLEEPING:
                entity.getNavigation().stop();
                break;

            case AWAKENING:
                if (timer >= wakeDuration) {
                }
                break;

            default:
                break;
        }
    }

    private void startWaking() {
        if (entity.getSleepState() != SleepState.AWAKENING) {
            entity.setSleepState(SleepState.AWAKENING);
            timer = 0;
        }
    }
}