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
    private int cooldown = 0;

    private boolean isFinishing = false;
    private int finishingTimer = 0;

    public FindWaterBowlGoal(DiverseCritter critter, double speed, int searchRadius) {
        this.critter = critter;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
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
        if (isFinishing) return finishingTimer < 5;

        return true;
    }

    @Override
    public void start() {
        drinkTimer = 0;
        isFinishing = false;
        finishingTimer = 0;
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
        isFinishing = false;
        finishingTimer = 0;
        critter.setIsDrinking(false);
        critter.setDrinkPos(null);
        bowlPos = null;
        critter.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (isFinishing) {
            finishingTimer++;
            if (finishingTimer >= 5) {
                this.stop();
            }
            return;
        }

        if (bowlPos == null) return;

        boolean isCloseEnough = critter.getBoundingBox().inflate(0.32D).intersects(new net.minecraft.world.phys.AABB(bowlPos));
        Vec3 bowlCenter = Vec3.atBottomCenterOf(bowlPos);

        if (!isCloseEnough) {
            critter.setIsDrinking(false);
            critter.setDrinkPos(null);

            if (!critter.getNavigation().isInProgress()) {
                critter.getNavigation().moveTo(bowlCenter.x, bowlPos.getY(), bowlCenter.z, speed);
            }
            return;
        }

        critter.setIsDrinking(true);
        critter.setDrinkPos(bowlPos);
        critter.getNavigation().stop();

        Vec3 dm = critter.getDeltaMovement();
        critter.setDeltaMovement(0, dm.y, 0);

        critter.getLookControl().setLookAt(bowlCenter.x, critter.getEyeY(), bowlCenter.z);

        drinkTimer++;

        boolean hasWater = BowlFeedingHelper.hasWaterFor((Level) critter.level(), bowlPos);
        if (!hasWater) {
            critter.setIsDrinking(false);
            this.cooldown = 100;
            this.stop();
            return;
        }

        if (drinkTimer >= 5 && (drinkTimer - 5) % 20 == 10) {
            BowlFeedingHelper.consumeWaterFor(critter, (Level) critter.level(), bowlPos);
        }

        if (drinkTimer >= 5 && (drinkTimer - 5) % 20 == 0) {
            if (!hasWater) {
                critter.setIsDrinking(false);
                isFinishing = true;
            }
        }

        if (critter.getThirst() >= critter.maxThirst()) {
            critter.setIsDrinking(false);
            isFinishing = true;
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