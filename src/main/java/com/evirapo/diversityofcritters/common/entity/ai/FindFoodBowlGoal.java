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

    // Aumentamos un poco la distancia aceptada.
    // Así el goal considera "llegado" antes de empujarla justo al centro del bowl.
    @Override
    public double acceptedDistance() {
        return 1.4D; // prueba 1.4; si aún se mete encima, súbelo a 1.6
    }

    @Override
    public boolean canUse() {
        if (critter.level().isClientSide()) return false;
        if (!critter.isHungry()) return false;
        if (!critter.canMove())  return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (critter.level().isClientSide()) return false;
        if (!critter.canMove()) return false;

        if (!this.isReachedTarget()) {
            // Aún no llega → sigue pathing mientras tenga hambre
            return super.canContinueToUse() && critter.isHungry();
        }

        // Ya está lo bastante cerca del bowl
        boolean hasFood = BowlFeedingHelper.hasFoodFor(critter, (Level) critter.level(), this.blockPos);
        boolean notFull = critter.getHunger() < critter.maxHunger();
        return hasFood && notFull;
    }

    @Override
    public void start() {
        super.start();
        eatTimer = 0;
        critter.setIsDrinking(true); // usamos esto para la anim de comer/beber
        critter.debugGoalMessage("FindFoodBowlGoal", "START");
    }

    @Override
    public void stop() {
        super.stop();
        eatTimer = 0;
        critter.setIsDrinking(false);
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
        // si aún no estamos dentro de acceptedDistance() → deja que MoveToBlockGoal haga su trabajo
        if (!this.isReachedTarget()) {
            super.tick();
            return;
        }

        // --- YA ESTAMOS LO BASTANTE CERCA DEL BOWL ---

        // 1) detener navegación y movimiento lateral
        critter.getNavigation().stop();
        critter.setDeltaMovement(0, critter.getDeltaMovement().y, 0);

        // 2) mira al bowl para evitar giros raros
        Vec3 bowlCenter = Vec3.atCenterOf(this.blockPos);
        critter.getLookControl().setLookAt(
                bowlCenter.x,
                bowlCenter.y + 0.1D,
                bowlCenter.z
        );

        // 3) comer cada cierto tiempo
        eatTimer++;
        if (eatTimer % EAT_INTERVAL == 0) {
            System.out.println("[BOWL-GOAL] Intentando comer en " + this.blockPos +
                    " hunger=" + critter.getHunger());

            boolean ate = BowlFeedingHelper.consumeFoodFor(critter, (Level) critter.level(), this.blockPos);

            System.out.println("[BOWL-GOAL] ATE=" + ate +
                    " newHunger=" + critter.getHunger());

            if (!ate || critter.getHunger() >= critter.maxHunger()) {
                System.out.println("[BOWL-GOAL] Fin de comida (sin comida o lleno).");
                this.stop();
            }
        }
    }
}



