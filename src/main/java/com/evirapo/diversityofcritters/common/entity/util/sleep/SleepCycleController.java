package com.evirapo.diversityofcritters.common.entity.util.sleep;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class SleepCycleController<T extends DiverseCritter & ISleepingEntity> {

    // === DEBUG ===
    private static final boolean DEBUG_SLEEP = false;
    private void dlog(String msg) {
        if (DEBUG_SLEEP && !entity.level().isClientSide()) {
            System.out.println("[SLEEP][SVR][" + entity.getName().getString() + "#" + entity.getId() + "] " + msg);
        }
    }

    private final T entity;
    private final AnimationState preparingSleepState;
    private final AnimationState sleepState;
    private final AnimationState awakeningState;

    private final int preparingSleepDuration;
    private final int awakeningDuration;

    private int preparingSleepTimer = -1;
    private int awakeningTimer = -1;

    // ahora significa: "¿la tick anterior estaba dentro de la ventana de dormir?"
    private boolean wasSleepTime = false;

    private static final int SLEEP_DELAY_TICKS = 100;
    private final int entityOffset;

    private int ticksSinceLastInterruption = -1;
    private int ticksSinceNoTarget = -1;

    private int preparingSleepStartTick = -1;
    private int awakeningStartTick = -1;

    public SleepCycleController(
            T entity,
            AnimationState preparingSleepState,
            AnimationState sleepState,
            AnimationState awakeningState,
            int preparingSleepDuration,
            int awakeningDuration
    ) {
        this.entity = entity;
        this.preparingSleepState = preparingSleepState;
        this.sleepState = sleepState;
        this.awakeningState = awakeningState;
        this.preparingSleepDuration = preparingSleepDuration;
        this.awakeningDuration = awakeningDuration;
        this.entityOffset = entity.getId() % 10;
    }

    public void tick(int tickCount) {
        Level level = entity.level();
        boolean isClient = level.isClientSide();

        long timeOfDay = level.getDayTime() % 24000L;
        boolean isDay   = (timeOfDay >= 0L && timeOfDay < 12000L);
        boolean isNight = (timeOfDay >= 13000L && timeOfDay < 23000L);
        boolean diurnal = entity.isDiurnal();       // true = duerme de noche, false = duerme de día
        boolean sleepTime = diurnal ? isNight : isDay;

        boolean wasSleepTimeBefore = this.wasSleepTime;
        this.wasSleepTime = sleepTime;

        if (ticksSinceLastInterruption >= 0) {
            ticksSinceLastInterruption++;
        }

        // Dump de estado cada 40 ticks (solo servidor)
        if (!isClient && (tickCount % 40) == 0) {
            dlog("diurnal=" + diurnal +
                    " time=" + timeOfDay +
                    " (isDay=" + isDay + ", isNight=" + isNight + ")" +
                    " sleepTime=" + sleepTime +
                    " state[prep=" + entity.isPreparingSleep() +
                    ", sleep=" + entity.isSleeping() +
                    ", awakeing=" + entity.isAwakeing() + "]" +
                    " timers[prep=" + preparingSleepTimer +
                    ", awake=" + awakeningTimer + "]" +
                    " cooldown[lastInterrupt=" + ticksSinceLastInterruption +
                    ", noTarget=" + ticksSinceNoTarget + "]" +
                    " target=" + (entity.getTarget()==null ? "null" : entity.getTarget().getName().getString()));
            dlog("dimension[hasSkyLight=" + level.dimensionType().hasSkyLight() +
                    ", natural=" + level.dimensionType().natural() +
                    ", fixedTime=" + (level.dimensionType().fixedTime().isPresent() ? level.dimensionType().fixedTime().getAsLong() : "none") + "]");
        }

        if (!isClient) {
            // seguimiento "sin objetivo"
            if (entity.getTarget() == null) {
                if (ticksSinceNoTarget >= 0) {
                    ticksSinceNoTarget++;
                } else {
                    ticksSinceNoTarget = 0;
                }
            } else {
                ticksSinceNoTarget = -1;
            }

            // entrar a PREPARING cuando toca dormir
            if (sleepTime && !entity.isSleeping() && !entity.isPreparingSleep()
                    && awakeningTimer < 0 && preparingSleepTimer < 0 && entity.getTarget() == null
                    && ticksSinceNoTarget >= SLEEP_DELAY_TICKS) {

                enterPreparing();
                preparingSleepTimer = preparingSleepDuration + entityOffset;
                preparingSleepStartTick = tickCount;
                ticksSinceNoTarget = -1;

                dlog("→ PREPARING start: duration=" + preparingSleepTimer);
            }

            // avance de PREPARING
            if (entity.isPreparingSleep() && preparingSleepTimer >= 0) {
                preparingSleepTimer--;
                if (preparingSleepTimer <= 0) {
                    preparingSleepTimer = -1;

                    if (!sleepTime) {
                        entity.setPreparingSleep(false);
                        dlog("✕ PREPARING cancel: sleep window ended");
                    } else if (!entity.isSleeping()) {
                        enterSleeping();
                        dlog("→ SLEEP start: preparedIn=" + (tickCount - preparingSleepStartTick) + " ticks");
                    }
                }
            }

            // amenazas cercanas
            if ((entity.isSleeping() || entity.isPreparingSleep()) && awakeningTimer < 0) {
                List<LivingEntity> threats = entity.level().getEntitiesOfClass(
                        LivingEntity.class,
                        entity.getBoundingBox().inflate(4),
                        other -> shouldInterruptSleep(entity, other)
                );
                if (!threats.isEmpty()) {
                    dlog("⚠ threats=" + threats.size());
                    interruptSleep("nearby threat", tickCount);
                }
            }

            // fin de ventana de sueño
            if (!sleepTime && wasSleepTimeBefore && entity.isSleeping() && awakeningTimer < 0) {
                interruptSleep("sleep window ended", tickCount);
            }

            // reintento de dormir tras interrupción
            if (!entity.isSleeping() && !entity.isPreparingSleep() && !entity.isAwakeing()
                    && sleepTime && ticksSinceLastInterruption >= SLEEP_DELAY_TICKS
                    && ticksSinceNoTarget >= SLEEP_DELAY_TICKS
                    && preparingSleepTimer < 0 && awakeningTimer < 0) {

                List<LivingEntity> threats = entity.level().getEntitiesOfClass(
                        LivingEntity.class,
                        entity.getBoundingBox().inflate(4),
                        other -> shouldInterruptSleep(entity, other)
                );

                if (threats.isEmpty()) {
                    enterPreparing();
                    preparingSleepTimer = preparingSleepDuration + entityOffset;
                    preparingSleepStartTick = tickCount;
                    ticksSinceLastInterruption = -1;
                    ticksSinceNoTarget = -1;

                    dlog("→ PREPARING (retry) start: duration=" + preparingSleepTimer);
                } else {
                    dlog("↻ retry blocked by threats=" + threats.size());
                }
            }

            // timer de AWAKENING
            if (awakeningTimer >= 0) {
                awakeningTimer--;
                if (awakeningTimer <= 0) {
                    if (entity.isAwakeing()) {
                        entity.setAwakeing(false);
                        dlog("✓ AWAKENING end");
                    }
                    awakeningTimer = -1;
                }
            }
        }
    }

    // decide si "toca dormir" según diurnal/nocturnal
    private boolean isSleepTime(long timeOfDay) {
        boolean isDay   = (timeOfDay >= 0L && timeOfDay < 12000L);
        boolean isNight = (timeOfDay >= 13000L && timeOfDay < 23000L);
        return entity.isDiurnal() ? isNight : isDay;
    }

    // transiciones atómicas
    private void enterPreparing() {
        entity.setPreparingSleep(true);
        entity.setSleeping(false);
        entity.setAwakeing(false);
    }

    private void enterSleeping() {
        entity.setPreparingSleep(false);
        entity.setSleeping(true);
        entity.setAwakeing(false);
    }

    private void enterAwakening() {
        entity.setPreparingSleep(false);
        entity.setSleeping(false);
        entity.setAwakeing(true);
    }

    public void interruptSleep(String reason, int tickCount) {
        if (entity.isSleeping() || entity.isPreparingSleep()) {
            enterAwakening();
            awakeningTimer = awakeningDuration + entityOffset;
            awakeningStartTick = tickCount;

            ticksSinceLastInterruption = 0;
            ticksSinceNoTarget = 0;

            dlog("→ AWAKENING start reason=" + reason + " duration=" + awakeningTimer);
        }
    }

    private boolean shouldInterruptSleep(LivingEntity sleeperEntity, LivingEntity nearbyEntity) {
        if (nearbyEntity == sleeperEntity) return false;
        if (!nearbyEntity.isAlive()) return false;

        if (nearbyEntity instanceof Player player && !player.isSpectator()) {
            if (sleeperEntity instanceof ISleepAwareness aware) {
                return aware.shouldWakeOnPlayerProximity();
            }
            // por defecto, si NO implementa ISleepAwareness, el jugador despierta
            return true;
        }

        if (sleeperEntity instanceof ISleepThreatEvaluator evaluator) {
            return evaluator.shouldInterruptSleepDueTo(nearbyEntity);
        }

        if (sleeperEntity instanceof ISleepingEntity sleeper) {
            return sleeper.getInterruptingEntityTypes().contains(nearbyEntity.getType());
        }

        return false;
    }
}

// Note: If an entity does NOT implement ISleepAwareness,
// it will default to being woken by nearby players.
// This is useful for testing and allows optional override per entity.
