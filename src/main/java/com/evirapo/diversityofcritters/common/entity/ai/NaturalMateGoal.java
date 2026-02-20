package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;

public class NaturalMateGoal extends Goal {
    protected final DiverseCritter animal;
    protected final Level level;
    protected final double speedModifier;
    protected DiverseCritter mate;
    private int loveTime;
    private boolean bred;

    private static final int COURTSHIP_TICKS = 60;
    private static final double CLOSE_DISTANCE_SQ = 1.5D;
    private static final int MAX_APPROACH_TICKS = 200;

    private static final TargetingConditions PARTNER_TARGETING =
            TargetingConditions.forNonCombat().range(8.0D).ignoreLineOfSight();

    public NaturalMateGoal(DiverseCritter animal, double speedModifier) {
        this.animal = animal;
        this.level = animal.level();
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.animal.isBaby() || this.animal.isPregnant()
                || !this.animal.canBreed() || this.animal.breedCooldown > 0) {
            return false;
        }

        if (this.animal.isOrderedToSit() || this.animal.isSleeping() || this.animal.isCleaning()) {
            return false;
        }

        if (this.animal.getRandom().nextInt(400) != 0) {
            return false;
        }

        this.mate = this.getFreePartner();
        return this.mate != null;
    }

    private DiverseCritter getFreePartner() {
        List<? extends DiverseCritter> list = this.level.getNearbyEntities(
                this.animal.getClass(), PARTNER_TARGETING, this.animal,
                this.animal.getBoundingBox().inflate(8.0D));

        double closestDist = Double.MAX_VALUE;
        DiverseCritter best = null;

        for (DiverseCritter candidate : list) {
            if (this.animal.canMate(candidate) && this.animal.distanceToSqr(candidate) < closestDist) {
                best = candidate;
                closestDist = this.animal.distanceToSqr(candidate);
            }
        }
        return best;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.bred
                && this.mate != null
                && this.mate.isAlive()
                && this.animal.canMate(this.mate)
                && this.loveTime < MAX_APPROACH_TICKS + COURTSHIP_TICKS;
    }

    @Override
    public void start() {
        this.loveTime = 0;
        this.bred = false;
    }

    @Override
    public void stop() {
        this.mate = null;
        this.loveTime = 0;
        this.bred = false;
    }

    @Override
    public void tick() {
        this.animal.getLookControl().setLookAt(this.mate, 10.0F, (float) this.animal.getMaxHeadXRot());
        ++this.loveTime;

        boolean close = this.animal.distanceToSqr(this.mate) < CLOSE_DISTANCE_SQ;

        if (!close) {
            this.animal.getNavigation().moveTo(this.mate, this.speedModifier);
        } else {
            this.animal.getNavigation().stop();

            if (this.loveTime >= COURTSHIP_TICKS) {
                this.breed();
                this.bred = true;
            }
        }
    }

    private void breed() {
        DiverseCritter female = this.animal.getIsMale() ? this.mate : this.animal;
        DiverseCritter male = this.animal.getIsMale() ? this.animal : this.mate;

        female.setPregnant(true);
        female.pregnancyTimer = DiverseCritter.PREGNANCY_TIME_TICKS;

        int cooldown = 96000 + this.animal.getRandom().nextInt(24000);
        female.breedCooldown = cooldown;
        male.breedCooldown = cooldown;

        this.level.broadcastEntityEvent(female, (byte) 18);
        this.level.broadcastEntityEvent(male, (byte) 18);
    }
}
