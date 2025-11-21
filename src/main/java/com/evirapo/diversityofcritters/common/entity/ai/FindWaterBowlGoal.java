package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.util.BowlFeedingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

public class FindWaterBowlGoal extends MoveToBlockGoal {

    private final DiverseCritter critter;
    private int drinkTicks;
    private static final int DRINK_DURATION = 40; // ~2 segundos

    public FindWaterBowlGoal(DiverseCritter critter, double speedModifier, int searchRange) {
        super(critter, speedModifier, searchRange);
        this.critter = critter;
    }

    @Override
    public boolean canUse() {
        if (critter.level().isClientSide()) return false;

        boolean thirsty = critter.isThirsty();
        boolean canMove = critter.canMove();

        critter.debugGoalMessage("FindWaterBowlGoal", "canUse? thirsty=" + thirsty + " canMove=" + canMove);

        if (!thirsty) return false;
        if (!canMove) return false;

        boolean superUse = super.canUse();
        critter.debugGoalMessage("FindWaterBowlGoal", "super.canUse()=" + superUse);

        return superUse;
    }

    @Override
    public boolean canContinueToUse() {
        if (critter.level().isClientSide()) return false;

        boolean thirsty = critter.isThirsty();
        boolean canMove = critter.canMove();

        if (!thirsty) return false;
        if (!canMove) return false;

        boolean cont = super.canContinueToUse();
        critter.debugGoalMessage("FindWaterBowlGoal",
                "canContinueToUse=" + cont + " thirsty=" + thirsty + " canMove=" + canMove);
        return cont;
    }

    @Override
    public void start() {
        super.start();
        drinkTicks = 0;
        critter.setIsDrinking(true);
        critter.debugGoalMessage("FindWaterBowlGoal", "START at target=" + this.blockPos);
    }

    @Override
    public void stop() {
        super.stop();
        drinkTicks = 0;
        critter.setIsDrinking(false);
        critter.debugGoalMessage("FindWaterBowlGoal", "STOP");
    }

    @Override
    protected boolean isValidTarget(LevelReader levelReader, BlockPos pos) {
        boolean hasWater = BowlFeedingHelper.hasWater((Level) levelReader, pos);
        if (hasWater) {
            critter.debugGoalMessage("FindWaterBowlGoal",
                    "isValidTarget pos=" + pos.toShortString() + " hasWater=" + hasWater);
        }
        return hasWater;
    }

    private boolean isAtBowlCenter() {
        double cx = blockPos.getX() + 0.5D;
        double cy = blockPos.getY() + 1.0D;
        double cz = blockPos.getZ() + 0.5D;

        double dist2 = critter.distanceToSqr(cx, cy, cz);
        return dist2 <= (1.5D * 1.5D);
    }

    @Override
    public void tick() {
        if (!isAtBowlCenter()) {
            super.tick();

            double cx = blockPos.getX() + 0.5D;
            double cy = blockPos.getY() + 1.0D;
            double cz = blockPos.getZ() + 0.5D;
            double dist2 = critter.distanceToSqr(cx, cy, cz);

            critter.debugGoalMessage("FindWaterBowlGoal",
                    "MOVING dist2=" + String.format("%.3f", dist2));
            return;
        }

        // Ya está cerca del bowl
        this.critter.getNavigation().stop();
        var vel = critter.getDeltaMovement();
        critter.setDeltaMovement(0.0D, vel.y, 0.0D);

        drinkTicks++;
        critter.debugGoalMessage("FindWaterBowlGoal",
                "DRINKING tick=" + drinkTicks + "/" + DRINK_DURATION + " at " + blockPos.toShortString());

        if (drinkTicks >= DRINK_DURATION) {
            boolean drank = BowlFeedingHelper.consumeWaterFor(critter, (Level) critter.level(), this.blockPos);
            critter.debugGoalMessage("FindWaterBowlGoal",
                    "DRANK=" + drank + " thirst=" + critter.getThirst());
            this.stop();
        }
    }
}
