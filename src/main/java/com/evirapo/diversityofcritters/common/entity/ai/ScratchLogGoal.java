package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ScratchLogGoal extends Goal {
    private final CivetEntity civet;
    private final double speed;
    private final int searchRadius;
    private BlockPos logPos;
    private int scratchTimer = 0;

    private static final int TOTAL_DURATION = 100;

    public ScratchLogGoal(CivetEntity civet, double speed, int searchRadius) {
        this.civet = civet;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (civet.level().isClientSide() || !civet.isEnrichmentNeeded() || civet.isNewborn()) {
            return false;
        }

        logPos = findNearestLog();
        return logPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return logPos != null && scratchTimer < TOTAL_DURATION && civet.isEnrichmentNeeded() && isValidLog(logPos);
    }

    @Override
    public void start() {
        scratchTimer = 0;
        civet.getNavigation().moveTo(logPos.getX() + 0.5, logPos.getY(), logPos.getZ() + 0.5, speed);
    }

    @Override
    public void stop() {
        scratchTimer = 0;
        logPos = null;
        civet.getNavigation().stop();
        civet.setScratching(false);
    }

    @Override
    public void tick() {
        if (logPos == null) return;

        double distSq = civet.distanceToSqr(logPos.getX() + 0.5, civet.getY(), logPos.getZ() + 0.5);

        if (distSq > 0.80D) {
            civet.setScratching(false);
            if (!civet.getNavigation().isInProgress()) {
                civet.getNavigation().moveTo(logPos.getX() + 0.5, logPos.getY(), logPos.getZ() + 0.5, speed);
            }
            return;
        }

        civet.getNavigation().stop();
        civet.getLookControl().setLookAt(logPos.getX() + 0.5, logPos.getY() + 0.5, logPos.getZ() + 0.5);

        if (scratchTimer == 0) {
            civet.setDeltaMovement(Vec3.ZERO);
        }

        civet.setScratching(true);
        scratchTimer++;

        if (scratchTimer >= TOTAL_DURATION) {
            civet.setEnrichment(civet.maxEnrichment());
            this.stop();
        }
    }

    private BlockPos findNearestLog() {
        BlockPos origin = civet.blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (isValidLog(pos)) {
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

    private boolean isValidLog(BlockPos pos) {
        return civet.level().getBlockState(pos).is(BlockTags.LOGS);
    }
}