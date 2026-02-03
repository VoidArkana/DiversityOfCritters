package com.evirapo.diversityofcritters.common.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.phys.Vec3;

public class CivetNavigation extends WallClimberNavigation {

    public CivetNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.mob.horizontalCollision && this.mob.onGround() && !this.isDone()) {
            this.fixBodyRotation();
        }
    }

    private void fixBodyRotation() {
        Vec3 velocity = this.mob.getDeltaMovement();

        if (velocity.horizontalDistanceSqr() > 1.0E-6) {
            double dx = velocity.x;
            double dz = velocity.z;

            float targetYaw = (float)(Mth.atan2(dz, dx) * (double)(180F / (float)Math.PI)) - 90.0F;

            float newRot = this.rotlerp(this.mob.yBodyRot, targetYaw, 20.0F);

            this.mob.setYBodyRot(newRot);
            this.mob.setYRot(newRot);
        }
    }

    private float rotlerp(float current, float target, float maxDelta) {
        float f = Mth.wrapDegrees(target - current);
        if (f > maxDelta) {
            f = maxDelta;
        }
        if (f < -maxDelta) {
            f = -maxDelta;
        }
        return current + f;
    }
}