package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class CivetMoveControl extends MoveControl {
    private final CivetEntity civet;

    private static final double CLIMB_SPEED = 0.16D;
    private static final double STEP_UP_IMPULSE = 0.02D;
    private static final double STEP_UP_HORIZONTAL_DAMPING = 0.02D;
    private static final int POST_CLIMB_DAMPING_TICKS = 6;
    private static final double POST_CLIMB_HORIZONTAL_DAMPING = 0.65D;

    private boolean wasClimbingLastTick = false;
    private int postClimbDampingTicks = 0;

    public CivetMoveControl(CivetEntity pMob) {
        super(pMob);
        this.civet = pMob;
    }

    @Override
    public void tick() {
        boolean isClimbingNow = this.civet.isClimbing();

        // When climbing, handle movement directly regardless of navigation state
        if (isClimbingNow) {
            this.wasClimbingLastTick = true;
            this.postClimbDampingTicks = 0;
            this.handleClimbingMovement();
            return;
        }

        if (this.wasClimbingLastTick) {
            // Short horizontal damping window when leaving a wall to avoid overshooting.
            this.postClimbDampingTicks = POST_CLIMB_DAMPING_TICKS;
            this.wasClimbingLastTick = false;
        }

        if (this.postClimbDampingTicks > 0) {
            Vec3 motion = this.civet.getDeltaMovement();
            this.civet.setDeltaMovement(
                    motion.x * POST_CLIMB_HORIZONTAL_DAMPING,
                    motion.y,
                    motion.z * POST_CLIMB_HORIZONTAL_DAMPING
            );
            this.postClimbDampingTicks--;
        }

        if (this.operation == Operation.MOVE_TO && !this.civet.getNavigation().isDone()) {
            double dx = this.wantedX - this.civet.getX();
            double dy = this.wantedY - this.civet.getY();
            double dz = this.wantedZ - this.civet.getZ();
            double distSqr = dx * dx + dy * dy + dz * dz;

            if (distSqr < 2.500000277905201E-7D) {
                this.mob.setZza(0.0F);
                return;
            }

            // --- NORMAL GROUND MOVEMENT ---
            float targetYaw = (float)(Mth.atan2(dz, dx) * (double)(180F / (float)Math.PI)) - 90.0F;
            this.civet.setYRot(this.rotlerp(this.civet.getYRot(), targetYaw, 30.0F));
            this.civet.setYBodyRot(this.civet.getYRot());
            this.civet.setSpeed((float)(this.speedModifier * this.civet.getAttributeValue(Attributes.MOVEMENT_SPEED)));

            if (dy > (double)this.civet.maxUpStep() && dx * dx + dz * dz < (double)Math.max(1.0F, this.civet.getBbWidth())) {
                Vec3 motion = this.civet.getDeltaMovement();
                this.civet.setDeltaMovement(
                        motion.x * STEP_UP_HORIZONTAL_DAMPING,
                        Math.max(motion.y, STEP_UP_IMPULSE),
                        motion.z * STEP_UP_HORIZONTAL_DAMPING
                );
            }

        } else {
            this.civet.setZza(0.0F);
        }
    }

    /**
     * Handles movement while the civet is attached to a wall.
     * Pushes horizontally into the wall to maintain contact.
     * CLIMB_UP: applies upward velocity.
     * CLIMB_HANG: zeroes vertical velocity to stay in place.
     */
    private void handleClimbingMovement() {
        // Push horizontally into the wall so we keep horizontalCollision=true
        float horizontalSpeed = (float)(0.8 * this.civet.getAttributeValue(Attributes.MOVEMENT_SPEED)) * 0.5F;
        this.civet.setSpeed(horizontalSpeed);

        Vec3 motion = this.civet.getDeltaMovement();

        if (this.civet.isHanging()) {
            // HANG: stay in place on the wall - zero vertical velocity
            this.civet.setDeltaMovement(motion.x, 0.0D, motion.z);
        } else {
            // CLIMB_UP: apply vertical climbing movement
            this.civet.setDeltaMovement(motion.x, CLIMB_SPEED, motion.z);
        }
    }
}
