package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.util.BowlFeedingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class FindWaterBowlGoal extends Goal {

    private final DiverseCritter critter;
    private final double speed;
    private final int searchRadius;

    private BlockPos bowlPos;
    private int drinkTimer = 0;
    private static final int DRINK_INTERVAL = 40;

    private static final double MAX_DRINK_DIST_SQ = 2.8D;

    public FindWaterBowlGoal(DiverseCritter critter, double speed, int searchRadius) {
        this.critter = critter;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (critter.isNewborn()) return false;
        if (critter.level().isClientSide()) return false;
        if (!critter.isThirsty()) return false;

        bowlPos = findNearestWaterBowl();
        return bowlPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (critter.level().isClientSide()) return false;
        if (bowlPos == null) return false;

        boolean hasWater = BowlFeedingHelper.hasWaterFor((Level) critter.level(), bowlPos);
        if (!hasWater) return false;

        return critter.getThirst() < critter.maxThirst();
    }

    @Override
    public void start() {
        drinkTimer = 0;
        critter.setIsDrinking(false);
        critter.setDrinkPos(null);

        if (bowlPos != null) {
            Vec3 center = Vec3.atCenterOf(bowlPos);
            critter.getNavigation().moveTo(center.x, center.y, center.z, speed);
        }
    }

    @Override
    public void stop() {
        drinkTimer = 0;
        critter.setIsDrinking(false);
        critter.setDrinkPos(null);
        bowlPos = null;
        critter.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (bowlPos == null) return;

        Vec3 bowlCenter = Vec3.atCenterOf(bowlPos);
        double distSq = critter.distanceToSqr(bowlCenter);

        if (distSq > MAX_DRINK_DIST_SQ) {
            critter.setIsDrinking(false);
            critter.setDrinkPos(null);
            if (!critter.getNavigation().isInProgress()) {
                critter.getNavigation().moveTo(bowlCenter.x, bowlCenter.y, bowlCenter.z, speed);
            }
            return;
        }

        critter.setIsDrinking(true);
        critter.setDrinkPos(bowlPos);
        critter.getNavigation().stop();

        Vec3 dm = critter.getDeltaMovement();
        critter.setDeltaMovement(0, dm.y, 0);

        critter.getLookControl().setLookAt(
                bowlCenter.x, bowlCenter.y + 0.1D, bowlCenter.z
        );

        drinkTimer++;
        if (drinkTimer % DRINK_INTERVAL == 0) {
            boolean drank = BowlFeedingHelper.consumeWaterFor(critter, (Level) critter.level(), bowlPos);
            if (!drank || critter.getThirst() >= critter.maxThirst()) {
                this.stop();
            }
        }
    }

    @Nullable
    private BlockPos findNearestWaterBowl() {
        Level level = (Level) critter.level();
        BlockPos origin = critter.blockPosition();

        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        int r = this.searchRadius;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -2, -r), origin.offset(r, 2, r))) {
            if (BowlFeedingHelper.hasWaterFor(level, pos)) {
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