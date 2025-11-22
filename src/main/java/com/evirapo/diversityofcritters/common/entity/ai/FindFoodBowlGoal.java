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

    private static final double MAX_EAT_DIST_SQ = 1.0D;

    public FindFoodBowlGoal(DiverseCritter critter, double speed, int searchRadius) {
        this.critter = critter;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (critter.level().isClientSide()) return false;

        if (!critter.isHungry()) return false;

        bowlPos = findNearestFoodBowl();
        if (bowlPos != null) {
            if (DiverseCritter.DEBUG_BOWL_GOALS) {
                System.out.println("[BOWL-GOAL] canUse=TRUE pos=" + critter.blockPosition()
                        + " hunger=" + critter.getHunger()
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

        critter.debugGoalMessage("FindFoodBowlGoal", "START");
    }

    @Override
    public void stop() {
        eatTimer = 0;
        critter.setIsDrinking(false);
        bowlPos = null;
        critter.getNavigation().stop();
        critter.debugGoalMessage("FindFoodBowlGoal", "STOP");
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
                bowlCenter.x,
                bowlCenter.y + 0.1D,
                bowlCenter.z
        );

        eatTimer++;
        if (eatTimer % EAT_INTERVAL == 0) {
            if (DiverseCritter.DEBUG_BOWL_GOALS) {
                System.out.println("[BOWL-GOAL] Intentando comer en " + bowlPos +
                        " hunger=" + critter.getHunger());
            }

            boolean ate = BowlFeedingHelper.consumeFoodFor(critter, (Level) critter.level(), bowlPos);

            if (DiverseCritter.DEBUG_BOWL_GOALS) {
                System.out.println("[BOWL-GOAL] ATE=" + ate +
                        " newHunger=" + critter.getHunger());
            }

            if (!ate || critter.getHunger() >= critter.maxHunger()) {
                if (DiverseCritter.DEBUG_BOWL_GOALS) {
                    System.out.println("[BOWL-GOAL] Fin de comida (sin comida o lleno).");
                }
                this.stop();
            }
        }
    }

    @Nullable
    private BlockPos findNearestFoodBowl() {
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
                    if (!BowlFeedingHelper.hasFoodFor(critter, level, pos)) continue;

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
