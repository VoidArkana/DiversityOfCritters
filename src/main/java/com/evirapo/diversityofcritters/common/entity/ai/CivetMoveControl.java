package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class CivetMoveControl extends MoveControl {
    private final CivetEntity civet;

    private static final double CLIMB_SPEED = 0.16D;
    private static final double STEP_UP_IMPULSE = 0.02D;
    private static final double STEP_UP_HORIZONTAL_DAMPING = 0.02D;
    private static final int POST_CLIMB_DAMPING_TICKS = 6;
    private static final double POST_CLIMB_HORIZONTAL_DAMPING = 0.65D;
    private static final int LOOKAHEAD_NODES = 5;

    private boolean wasClimbingLastTick = false;
    private int postClimbDampingTicks = 0;

    private int landingCooldown = 0;
    private static final int LANDING_COOLDOWN_TICKS = 15; // enough to clear residual horizontal velocity

    public CivetMoveControl(CivetEntity pMob) {
        super(pMob);
        this.civet = pMob;
    }

    @Override
    public void tick() {
        boolean isClimbingNow = this.civet.isClimbing();

        // Track landing: when we were climbing down and just hit the ground
        if (this.wasClimbingLastTick && !isClimbingNow && this.civet.onGround()) {
            this.landingCooldown = LANDING_COOLDOWN_TICKS;
        }
        if (this.landingCooldown > 0) {
            this.landingCooldown--;
            // Zero out horizontal velocity during landing to prevent
            // the push-into-wall from triggering CLIMB_UP via horizontalCollision
            Vec3 motion = this.civet.getDeltaMovement();
            this.civet.setDeltaMovement(0, motion.y, 0);
        }

        if (isClimbingNow) {
            this.wasClimbingLastTick = true;
            this.postClimbDampingTicks = 0;
            this.handleClimbingMovement();
            return;
        }

        if (this.wasClimbingLastTick) {
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

        if (!this.civet.getNavigation().isDone()) {
            updateClimbDirectionFromPath();
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

            float targetYaw = (float)(Mth.atan2(dz, dx) * (double)(180F / (float)Math.PI)) - 90.0F;
            this.civet.setYRot(this.rotlerp(this.civet.getYRot(), targetYaw, 30.0F));
            this.civet.setYBodyRot(this.civet.getYRot());
            this.civet.setSpeed((float)(this.speedModifier * this.civet.getAttributeValue(Attributes.MOVEMENT_SPEED)));

            if (dy > (double)this.civet.maxUpStep()
                    && dx * dx + dz * dz < (double)Math.max(1.0F, this.civet.getBbWidth())) {
                if (!this.civet.isNewborn()) {
                    Vec3 motion = this.civet.getDeltaMovement();
                    this.civet.setDeltaMovement(
                            motion.x * STEP_UP_HORIZONTAL_DAMPING,
                            Math.max(motion.y, STEP_UP_IMPULSE),
                            motion.z * STEP_UP_HORIZONTAL_DAMPING
                    );
                }
            }
        } else {
            this.civet.setZza(0.0F);
        }
    }

    private void updateClimbDirectionFromPath() {
        Path path = this.civet.getNavigation().getPath();
        if (path == null || path.isDone()) return;

        int nextIdx = path.getNextNodeIndex();
        if (nextIdx >= path.getNodeCount()) return;

        Node nextNode = path.getNode(nextIdx);
        int currentY = (nextIdx > 0)
                ? path.getNode(nextIdx - 1).y
                : Mth.floor(this.civet.getY());

        int dy = nextNode.y - currentY;
        boolean adjacentToWall = this.civet.hasAdjacentClimbableBlock();
        boolean onGround = this.civet.onGround();

        // CLIMB_UP: path goes up AND touching wall AND not in post-descent cooldown.
        if (dy > 0 && adjacentToWall && !this.civet.isInPostDescentCooldown()) {
            this.civet.setClimbState(CivetEntity.CLIMB_UP);
            return;
        }

        // CLIMB_DOWN: path goes down AND off ground AND touching wall AND not in post-descent cooldown.
        if (dy < 0 && adjacentToWall && !onGround && !this.civet.isInPostDescentCooldown()) {
            this.civet.setClimbState(CivetEntity.CLIMB_DOWN);
            return;
        }

        // Special case: civet is onGround ON TOP of a climbable column.
        if (onGround && !this.civet.isInPostDescentCooldown()) {
            BlockPos below = this.civet.blockPosition().below();
            boolean onTopOfClimbable = this.civet.level()
                    .getBlockState(below).is(DoCTags.Blocks.CIVET_CLIMBABLE);

            if (onTopOfClimbable) {
                // Scan the full remaining path for any descent node
                boolean pathHasDescent = false;
                for (int i = nextIdx; i < path.getNodeCount(); i++) {
                    if (path.getNode(i).y < currentY) {
                        pathHasDescent = true;
                        break;
                    }
                }

                if (pathHasDescent) {
                    // Find the first descent node and walk toward it
                    for (int i = nextIdx; i < Math.min(nextIdx + LOOKAHEAD_NODES, path.getNodeCount()); i++) {
                        if (path.getNode(i).y < currentY) {
                            Node target = path.getNode(i);
                            float speed = (float)(this.speedModifier
                                    * this.civet.getAttributeValue(Attributes.MOVEMENT_SPEED));
                            double tx = target.x + 0.5 - this.civet.getX();
                            double tz = target.z + 0.5 - this.civet.getZ();
                            float yaw = (float)(Mth.atan2(tz, tx) * (180.0 / Math.PI)) - 90.0F;
                            this.civet.setYRot(this.rotlerp(this.civet.getYRot(), yaw, 30.0F));
                            this.civet.setYBodyRot(this.civet.getYRot());
                            this.civet.setSpeed(speed);
                            return;
                        }
                    }
                }
                // If no descent in full path, fall through to normal movement
            }
        }

        // Lookahead for upcoming descent (already off ground, not in post-descent cooldown).
        if (dy == 0 && adjacentToWall && !onGround && !this.civet.isInPostDescentCooldown()) {
            int limit = Math.min(nextIdx + LOOKAHEAD_NODES, path.getNodeCount());
            for (int i = nextIdx + 1; i < limit; i++) {
                int futureY = path.getNode(i).y;
                if (futureY < currentY) {
                    this.civet.setClimbState(CivetEntity.CLIMB_DOWN);
                    return;
                }
                if (futureY > currentY) break;
            }
        }
    }

    private void handleClimbingMovement() {
        float horizontalSpeed = (float)(0.8 * this.civet.getAttributeValue(Attributes.MOVEMENT_SPEED)) * 0.5F;
        this.civet.setSpeed(horizontalSpeed);
        Vec3 motion = this.civet.getDeltaMovement();

        if (this.civet.isHanging()) {
            this.civet.setDeltaMovement(motion.x, 0.0D, motion.z);
        } else if (this.civet.isClimbingDown()) {
            // Push INTO the wall during descent so the hitbox stays flush.
            // climbFacingYaw is the yaw the entity FACES (outward from wall).
            // In Minecraft yaw convention: yaw=0 faces +Z, yaw=90 faces -X.
            // Entity.getLookAngle() uses: x = -sin(yaw), z = cos(yaw)
            // So the "look" direction (outward) = (-sin(yaw), 0, cos(yaw))
            // Push INTO wall = opposite = (sin(yaw), 0, -cos(yaw))
            float yawRad = (float) Math.toRadians(this.civet.getClimbFacingYaw());
            double pushX = -Math.sin(yawRad) * 0.15;
            double pushZ = Math.cos(yawRad) * 0.15;
            this.civet.setDeltaMovement(pushX, -CLIMB_SPEED, pushZ);
        } else {
            // CLIMB_UP
            this.civet.setDeltaMovement(motion.x, CLIMB_SPEED, motion.z);
        }
    }
}