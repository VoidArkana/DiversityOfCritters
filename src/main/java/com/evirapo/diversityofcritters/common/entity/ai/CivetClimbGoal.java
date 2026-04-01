package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Makes the civet spontaneously seek out climbable surfaces and climb them.
 * Two behavior variants chosen randomly:
 *   CLIMB_TOP  — climbs all the way up and vaults over the edge
 *   CLIMB_HANG — climbs partway up, hangs idle for a while, then lets go
 */
public class CivetClimbGoal extends Goal {
    private final CivetEntity civet;
    private final double speed;
    private final int searchRadius;

    private enum ClimbBehavior { CLIMB_TOP, CLIMB_HANG }

    private BlockPos targetWallPos;
    private int ticksInGoal = 0;
    private int cooldown = 0;
    private boolean hasClimbedAtAll = false;
    private ClimbBehavior behavior = ClimbBehavior.CLIMB_TOP;

    // HANG sub-state
    private int hangTicksRemaining = 0;
    private int climbTicksBeforeHang = 0;
    private boolean isCurrentlyHanging = false;

    /** Max ticks we'll spend in this goal before giving up */
    private static final int MAX_TICKS = 200;
    /** Ticks between climb attempts */
    private static final int COOLDOWN_MIN = 600;  // 30 seconds
    private static final int COOLDOWN_MAX = 2400; // 2 minutes
    /** How many ticks to climb before entering hang */
    private static final int HANG_CLIMB_MIN = 15;
    private static final int HANG_CLIMB_MAX = 40;
    /** How long to hang on the wall */
    private static final int HANG_DURATION_MIN = 60;  // 3 seconds
    private static final int HANG_DURATION_MAX = 160; // 8 seconds

    public CivetClimbGoal(CivetEntity civet, double speed, int searchRadius) {
        this.civet = civet;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (civet.level().isClientSide() || civet.isNewborn() || civet.isBaby()) return false;
        if (civet.isSleeping() || civet.isOrderedToSit()) return false;

        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }

        // Random chance (~1/80 per tick when cooldown is 0)
        if (civet.getRandom().nextInt(80) != 0) return false;

        this.targetWallPos = findNearestClimbable();
        return this.targetWallPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (civet.isSleeping() || civet.isOrderedToSit()) return false;
        if (this.ticksInGoal > MAX_TICKS) return false;

        // For CLIMB_TOP: done when we climbed and landed back on ground (vaulted over)
        if (this.behavior == ClimbBehavior.CLIMB_TOP) {
            if (this.hasClimbedAtAll && !civet.isClimbing() && civet.onGround()) {
                return false;
            }
        }

        // For CLIMB_HANG: done when hang timer expires and we've fallen back to ground
        if (this.behavior == ClimbBehavior.CLIMB_HANG) {
            if (this.isCurrentlyHanging && this.hangTicksRemaining <= 0) {
                // Hang time over — release, wait to land
                return false;
            }
            // If we were hanging, released, and landed
            if (this.hasClimbedAtAll && !this.isCurrentlyHanging
                    && !civet.isClimbing() && civet.onGround()
                    && this.hangTicksRemaining <= 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void start() {
        this.ticksInGoal = 0;
        this.hasClimbedAtAll = false;
        this.isCurrentlyHanging = false;
        this.hangTicksRemaining = 0;
        this.climbTicksBeforeHang = 0;

        // 40% chance to hang, 60% to climb all the way
        this.behavior = civet.getRandom().nextFloat() < 0.4f
                ? ClimbBehavior.CLIMB_HANG
                : ClimbBehavior.CLIMB_TOP;

        if (this.behavior == ClimbBehavior.CLIMB_HANG) {
            this.climbTicksBeforeHang = HANG_CLIMB_MIN
                    + civet.getRandom().nextInt(HANG_CLIMB_MAX - HANG_CLIMB_MIN);
            this.hangTicksRemaining = HANG_DURATION_MIN
                    + civet.getRandom().nextInt(HANG_DURATION_MAX - HANG_DURATION_MIN);
        }

        // Navigate towards the climbable block
        civet.getNavigation().moveTo(
            targetWallPos.getX() + 0.5,
            targetWallPos.getY(),
            targetWallPos.getZ() + 0.5,
            speed
        );
    }

    @Override
    public void tick() {
        if (targetWallPos == null) return;

        this.ticksInGoal++;

        if (civet.isClimbing()) {
            this.hasClimbedAtAll = true;
        }

        // --- HANG behavior: transition to hanging after climbing a bit ---
        if (this.behavior == ClimbBehavior.CLIMB_HANG && this.hasClimbedAtAll) {
            if (!this.isCurrentlyHanging) {
                this.climbTicksBeforeHang--;
                if (this.climbTicksBeforeHang <= 0 && civet.isClimbing()) {
                    // Transition to hang
                    this.isCurrentlyHanging = true;
                    civet.setClimbState(CivetEntity.CLIMB_HANG);
                    civet.getNavigation().stop();
                    return;
                }
            } else {
                // Currently hanging
                this.hangTicksRemaining--;
                if (this.hangTicksRemaining <= 0) {
                    // Release — let go of the wall
                    civet.setClimbState(CivetEntity.CLIMB_NONE);
                    this.isCurrentlyHanging = false;
                    return;
                }
                // Keep hanging: ensure state stays HANG and push into wall
                if (civet.getClimbState() != CivetEntity.CLIMB_HANG) {
                    civet.setClimbState(CivetEntity.CLIMB_HANG);
                }
                civet.getMoveControl().setWantedPosition(
                    targetWallPos.getX() + 0.5,
                    civet.getY(),
                    targetWallPos.getZ() + 0.5,
                    speed * 0.3
                );
                return;
            }
        }

        // --- Normal climbing (both CLIMB_TOP and CLIMB_HANG before hang) ---
        if (civet.isClimbing() && !this.isCurrentlyHanging) {
            // On the wall — keep pushing into it with an upward target
            civet.getMoveControl().setWantedPosition(
                targetWallPos.getX() + 0.5,
                civet.getY() + 3.0,
                targetWallPos.getZ() + 0.5,
                speed
            );
        } else if (!civet.isClimbing() && !this.isCurrentlyHanging) {
            // Not climbing yet — walk towards the wall
            double distSq = civet.distanceToSqr(
                targetWallPos.getX() + 0.5,
                civet.getY(),
                targetWallPos.getZ() + 0.5
            );

            if (distSq > 4.0D) {
                if (!civet.getNavigation().isInProgress()) {
                    civet.getNavigation().moveTo(
                        targetWallPos.getX() + 0.5,
                        targetWallPos.getY(),
                        targetWallPos.getZ() + 0.5,
                        speed
                    );
                }
            } else {
                // Close enough — push into the wall
                civet.getMoveControl().setWantedPosition(
                    targetWallPos.getX() + 0.5,
                    civet.getY() + 3.0,
                    targetWallPos.getZ() + 0.5,
                    speed
                );
            }
        }
    }

    @Override
    public void stop() {
        // Award enrichment if we actually climbed
        if (this.hasClimbedAtAll) {
            civet.setEnrichment(civet.maxEnrichment());
        }

        // Make sure we leave climbing state
        if (civet.isClimbing()) {
            civet.setClimbState(CivetEntity.CLIMB_NONE);
        }

        this.targetWallPos = null;
        this.ticksInGoal = 0;
        this.hasClimbedAtAll = false;
        this.isCurrentlyHanging = false;
        this.hangTicksRemaining = 0;
        this.cooldown = COOLDOWN_MIN + civet.getRandom().nextInt(COOLDOWN_MAX - COOLDOWN_MIN);
        civet.getNavigation().stop();
    }

    private BlockPos findNearestClimbable() {
        BlockPos origin = civet.blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (civet.level().getBlockState(pos).is(DoCTags.Blocks.CIVET_CLIMBABLE)) {
                        double distSq = origin.distSqr(pos);
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestPos = pos;
                        }
                    }
                }
            }
        }
        return bestPos;
    }
}
