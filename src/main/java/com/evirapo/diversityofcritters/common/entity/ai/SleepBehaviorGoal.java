package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter.SleepState;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SleepBehaviorGoal extends Goal {
    private final DiverseCritter entity;
    private int timer;
    private int prepareDuration;
    private int wakeDuration;

    public SleepBehaviorGoal(DiverseCritter entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (entity.isInWater() || entity.isVehicle() || entity.isCleaning() || entity.isOrderedToSit()) { //Attacking
            return false;
        }

        // Lógica de Horario
        long time = entity.level().getDayTime() % 24000;
        boolean isNight = time >= 13000 && time < 23000;

        // Si es diurno duerme de noche, si es nocturno duerme de día
        return entity.isDiurnal() ? isNight : !isNight;
    }

    @Override
    public boolean canContinueToUse() {
        // 1. Si estamos despertando, seguimos hasta que acabe la animación
        if (entity.getSleepState() == SleepState.AWAKENING) {
            return timer < wakeDuration;
        }

        // 2. Interrupciones: Daño o Agua
        if (entity.hurtTime > 0 || entity.isInWater()) {
            startWaking();
            return true; // Continuamos para procesar el despertar
        }

        // 3. Chequeo de Horario (Despertar si cambió el día)
        long time = entity.level().getDayTime() % 24000;
        boolean isNight = time >= 13000 && time < 23000;
        boolean shouldBeAsleep = entity.isDiurnal() ? isNight : !isNight;

        if (!shouldBeAsleep && entity.getSleepState() == SleepState.SLEEPING) {
            startWaking();
            return true;
        }

        return true;
    }

    @Override
    public void start() {
        entity.getNavigation().stop();
        this.prepareDuration = entity.getPreparingSleepDuration();
        this.wakeDuration = entity.getAwakeningDuration();

        // INICIO: Transición a PREPARING
        entity.setSleepState(SleepState.PREPARING);
        this.timer = 0;
    }

    @Override
    public void stop() {
        // Al terminar (por cualquier razón), estado AWAKE
        entity.setSleepState(SleepState.AWAKE);
    }

    @Override
    public void tick() {
        timer++;

        SleepState current = entity.getSleepState();

        switch (current) {
            case PREPARING:
                // Transición: Preparing -> Sleeping
                if (timer >= prepareDuration) {
                    entity.setSleepState(SleepState.SLEEPING);
                    timer = 0;
                }
                break;

            case SLEEPING:
                // En este estado simplemente esperamos.
                // Las condiciones de salida están en canContinueToUse()
                entity.getNavigation().stop();
                break;

            case AWAKENING:
                // Transición: Awakening -> Fin del Goal (stop() pone AWAKE)
                if (timer >= wakeDuration) {
                    // No hacemos nada, canContinueToUse devolverá false en el siguiente tick
                }
                break;

            default:
                break;
        }
    }

    private void startWaking() {
        if (entity.getSleepState() != SleepState.AWAKENING) {
            entity.setSleepState(SleepState.AWAKENING);
            timer = 0;
        }
    }
}