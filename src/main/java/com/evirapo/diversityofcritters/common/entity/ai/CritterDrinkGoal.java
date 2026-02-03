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
    private int drinkTime = 0;

    private static final float DRINK_CHANCE = 0.3F;
    // CAMBIO: Aumentamos el rango de interacción (3.0D = 1.73 bloques de distancia)
    // Esto evita que se quede atascado intentando entrar al bloque de agua.
    private static final double MAX_DRINK_DIST_SQ = 3.0D;

    private final Direction[] HORIZONTALS = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public CritterDrinkGoal(DiverseCritter creature) {
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.critter = creature;
    }

    @Override
    public boolean canUse() {
        if (critter.isNewborn()) return false;
        if (critter.level().isClientSide()) return false;
        if (critter.getThirst() >= critter.maxThirst()) return false;
        if (critter.getRandom().nextFloat() >= DRINK_CHANCE) return false;

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

            // Si se aleja mucho, cancelar
            if (dist > (MAX_DRINK_DIST_SQ + 2.0D) && this.critter.IsDrinking()) {
                this.critter.setIsDrinking(false);
                this.critter.setDrinkPos(null);
            }

            // CAMBIO: Usamos la nueva constante MAX_DRINK_DIST_SQ en lugar de 1.0F
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

                drinkTime++;

                if (drinkTime % 10 == 0) {
                    CritterDietConfig diet = critter.getDietConfig();
                    int restorePerSip = diet.thirstPerWaterBowl;

                    int prevThirst = critter.getThirst();
                    this.critter.setThirst(
                            Math.min(prevThirst + restorePerSip, critter.maxThirst())
                    );

                    this.critter.gameEvent(GameEvent.BLOCK_ACTIVATE);
                    this.critter.playSound(
                            SoundEvents.GENERIC_SWIM,
                            0.7F,
                            0.5F + critter.getRandom().nextFloat()
                    );
                }

                // Mirar al agua
                this.critter.getLookControl().setLookAt(
                        (double)this.waterPos.getX() + 0.5D,
                        (double)(this.waterPos.getY()), // Ajuste visual ligero
                        (double)this.waterPos.getZ() + 0.5D,
                        10.0F,
                        (float)this.critter.getMaxHeadXRot()
                );

                if (drinkTime > 100 || critter.getThirst() >= critter.maxThirst()) {
                    this.stop();
                }
            } else {
                // Si aún no llega, moverse hacia el TARGET (tierra firme), no hacia el agua directamente
                // para evitar caerse dentro si es profundo.
                this.critter.getNavigation().moveTo(
                        targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, 1.2D
                );
            }
        }
    }


    @Override
    public boolean canContinueToUse() {
        if (critter.getThirst() >= critter.maxThirst()) {return false;}
        return targetPos != null && !this.critter.isInWater();
    }

    // --------- Búsqueda de agua natural ---------

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
