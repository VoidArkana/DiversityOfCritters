package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.util.BowlFeedingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public class FindFoodBowlGoal extends MoveToBlockGoal {

    private final DiverseCritter critter;
    private int eatTimer = 0;
    private static final int EAT_INTERVAL = 40; // 2 segundos

    public FindFoodBowlGoal(DiverseCritter critter, double speed, int searchRadius) {
        super(critter, speed, searchRadius);
        this.critter = critter;
    }

    @Override
    public double acceptedDistance() {
        // Consideramos "llegado" algo antes de estar encima del bowl
        return 1.4D;
    }

    @Override
    public boolean canUse() {
        if (critter.level().isClientSide()) return false;

        // Solo si tiene hambre
        if (!critter.isHungry()) return false;

        boolean base = super.canUse();
        if (base) {
            System.out.println("[BOWL-GOAL] canUse=TRUE pos=" + critter.blockPosition()
                    + " hunger=" + critter.getHunger());
        }
        return base;
    }

    @Override
    public boolean canContinueToUse() {
        if (critter.level().isClientSide()) return false;

        // Si aún NO llega, seguimos mientras el MoveToBlockGoal quiera
        if (!this.isReachedTarget()) {
            return super.canContinueToUse() && critter.isHungry();
        }

        // Ya estamos lo bastante cerca del bowl:
        // seguimos mientras el bowl tenga comida y no esté lleno de hunger
        boolean hasFood = BowlFeedingHelper.hasFoodFor(critter, (Level) critter.level(), this.blockPos);
        boolean notFull = critter.getHunger() < critter.maxHunger();
        return hasFood && notFull;
    }

    @Override
    public void start() {
        super.start();
        eatTimer = 0;
        critter.debugGoalMessage("FindFoodBowlGoal", "START");
        critter.setIsDrinking(false); // 👈 todavía no bloqueamos, hasta llegar
    }

    @Override
    public void stop() {
        super.stop();
        eatTimer = 0;
        critter.setIsDrinking(false); // 👈 desbloquear al terminar
        critter.debugGoalMessage("FindFoodBowlGoal", "STOP");
    }

    @Override
    protected boolean isValidTarget(LevelReader levelReader, BlockPos pos) {
        boolean ok = BowlFeedingHelper.hasFoodFor(critter, (Level) levelReader, pos);
        if (ok) {
            System.out.println("[BOWL-GOAL] isValidTarget=TRUE bowl=" + pos.toShortString());
        }
        return ok;
    }

    @Override
    public void tick() {
        // Mientras no esté dentro de acceptedDistance(), dejamos que MoveToBlockGoal haga el path
        if (!this.isReachedTarget()) {
            super.tick();
            return;
        }

        // ---------- YA ESTÁ LO BASTANTE CERCA DEL BOWL ----------

        // Bloqueamos movimiento real
        critter.setIsDrinking(true);
        critter.getNavigation().stop();
        Vec3 dm = critter.getDeltaMovement();
        critter.setDeltaMovement(0, dm.y, 0);

        // Mirar hacia el bowl
        Vec3 bowlCenter = Vec3.atCenterOf(this.blockPos);
        critter.getLookControl().setLookAt(
                bowlCenter.x,
                bowlCenter.y + 0.1D,
                bowlCenter.z
        );

        // Comer cada cierto tiempo
        eatTimer++;
        if (eatTimer % EAT_INTERVAL == 0) {
            System.out.println("[BOWL-GOAL] Intentando comer en " + this.blockPos +
                    " hunger=" + critter.getHunger());

            boolean ate = BowlFeedingHelper.consumeFoodFor(critter, (Level) critter.level(), this.blockPos);

            System.out.println("[BOWL-GOAL] ATE=" + ate +
                    " newHunger=" + critter.getHunger());

            if (!ate || critter.getHunger() >= critter.maxHunger()) {
                System.out.println("[BOWL-GOAL] Fin de comida (sin comida o lleno).");
                // al hacer stop(), setIsDrinking(false) y el goal termina
                this.stop();
            }
        }
    }
}

