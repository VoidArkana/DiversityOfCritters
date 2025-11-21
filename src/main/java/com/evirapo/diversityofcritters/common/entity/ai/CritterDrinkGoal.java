package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
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
    private int drinkTime = 0;

    // ---- NUEVO: probabilidad de intentar beber (70%) ----
    private static final float DRINK_CHANCE = 0.7F;

    private int executionChance = 30; // (sigue aquí por si luego lo quieres usar)
    private Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public CritterDrinkGoal(DiverseCritter creature) {
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.critter = creature;
    }

    @Override
    public boolean canUse() {
        // *** IMPORTANTE: se mantiene EXACTAMENTE tu lógica actual ***
        // Solo entra si NO está sediento
        if (critter.isThirsty()) {
            return false;
        }

        // ---- Probabilidad: solo 70% de las veces que pasa por aquí intentará beber ----
        // random.nextFloat() devuelve [0.0, 1.0)
        // Permitimos el goal solo si < 0.7  →  70% de probabilidad
        if (critter.getRandom().nextFloat() >= DRINK_CHANCE) {
            return false;
        }

        waterPos = generateTarget();
        if (waterPos != null) {
            targetPos = getLandPos(waterPos);
            return targetPos != null;
        }

        return false;
    }

    @Override
    public void stop() {
        targetPos = null;
        waterPos = null;
        drinkTime = 0;
        this.critter.setDrinkPos(null);
        this.critter.setIsDrinking(false);
        this.critter.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPos != null && waterPos != null) {
            double dist = this.critter.distanceToSqr(Vec3.atCenterOf(waterPos));
            if (dist > 2 && this.critter.IsDrinking()) {
                this.critter.setIsDrinking(false);
            }
            if (dist <= 1F) {
                double d0 = waterPos.getX() + 0.5D - this.critter.getX();
                double d2 = waterPos.getZ() + 0.5D - this.critter.getZ();
                float yaw = (float)(Mth.atan2(d2, d0) * (double) Mth.RAD_TO_DEG) - 90.0F;
                this.critter.setYRot(yaw);
                this.critter.yHeadRot = yaw;
                this.critter.yBodyRot = yaw;
                this.critter.getNavigation().stop();
                this.critter.setIsDrinking(true);
                this.critter.setDrinkPos(waterPos);
                int prevThirst = critter.getThirst();
                this.critter.setThirst(Math.min(prevThirst + (critter.maxThirst()/50), critter.maxThirst()));
                if(drinkTime % 10 == 0){
                    this.critter.gameEvent(GameEvent.BLOCK_ACTIVATE);
                    this.critter.playSound(SoundEvents.GENERIC_SWIM, 0.7F, 0.5F + critter.getRandom().nextFloat());
                }
                this.critter.getLookControl().setLookAt(
                        (double)this.waterPos.getX() + 0.5D,
                        (double)(this.waterPos.getY()-1.5),
                        (double)this.waterPos.getZ() + 0.5D,
                        10.0F,
                        (float)this.critter.getMaxHeadXRot()
                );
                drinkTime++;
                if(drinkTime > 100){
                    this.stop();
                }
            } else {
                this.critter.getNavigation().moveTo(
                        waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.2D);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if(critter.getThirst()>=critter.maxThirst()){
            return false;
        }
        return targetPos != null && !this.critter.isInWater();
    }

    public BlockPos generateTarget() {
        BlockPos blockpos = null;
        final RandomSource random = this.critter.getRandom();
        int range = 32;
        for (int i = 0; i < 15; i++) {
            BlockPos blockpos1 = this.critter.blockPosition().offset(
                    random.nextInt(range) - range / 2,
                    3,
                    random.nextInt(range) - range / 2
            );
            while (this.critter.level().isEmptyBlock(blockpos1)
                    && blockpos1.getY() > critter.level().getMinBuildHeight()) {
                blockpos1 = blockpos1.below();
            }
            if (isConnectedToLand(blockpos1)) {
                blockpos = blockpos1;
            }
        }
        return blockpos;
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
