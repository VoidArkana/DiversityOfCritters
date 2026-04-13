package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class CivetMoveControl extends MoveControl {
    private final CivetEntity civet;
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double CLIMB_SPEED = 0.16D;
    private static final double STEP_UP_IMPULSE = 0.02D;
    private static final double STEP_UP_HORIZONTAL_DAMPING = 0.02D;
    private static final int POST_CLIMB_DAMPING_TICKS = 6;
    private static final double POST_CLIMB_HORIZONTAL_DAMPING = 0.65D;
    private static final int LOOKAHEAD_NODES = 3;

    private boolean wasClimbingLastTick = false;
    private int postClimbDampingTicks = 0;
    private int logThrottle = 0;

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

        // Always run path analysis, even outside MOVE_TO block
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

        BlockPos below = this.civet.blockPosition().below();
        boolean onTopOfClimbable = onGround
                && this.civet.level().getBlockState(below).is(DoCTags.Blocks.CIVET_CLIMBABLE);

        // Log every 10 ticks
        logThrottle++;
        if (logThrottle >= 10) {
            logThrottle = 0;
            LOGGER.info("[MoveCtrl] pos=({},{}) nextNode=({},{},{}) dy={} adjWall={} onGround={} onTopClimb={} op={}",
                    String.format("%.1f", this.civet.getX()),
                    String.format("%.1f", this.civet.getY()),
                    nextNode.x, nextNode.y, nextNode.z,
                    dy, adjacentToWall, onGround, onTopOfClimbable,
                    this.operation);
        }

        // CLIMB_UP
        if (dy > 0 && adjacentToWall) {
            this.civet.setClimbState(CivetEntity.CLIMB_UP);
            return;
        }

        // CLIMB_DOWN while on wall (not on ground)
        if (dy < 0 && adjacentToWall && !onGround) {
            this.civet.setClimbState(CivetEntity.CLIMB_DOWN);
            return;
        }

        // On top of climbable column, path goes down:
        // walk toward descent node to reach the edge
        if (onTopOfClimbable) {
            int limit = Math.min(nextIdx + LOOKAHEAD_NODES + 2, path.getNodeCount());
            for (int i = nextIdx; i < limit; i++) {
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
                    LOGGER.info("[MoveCtrl] onTopOfClimbable → forcing toward ({},{},{}) speed={}",
                            target.x, target.y, target.z, String.format("%.3f", speed));
                    return;
                }
            }
            LOGGER.info("[MoveCtrl] onTopOfClimbable but no descent in lookahead nextIdx={} total={}",
                    nextIdx, path.getNodeCount());
        }

        // Lookahead descent (off ground)
        if (dy == 0 && adjacentToWall && !onGround) {
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
            this.civet.setDeltaMovement(motion.x, -CLIMB_SPEED, motion.z);
        } else {
            this.civet.setDeltaMovement(motion.x, CLIMB_SPEED, motion.z);
        }
    }
}
