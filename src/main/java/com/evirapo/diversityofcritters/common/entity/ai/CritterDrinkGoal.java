package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.util.CritterDietConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CritterDrinkGoal extends Goal {

    private final DiverseCritter critter;
    private BlockPos waterPos;
    private BlockPos targetPos;
    private int drinkTimer = 0;

    private boolean isFinishing = false;
    private int finishingTimer = 0;

    private static final float DRINK_CHANCE = 0.3F;
    private static final double MAX_DRINK_DIST_SQ = 1.2D;

    private final Direction[] HORIZONTALS = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public CritterDrinkGoal(DiverseCritter creature) {
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.critter = creature;
    }

    @Override
    public boolean canUse() {
        if (critter.isNewborn() || critter.level().isClientSide()) return false;
        if (!critter.isThirsty()) return false;

        if (critter.getRandom().nextFloat() >= DRINK_CHANCE) return false;

        waterPos = generateTarget();
        if (waterPos != null) {
            targetPos = getLandPos(waterPos);
            return targetPos != null;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (isFinishing) return finishingTimer < 5;
        return targetPos != null && !this.critter.isInWater();
    }

    @Override
    public void start() {
        drinkTimer = 0;
        isFinishing = false;
        finishingTimer = 0;
    }

    @Override
    public void stop() {
        targetPos = null;
        waterPos = null;
        drinkTimer = 0;
        isFinishing = false;
        finishingTimer = 0;
        this.critter.setDrinkPos(null);
        this.critter.setIsDrinking(false);
        this.critter.getNavigation().stop();
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

        if (targetPos != null && waterPos != null) {
            double dist = this.critter.distanceToSqr(Vec3.atCenterOf(waterPos));

            if (dist > (MAX_DRINK_DIST_SQ + 2.0D) && this.critter.IsDrinking()) {
                this.critter.setIsDrinking(false);
                this.critter.setDrinkPos(null);
            }

            if (dist <= MAX_DRINK_DIST_SQ) {
                double d0 = waterPos.getX() + 0.5D - this.critter.getX();
                double d2 = waterPos.getZ() + 0.5D - this.critter.getZ();
                float yaw = (float)(Mth.atan2(d2, d0) * (double)Mth.RAD_TO_DEG) - 90.0F;

                this.critter.setYRot(yaw);
                this.critter.yHeadRot = yaw;
                this.critter.yBodyRot = yaw;

                this.critter.getNavigation().stop();
                this.critter.setIsDrinking(true);
                this.critter.setDrinkPos(waterPos);

                this.critter.getLookControl().setLookAt(
                        this.waterPos.getX() + 0.5D,
                        this.critter.getEyeY(),
                        this.waterPos.getZ() + 0.5D
                );

                drinkTimer++;

                if (drinkTimer >= 5 && (drinkTimer - 5) % 20 == 10) {
                    this.critter.setThirst(this.critter.maxThirst());
                    this.critter.gameEvent(GameEvent.BLOCK_ACTIVATE);
                    this.critter.playSound(SoundEvents.GENERIC_SWIM, 0.7F, 0.5F + critter.getRandom().nextFloat());
                }

                if (critter.getThirst() >= critter.maxThirst()) {
                    this.critter.setIsDrinking(false);
                    this.isFinishing = true;
                }

            } else {
                this.critter.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, 1.2D);
            }
        }
    }

    public BlockPos generateTarget() {
        BlockPos closestPos = null;
        double closestDistSq = Double.MAX_VALUE;
        final RandomSource random = this.critter.getRandom();

        int range = 24;

        for (int i = 0; i < 30; i++) {
            BlockPos blockpos1 = this.critter.blockPosition().offset(
                    random.nextInt(range) - range / 2,
                    random.nextInt(8) - 4,
                    random.nextInt(range) - range / 2
            );

            while (this.critter.level().isEmptyBlock(blockpos1)
                    && blockpos1.getY() > critter.level().getMinBuildHeight()) {
                blockpos1 = blockpos1.below();
            }

            if (isConnectedToLand(blockpos1)) {
                double distSq = this.critter.blockPosition().distSqr(blockpos1);

                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestPos = blockpos1;
                }
            }
        }
        return closestPos;
    }

    public boolean isConnectedToLand(BlockPos pos) {
        if (this.critter.level().getFluidState(pos).is(FluidTags.WATER)) {
            for (Direction dir : HORIZONTALS) {
                BlockPos offsetPos = pos.relative(dir);
                if (this.critter.level().getFluidState(offsetPos).isEmpty()
                        && this.critter.level().getFluidState(offsetPos.above()).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public BlockPos getLandPos(BlockPos pos) {
        if (this.critter.level().getFluidState(pos).is(FluidTags.WATER)) {
            for (Direction dir : HORIZONTALS) {
                BlockPos offsetPos = pos.relative(dir);
                if (this.critter.level().getFluidState(offsetPos).isEmpty()
                        && this.critter.level().getFluidState(offsetPos.above()).isEmpty()) {
                    return offsetPos;
                }
            }
        }
        return null;
    }
}