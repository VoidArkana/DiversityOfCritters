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
    private int cooldown = 0;
    private static final int EAT_INTERVAL = 40;

    private static final double MAX_EAT_DIST_SQ = 2.8D;

    private int navigationRetryTimer = 0;
    private static final int NAV_RETRY_INTERVAL = 40;

    public FindFoodBowlGoal(DiverseCritter critter, double speed, int searchRadius) {
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
        navigationRetryTimer = 0;
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

        boolean isCloseEnough = critter.getBoundingBox().inflate(0.32D).intersects(new net.minecraft.world.phys.AABB(bowlPos));
        Vec3 bowlCenter = Vec3.atBottomCenterOf(bowlPos);

        if (!isCloseEnough) {
            critter.setIsDrinking(false);
            // Retry navigation periodically — the path may have been cancelled
            // by stuck detection or failed the first time (e.g. civet was on
            // an elevated surface and the path needed to be rescheduled once
            // the entity started moving toward the wall).
            navigationRetryTimer++;
            if (!critter.getNavigation().isInProgress() || navigationRetryTimer >= NAV_RETRY_INTERVAL) {
                navigationRetryTimer = 0;
                critter.getNavigation().moveTo(bowlCenter.x, bowlPos.getY(), bowlCenter.z, speed);
            }
            return;
        }

        critter.setIsDrinking(true);
        critter.getNavigation().stop();

        Vec3 dm = critter.getDeltaMovement();
        critter.setDeltaMovement(0, dm.y, 0);

        critter.getLookControl().setLookAt(
                bowlCenter.x, bowlPos.getY() + 0.1D, bowlCenter.z
        );

        eatTimer++;
        
        boolean hasFood = BowlFeedingHelper.hasFoodFor(critter, (Level) critter.level(), bowlPos);
        if (!hasFood) {
            critter.setIsDrinking(false);
            this.cooldown = 100;
            this.stop();
            return;
        }

        if (eatTimer % EAT_INTERVAL == 0) {
            boolean ate = BowlFeedingHelper.consumeFoodFor(critter, (Level) critter.level(), bowlPos);
            if (!ate || critter.getHunger() >= critter.maxHunger()) {
                this.stop();
            }
        }
    }

    @Nullable
    public BlockPos getBowlPos() {
        return this.bowlPos;
    }

    @Nullable
    private BlockPos findNearestFoodBowl() {
        Level level = (Level) critter.level();
        BlockPos origin = critter.blockPosition();

        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        int r = this.searchRadius;
        // Use full vertical range so civets on elevated surfaces (e.g. top of
        // a climbable log) can still detect bowls on the ground below them.
        int verticalRange = r;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-r, -verticalRange, -r),
                origin.offset(r,  verticalRange, r))) {
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