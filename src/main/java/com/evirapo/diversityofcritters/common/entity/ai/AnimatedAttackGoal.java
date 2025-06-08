package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.IAnimatedAttacker;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class AnimatedAttackGoal extends MeleeAttackGoal {
    private final LivingEntity entity;
    private int attackDelay;
    private int ticksUntilNextAttack;
    private int attackDelayStored; //7
    private int ticksUntilNextAttackStored; //13
    private boolean shouldCountTillNextAttack = false;

    public AnimatedAttackGoal(PathfinderMob pMob, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen, int pAttackDelay, int pTicksUntilNextAttack) {
        super(pMob, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
        entity = pMob;
        attackDelayStored = pAttackDelay;
        ticksUntilNextAttackStored = pTicksUntilNextAttack;
    }

    @Override
    public void start() {
        super.start();
        attackDelay = attackDelayStored;
        ticksUntilNextAttack = ticksUntilNextAttackStored;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
        if (isEnemyWithinAttackDistance(pEnemy, pDistToEnemySqr)) {
            shouldCountTillNextAttack = true;

            if(isTimeToStartAttackAnimation()) {
                if (entity instanceof IAnimatedAttacker attacker)
                    attacker.setAttacking(true);
            }

            if(isTimeToAttack()) {
                this.mob.getLookControl().setLookAt(pEnemy.getX(), pEnemy.getEyeY(), pEnemy.getZ());
                performAttack(pEnemy);
            }

        } else {
            resetAttackCooldown();
            shouldCountTillNextAttack = false;

            if (entity instanceof IAnimatedAttacker attacker){
                attacker.setAttacking(false);
                attacker.setAttackAnimationTimeout(0);;
            }

        }
    }

    private boolean isEnemyWithinAttackDistance(LivingEntity pEnemy, double pDistToEnemySqr) {
        return pDistToEnemySqr <= this.getAttackReachSqr(pEnemy);
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(attackDelayStored + ticksUntilNextAttackStored);
    }

    protected boolean isTimeToAttack() {
        return this.ticksUntilNextAttack <= 0;
    }

    protected boolean isTimeToStartAttackAnimation() {
        return this.ticksUntilNextAttack <= attackDelay;
    }

    protected int getTicksUntilNextAttack() {
        return this.ticksUntilNextAttack;
    }


    protected void performAttack(LivingEntity pEnemy) {
        this.resetAttackCooldown();
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.mob.doHurtTarget(pEnemy);
    }

    @Override
    public void tick() {
        super.tick();
        if(shouldCountTillNextAttack) {
            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        }
    }

    @Override
    public void stop() {
        if (entity instanceof IAnimatedAttacker attacker)
            attacker.setAttacking(false);
        super.stop();
    }

    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        return (double)(this.mob.getBbWidth() * 1.25F * this.mob.getBbWidth() * 1.25F + pAttackTarget.getBbWidth());
    }
}
