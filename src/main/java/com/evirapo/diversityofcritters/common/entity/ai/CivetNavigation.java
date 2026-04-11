package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class CivetNavigation extends WallClimberNavigation {

    private final CivetEntity civet;

    public CivetNavigation(Mob mob, Level level) {
        super(mob, level);
        this.civet = (CivetEntity) mob;
    }

    /**
     * FIX — ROOT CAUSE: GroundPathNavigation.createPath(BlockPos) intercepts
     * the destination and when the target block is solid (like a bowl block)
     * it walks UP until it finds a free block above it. If the bowl is at
     * Y=-60 and the first free block above it is Y=-59, and the civet is at
     * Y=-56, the path target becomes Y=-56 (same level) — a 2-node horizontal
     * path that never includes any downward nodes.
     *
     * Fix: when the target Y is below the civet's current Y, bypass
     * GroundPathNavigation.createPath() and call the grandparent
     * PathNavigation.createPath() directly via the ImmutableSet overload,
     * which does NOT adjust the Y of the destination.
     */
    @Override
    public Path createPath(BlockPos pPos, int pAccuracy) {
        // Only bypass the Y-adjustment when navigating downward.
        // For upward or same-level paths, let the normal logic handle it.
        if (pPos.getY() < Mth.floor(this.mob.getY())) {
            return this.createPath(com.google.common.collect.ImmutableSet.of(pPos), 8, false, pAccuracy);
        }
        return super.createPath(pPos, pAccuracy);
    }

    @Override
    protected PathFinder createPathFinder(int pMaxVisitedNodes) {
        this.nodeEvaluator = new CivetNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, pMaxVisitedNodes);
    }

    /**
     * FIX — ROOT CAUSE #1:
     * GroundPathNavigation.canUpdatePath() returns false when the mob is not
     * on the ground. When the civet is climbing a wall it is never onGround(),
     * so the entire navigation tick is skipped: followThePath() never runs,
     * path nodes never advance, and MoveControl never gets new target positions.
     * The civet freezes at the ledge.
     *
     * Fix: allow path updates whenever the civet is climbing OR the normal
     * ground conditions are met.
     */
    @Override
    protected boolean canUpdatePath() {
        return this.civet.isClimbing()
                || this.mob.onGround()
                || this.isInLiquid()
                || this.mob.isPassenger();
    }

    /**
     * FIX — ROOT CAUSE #2:
     * PathNavigation.tick() calls getMoveControl().setWantedPosition() with
     * the Y returned by getGroundY(). The default implementation snaps Y to
     * the floor of the block below the node, which is wrong for wall nodes
     * that are floating in air next to a climbable block — it would push the
     * wanted Y to the ground, confusing MoveControl.
     *
     * Fix: when the civet is climbing, return the node's exact Y so MoveControl
     * can read the correct dy and set CLIMB_UP / CLIMB_DOWN accordingly.
     */
    @Override
    protected double getGroundY(Vec3 pVec) {
        if (this.civet.isClimbing()) {
            return pVec.y;
        }
        return super.getGroundY(pVec);
    }

    /**
     * FIX — ROOT CAUSE #3 (partial):
     * getTempMobPos() in GroundPathNavigation returns a Vec3 with Y snapped
     * to the surface. While climbing, use the actual entity Y so that
     * followThePath()'s distance check (d1 < 1.0) can be satisfied correctly
     * and path nodes advance normally while on the wall.
     */
    @Override
    protected Vec3 getTempMobPos() {
        if (this.civet.isClimbing()) {
            return this.mob.position();
        }
        return super.getTempMobPos();
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
        if (f > maxDelta) f = maxDelta;
        if (f < -maxDelta) f = -maxDelta;
        return current + f;
    }
}
