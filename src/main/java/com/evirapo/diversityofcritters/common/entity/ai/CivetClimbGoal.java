package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Handles the HANG enrichment behavior: when the civet is already on a wall
 * (placed there by pathfinding), there is a small random chance it pauses
 * and hangs in place for a while before continuing.
 *
 * This goal no longer searches for walls or drives climbing —
 * that is now handled entirely by CivetNodeEvaluator + CivetMoveControl.
 */
public class CivetClimbGoal extends Goal {
    private final CivetEntity civet;

    private int hangTicksRemaining = 0;
    private int cooldown = 0;

    private static final int COOLDOWN_MIN = 600;
    private static final int COOLDOWN_MAX = 2400;
    private static final int HANG_DURATION_MIN = 60;
    private static final int HANG_DURATION_MAX = 160;

    public CivetClimbGoal(CivetEntity civet, double speed, int searchRadius) {
        this.civet = civet;
        // speed and searchRadius kept in constructor signature to avoid
        // changing the registration call in CivetEntity.registerGoals()
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (civet.level().isClientSide() || civet.isNewborn()) return false;
        if (civet.isSleeping() || civet.isOrderedToSit()) return false;
        if (this.cooldown-- > 0) return false;

        // Only hang if the civet is already climbing (pathfinding put it on a wall)
        return civet.isClimbingUp() && civet.getRandom().nextInt(60) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (civet.isSleeping() || civet.isOrderedToSit()) return false;
        return this.hangTicksRemaining > 0;
    }

    @Override
    public void start() {
        this.hangTicksRemaining = HANG_DURATION_MIN
                + civet.getRandom().nextInt(HANG_DURATION_MAX - HANG_DURATION_MIN);
        civet.setClimbState(CivetEntity.CLIMB_HANG);
        civet.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Lock in HANG state and zero movement
        if (civet.getClimbState() != CivetEntity.CLIMB_HANG) {
            civet.setClimbState(CivetEntity.CLIMB_HANG);
        }
        civet.setDeltaMovement(0, 0, 0);
        this.hangTicksRemaining--;
    }

    @Override
    public void stop() {
        if (civet.isHanging()) {
            civet.setClimbState(CivetEntity.CLIMB_NONE);
        }
        // Award enrichment for hanging
        civet.setEnrichment(civet.maxEnrichment());
        this.cooldown = COOLDOWN_MIN + civet.getRandom().nextInt(COOLDOWN_MAX - COOLDOWN_MIN);
    }
}
