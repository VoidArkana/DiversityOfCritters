package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;


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

    private int hangTicksRemaining = 0;
    private int climbTicksBeforeHang = 0;
    private boolean isCurrentlyHanging = false;

    private static final int MAX_TICKS = 200;
    private static final int COOLDOWN_MIN = 600;
    private static final int COOLDOWN_MAX = 2400;
    private static final int HANG_CLIMB_MIN = 15;
    private static final int HANG_CLIMB_MAX = 40;
    private static final int HANG_DURATION_MIN = 60;
    private static final int HANG_DURATION_MAX = 160;

    public CivetClimbGoal(CivetEntity civet, double speed, int searchRadius) {
        this.civet = civet;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (civet.level().isClientSide() || civet.isNewborn()) return false;
        if (civet.isSleeping() || civet.isOrderedToSit()) return false;

        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }

        if (civet.getRandom().nextInt(80) != 0) return false;

        this.targetWallPos = findNearestClimbable();
        return this.targetWallPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (civet.isSleeping() || civet.isOrderedToSit()) return false;
        if (this.ticksInGoal > MAX_TICKS) return false;

        if (this.behavior == ClimbBehavior.CLIMB_TOP) {
            if (this.hasClimbedAtAll && !civet.isClimbing() && civet.onGround()) {
                return false;
            }
        }

        if (this.behavior == ClimbBehavior.CLIMB_HANG) {
            if (this.isCurrentlyHanging && this.hangTicksRemaining <= 0) {
                // Hang time over — release, wait to land
                return false;
            }
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

        this.behavior = civet.getRandom().nextFloat() < 0.4f
                ? ClimbBehavior.CLIMB_HANG
                : ClimbBehavior.CLIMB_TOP;

        if (this.behavior == ClimbBehavior.CLIMB_HANG) {
            this.climbTicksBeforeHang = HANG_CLIMB_MIN
                    + civet.getRandom().nextInt(HANG_CLIMB_MAX - HANG_CLIMB_MIN);
            this.hangTicksRemaining = HANG_DURATION_MIN
                    + civet.getRandom().nextInt(HANG_DURATION_MAX - HANG_DURATION_MIN);
        }

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

        if (this.behavior == ClimbBehavior.CLIMB_HANG && this.hasClimbedAtAll) {
            if (!this.isCurrentlyHanging) {
                this.climbTicksBeforeHang--;
                if (this.climbTicksBeforeHang <= 0 && civet.isClimbing()) {
                    this.isCurrentlyHanging = true;
                    civet.setClimbState(CivetEntity.CLIMB_HANG);
                    civet.getNavigation().stop();
                    return;
                }
            } else {
                this.hangTicksRemaining--;
                if (this.hangTicksRemaining <= 0) {
                    civet.setClimbState(CivetEntity.CLIMB_NONE);
                    this.isCurrentlyHanging = false;
                    return;
                }
                if (civet.getClimbState() != CivetEntity.CLIMB_HANG) {
                    civet.setClimbState(CivetEntity.CLIMB_HANG);
                }

                civet.setDeltaMovement(0, 0, 0);
                return;
            }
        }

        net.minecraft.world.phys.AABB wallBox = new net.minecraft.world.phys.AABB(targetWallPos);

        boolean isTouchingWall = civet.getBoundingBox().inflate(0.1D).intersects(wallBox);

        if (!isTouchingWall && !this.hasClimbedAtAll) {

            civet.getLookControl().setLookAt(targetWallPos.getX() + 0.5, targetWallPos.getY() + 0.5, targetWallPos.getZ() + 0.5);

            if (!civet.getNavigation().isInProgress()) {
                net.minecraft.world.phys.Vec3 civetPos = civet.position();
                net.minecraft.world.phys.Vec3 targetCenter = new net.minecraft.world.phys.Vec3(targetWallPos.getX() + 0.5, civet.getY(), targetWallPos.getZ() + 0.5);

                net.minecraft.world.phys.Vec3 direction = targetCenter.subtract(civetPos).normalize().scale(0.05D);
                civet.setDeltaMovement(direction.x, civet.getDeltaMovement().y, direction.z);
            }
        } else {

            civet.getMoveControl().setWantedPosition(
                    targetWallPos.getX() + 0.5,
                    civet.getY() + 3.0,
                    targetWallPos.getZ() + 0.5,
                    speed
            );
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
