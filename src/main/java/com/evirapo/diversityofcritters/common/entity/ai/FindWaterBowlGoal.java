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

public class FindWaterBowlGoal extends Goal {

    private final DiverseCritter critter;
    private final double speed;
    private final int searchRadius;

    private BlockPos bowlPos;
    private int drinkTimer = 0;
    private static final int DRINK_INTERVAL = 40;

    private static final double MAX_DRINK_DIST_SQ = 1.0D;

    public FindWaterBowlGoal(DiverseCritter critter, double speed, int searchRadius) {
        this.critter = critter;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (critter.level().isClientSide()) return false;

        if (!critter.isThirsty()) return false;

        bowlPos = findNearestWaterBowl();
        if (bowlPos != null) {
            if (DiverseCritter.DEBUG_BOWL_GOALS) {
                System.out.println("[WATER-BOWL-GOAL] canUse=TRUE pos=" + critter.blockPosition()
                        + " thirst=" + critter.getThirst()
                        + " bowl=" + bowlPos.toShortString());
            }
            return true;
        }
        return false;
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

        critter.debugGoalMessage("FindWaterBowlGoal", "START");
    }

    @Override
    public void stop() {
        drinkTimer = 0;
        critter.setIsDrinking(false);
        critter.setDrinkPos(null);
        bowlPos = null;
        critter.getNavigation().stop();
        critter.debugGoalMessage("FindWaterBowlGoal", "STOP");
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
                bowlCenter.x,
                bowlCenter.y + 0.1D,
                bowlCenter.z
        );

        drinkTimer++;
        if (drinkTimer % DRINK_INTERVAL == 0) {
            if (DiverseCritter.DEBUG_BOWL_GOALS) {
                System.out.println("[WATER-BOWL-GOAL] Intentando beber en " + bowlPos +
                        " thirst=" + critter.getThirst());
            }

            boolean drank = BowlFeedingHelper.consumeWaterFor(critter, (Level) critter.level(), bowlPos);

            if (DiverseCritter.DEBUG_BOWL_GOALS) {
                System.out.println("[WATER-BOWL-GOAL] DRANK=" + drank +
                        " newThirst=" + critter.getThirst());
            }

            if (!drank || critter.getThirst() >= critter.maxThirst()) {
                if (DiverseCritter.DEBUG_BOWL_GOALS) {
                    System.out.println("[WATER-BOWL-GOAL] Fin de bebida (sin agua o lleno).");
                }
                this.stop();
            }
        }
    }

    @Nullable
    private BlockPos findNearestWaterBowl() {
        Level level = (Level) critter.level();
        BlockPos origin = critter.blockPosition();
        RandomSource random = critter.getRandom();

        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        int r = this.searchRadius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!BowlFeedingHelper.hasWaterFor(level, pos)) continue;

                    double distSq = origin.distSqr(pos);
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        bestPos = pos;
                    }
                }
            }
        }

        return bestPos;
    }
}
