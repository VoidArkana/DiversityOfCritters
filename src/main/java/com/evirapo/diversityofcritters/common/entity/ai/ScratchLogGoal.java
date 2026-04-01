package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ScratchLogGoal extends Goal {
    private final CivetEntity civet;
    private final double speed;
    private final int searchRadius;
    private BlockPos logPos;
    private int scratchTimer = 0;

    private int searchCooldown = 0;

    private static final int TOTAL_DURATION = 100;

    public ScratchLogGoal(CivetEntity civet, double speed, int searchRadius) {
        this.civet = civet;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }

        if (civet.level().isClientSide() || !civet.isEnrichmentNeeded() || civet.isNewborn()) {
            return false;
        }

        logPos = findNearestLog();

        if (logPos == null) {
            searchCooldown = 40;
            return false;
        }

        return true;
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

        net.minecraft.world.phys.AABB logBox = new net.minecraft.world.phys.AABB(logPos);
        boolean isTouchingLog = civet.getBoundingBox().inflate(0.05D).intersects(logBox);

        civet.getLookControl().setLookAt(logPos.getX() + 0.5, logPos.getY() + 0.5, logPos.getZ() + 0.5);

        if (!isTouchingLog) {
            civet.setScratching(false);
            scratchTimer = 0;

            if (!civet.getNavigation().isInProgress()) {

                Vec3 civetPos = civet.position();
                Vec3 targetPos = new Vec3(logPos.getX() + 0.5, civet.getY(), logPos.getZ() + 0.5);

                Vec3 direction = targetPos.subtract(civetPos).normalize().scale(0.05D);

                civet.setDeltaMovement(direction.x, civet.getDeltaMovement().y, direction.z);
            }
            return;
        }

        civet.getNavigation().stop();

        Vec3 dm = civet.getDeltaMovement();
        civet.setDeltaMovement(0, dm.y, 0);

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