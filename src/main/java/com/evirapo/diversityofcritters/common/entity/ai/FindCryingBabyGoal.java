package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class FindCryingBabyGoal extends Goal {
    private final DiverseCritter parent;
    private final double speedModifier;
    private final double searchRange;
    private DiverseCritter targetBaby;
    private int feedTimer;

    private static final int FEEDING_DURATION = 60;

    public FindCryingBabyGoal(DiverseCritter parent, double speed, double range) {
        this.parent = parent;
        this.speedModifier = speed;
        this.searchRange = range;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (parent.isBaby() || parent.isSleeping() || parent.isOrderedToSit() || parent.isPassenger()) {
            return false;
        }

        AABB searchBox = parent.getBoundingBox().inflate(searchRange, 4.0D, searchRange);
        List<DiverseCritter> list = parent.level().getEntitiesOfClass(DiverseCritter.class, searchBox, (entity) -> {
            return entity.getType() == parent.getType() && entity.isNewborn() && entity.isCrying();
        });

        if (list.isEmpty()) {
            return false;
        }

        this.targetBaby = list.get(0);
        System.out.println("[IA-MADRE] ¡Bebé llorando detectado! ID: " + targetBaby.getId());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetBaby != null && targetBaby.isAlive() && targetBaby.isCrying() && this.feedTimer < (FEEDING_DURATION + 10);
    }

    @Override
    public void start() {
        this.feedTimer = 0;
        this.parent.getNavigation().moveTo(this.targetBaby, this.speedModifier);
        System.out.println("[IA-MADRE] Corriendo hacia el bebé...");
    }

    @Override
    public void stop() {
        this.targetBaby = null;
        this.feedTimer = 0;
        this.parent.getNavigation().stop();
        System.out.println("[IA-MADRE] Goal finalizado.");
    }

    @Override
    public void tick() {
        if (targetBaby == null) return;

        this.parent.getLookControl().setLookAt(targetBaby, 30.0F, 30.0F);

        double distSqr = this.parent.distanceToSqr(targetBaby);

        if (distSqr <= 5.0D) {
            this.parent.getNavigation().stop();
            this.feedTimer++;

            if (this.feedTimer % 10 == 0) {
                System.out.println("[IA-MADRE] Alimentando... (" + feedTimer + "/" + FEEDING_DURATION + " ticks)");
            }

            if (this.feedTimer >= FEEDING_DURATION) {
                performNursing();
            }
        } else {
            this.feedTimer = 0;
            if (this.parent.getNavigation().isDone()) {
                this.parent.getNavigation().moveTo(this.targetBaby, this.speedModifier);
            }
        }
    }

    private void performNursing() {
        System.out.println("[IA-MADRE] --- CURANDO AL BEBÉ (FINALIZADO) ---");

        targetBaby.setHunger(targetBaby.maxHunger());
        targetBaby.setThirst(targetBaby.maxThirst());
        targetBaby.setCrying(false);

        Level level = parent.level();
        level.broadcastEntityEvent(targetBaby, (byte) 7); // Corazones
        parent.playSound(SoundEvents.CAT_PURR, 1.0F, 1.0F);

    }
}