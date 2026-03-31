package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CivetIdleGoal extends Goal {
    private final CivetEntity civet;
    private int durationTimer;
    private CivetEntity.IdleVariant currentVariant;

    public CivetIdleGoal(CivetEntity civet) {
        this.civet = civet;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        boolean sleepingLike = this.civet.isPreparingSleep() || this.civet.isSleeping() || this.civet.isAwakeing();
        boolean swimming     = this.civet.isInWaterOrBubble();
        boolean climbing     = this.civet.isClimbing();
        boolean onGround     = this.civet.onGround();

        boolean doingAttack  = this.civet.isAttacking();
        boolean drinking     = this.civet.IsDrinking();
        boolean hasTarget    = this.civet.getTarget() != null;
        boolean hasWantedNav = !this.civet.getNavigation().isDone();

        boolean canIdle = this.civet.isAlive()
                && !sleepingLike && !swimming && !climbing && !hasWantedNav
                && !doingAttack && !drinking && !hasTarget && onGround
                && !this.civet.isCleaning() && !this.civet.isNewborn();

        return canIdle && this.civet.idleVariantCooldown <= 0 && this.civet.getRandom().nextFloat() < 0.01f;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.civet.getTarget() != null || this.civet.getLastHurtByMob() != null || !this.civet.onGround() || this.civet.isInWater()) {
            return false;
        }
        return this.durationTimer > 0;
    }

    @Override
    public void start() {
        this.currentVariant = pickVariant();
        this.civet.setIdleVariant(this.currentVariant);
        this.civet.getNavigation().stop();

        switch (this.currentVariant) {
            case STAND_UP                -> this.durationTimer = 40;
            case SNIFF_LEFT, SNIFF_RIGHT -> this.durationTimer = 40;
            case SIT  -> this.durationTimer = 110;
            case LAY  -> this.durationTimer = 120;
            default   -> this.durationTimer = 0;
        }
    }

    @Override
    public void tick() {
        this.durationTimer--;
    }

    @Override
    public void stop() {
        int cd = switch (this.currentVariant) {
            case STAND_UP -> 40;
            case SNIFF_LEFT, SNIFF_RIGHT -> 40;
            case SIT -> 110;
            case LAY -> 120;
            default -> 40;
        };
        this.civet.idleVariantCooldown = cd;
        this.civet.setIdleVariant(CivetEntity.IdleVariant.NONE);
        this.currentVariant = CivetEntity.IdleVariant.NONE;
    }

    private CivetEntity.IdleVariant pickVariant(){
        int roll = this.civet.getRandom().nextInt(100);
        if      (roll < 20) return CivetEntity.IdleVariant.STAND_UP;
        else if (roll < 45) return CivetEntity.IdleVariant.SNIFF_LEFT;
        else if (roll < 70) return CivetEntity.IdleVariant.SNIFF_RIGHT;
        else if (roll < 85) return CivetEntity.IdleVariant.SIT;
        else                return CivetEntity.IdleVariant.LAY;
    }
}