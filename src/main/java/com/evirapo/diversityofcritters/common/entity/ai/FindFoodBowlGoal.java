package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.util.BowlFeedingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class FindFoodBowlGoal extends Goal {

    private final DiverseCritter critter;
    private final double speed;
    private final int searchRadius;

    private BlockPos bowlPos;
    private int eatTimer = 0;
    private static final int EAT_INTERVAL = 40;

    private static final double MAX_EAT_DIST_SQ = 2.8D;

    public FindFoodBowlGoal(DiverseCritter critter, double speed, int searchRadius) {
        this.critter = critter;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (critter.isNewborn()) return false;
        if (critter.level().isClientSide()) return false;
        if (!critter.isHungry()) return false;

        bowlPos = findNearestFoodBowl();
        return bowlPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (critter.level().isClientSide()) return false;
        if (bowlPos == null) return false;

        boolean hasFood = BowlFeedingHelper.hasFoodFor(critter, (Level) critter.level(), bowlPos);
        if (!hasFood) return false;

        return critter.getHunger() < critter.maxHunger();
    }

    @Override
    public void start() {
        eatTimer = 0;
        critter.setIsDrinking(false);
        if (bowlPos != null) {
            Vec3 center = Vec3.atCenterOf(bowlPos);
            critter.getNavigation().moveTo(center.x, center.y, center.z, speed);
        }
    }

    @Override
    public void stop() {
        eatTimer = 0;
        critter.setIsDrinking(false);
        bowlPos = null;
        critter.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (bowlPos == null) return;

        Vec3 bowlCenter = Vec3.atCenterOf(bowlPos);
        double distSq = critter.distanceToSqr(bowlCenter);

        if (distSq > MAX_EAT_DIST_SQ) {
            critter.setIsDrinking(false);
            if (!critter.getNavigation().isInProgress()) {
                critter.getNavigation().moveTo(bowlCenter.x, bowlCenter.y, bowlCenter.z, speed);
            }
            return;
        }

        critter.setIsDrinking(true);
        critter.getNavigation().stop();

        Vec3 dm = critter.getDeltaMovement();
        critter.setDeltaMovement(0, dm.y, 0);

        critter.getLookControl().setLookAt(
                bowlCenter.x, bowlCenter.y + 0.1D, bowlCenter.z
        );

        eatTimer++;
        if (eatTimer % EAT_INTERVAL == 0) {
            boolean ate = BowlFeedingHelper.consumeFoodFor(critter, (Level) critter.level(), bowlPos);
            if (!ate || critter.getHunger() >= critter.maxHunger()) {
                this.stop();
            }
        }
    }

    @Nullable
    private BlockPos findNearestFoodBowl() {
        Level level = (Level) critter.level();
        BlockPos origin = critter.blockPosition();

        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        int r = this.searchRadius;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -2, -r), origin.offset(r, 2, r))) {
            if (BowlFeedingHelper.hasFoodFor(critter, level, pos)) {
                double distSq = origin.distSqr(pos);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestPos = pos.immutable();
                }
            }
        }
        return bestPos;
    }
}