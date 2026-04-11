package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
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

    // How many nodes ahead to scan for a vertical change when the
    // immediate next node is at the same Y (e.g. standing on top of wall).
    private static final int LOOKAHEAD_NODES = 3;

    private boolean wasClimbingLastTick = false;
    private int postClimbDampingTicks = 0;

    public CivetMoveControl(CivetEntity pMob) {
        super(pMob);
        this.civet = pMob;
    }

    @Override
    public void tick() {
        boolean isClimbingNow = this.civet.isClimbing();

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

        if (this.operation == Operation.MOVE_TO && !this.civet.getNavigation().isDone()) {
            updateClimbDirectionFromPath();

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

            if (dy > (double)this.civet.maxUpStep() && dx * dx + dz * dz < (double)Math.max(1.0F, this.civet.getBbWidth())) {
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

    /**
     * Reads the active path to decide whether to climb up or down.
     *
     * Problem with the previous version: when the civet stands on top of a
     * wall, the immediate next node is at the same Y (horizontal move to the
     * edge), so dy == 0 and CLIMB_DOWN was never triggered.
     *
     * Fix: if dy == 0 on the next node, scan up to LOOKAHEAD_NODES further
     * nodes ahead. If any of them is lower than the current Y AND the civet
     * is adjacent to a climbable wall, start descending now.
     *
     * This lets the civet commit to descending before it reaches the exact
     * edge pixel, which is critical for the physics to engage in time.
     */
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

        if (dy > 0 && adjacentToWall) {
            this.civet.setClimbState(CivetEntity.CLIMB_UP);
            return;
        }

        if (dy < 0 && adjacentToWall) {
            this.civet.setClimbState(CivetEntity.CLIMB_DOWN);
            return;
        }

        // dy == 0: look ahead to detect an upcoming descent
        // This handles the "standing on top of wall" case where the path
        // first moves horizontally to the edge before going down.
        if (dy == 0 && adjacentToWall) {
            int lookaheadLimit = Math.min(nextIdx + LOOKAHEAD_NODES, path.getNodeCount());
            for (int i = nextIdx + 1; i < lookaheadLimit; i++) {
                int futureY = path.getNode(i).y;
                if (futureY < currentY) {
                    // Upcoming descent detected — start descending now
                    this.civet.setClimbState(CivetEntity.CLIMB_DOWN);
                    return;
                }
                if (futureY > currentY) {
                    // Upcoming ascent — nothing to do here, will be caught next tick
                    break;
                }
            }
        }

        // No vertical movement needed — leave existing state for
        // updateClimbState() in CivetEntity to clean up when appropriate.
    }

    /**
     * Applies vertical velocity while the civet is on a wall.
     * Also pushes lightly into the wall to maintain horizontalCollision,
     * which is what keeps onClimbable() = true and suppresses gravity.
     *
     * CLIMB_UP   → +CLIMB_SPEED in Y
     * CLIMB_DOWN → -CLIMB_SPEED in Y
     * CLIMB_HANG → 0 in Y (static hang)
     */
    private void handleClimbingMovement() {
        float horizontalSpeed = (float)(0.8 * this.civet.getAttributeValue(Attributes.MOVEMENT_SPEED)) * 0.5F;
        this.civet.setSpeed(horizontalSpeed);

        Vec3 motion = this.civet.getDeltaMovement();

        if (this.civet.isHanging()) {
            this.civet.setDeltaMovement(motion.x, 0.0D, motion.z);
        } else if (this.civet.isClimbingDown()) {
            this.civet.setDeltaMovement(motion.x, -CLIMB_SPEED, motion.z);
        } else {
            // CLIMB_UP
            this.civet.setDeltaMovement(motion.x, CLIMB_SPEED, motion.z);
        }
    }
}
