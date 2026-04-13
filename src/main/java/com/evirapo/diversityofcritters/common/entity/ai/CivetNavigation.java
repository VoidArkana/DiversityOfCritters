package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Set;

public class CivetNavigation extends WallClimberNavigation {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SLOW_PATH_MS = 10;

    private final CivetEntity civet;

    public CivetNavigation(Mob mob, Level level) {
        super(mob, level);
        this.civet = (CivetEntity) mob;
    }

    @Override
    protected PathFinder createPathFinder(int pMaxVisitedNodes) {
        LOGGER.info("[CivetNav] createPathFinder maxVisitedNodes={} entity={}",
                pMaxVisitedNodes, this.mob.getId());
        this.nodeEvaluator = new CivetNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, pMaxVisitedNodes);
    }

    // Intercept findPath to measure how long it takes
    @Override
    protected Path createPath(Set<BlockPos> pTargets, int pRegionOffset,
                              boolean pOffsetUpward, int pAccuracy, float pFollowRange) {
        long t0 = System.nanoTime();
        LOGGER.info("[CivetNav] createPath START entity={} targets={} range={} onGround={} climbing={}",
                this.mob.getId(), pTargets.size(), pFollowRange,
                this.mob.onGround(), this.civet.isClimbing());

        // Check all target chunks are loaded before attempting pathfinding.
        // PathNavigationRegion blocks the server thread loading chunks if any
        // are missing — this is the cause of the freeze.
        for (BlockPos target : pTargets) {
            int chunkX = target.getX() >> 4;
            int chunkZ = target.getZ() >> 4;
            if (!this.level.hasChunk(chunkX, chunkZ)) {
                LOGGER.warn("[CivetNav] Skipping path — chunk not loaded at ({},{})", chunkX, chunkZ);
                return null;
            }
        }
        // Also check entity's own chunk
        {
            int chunkX = this.mob.blockPosition().getX() >> 4;
            int chunkZ = this.mob.blockPosition().getZ() >> 4;
            if (!this.level.hasChunk(chunkX, chunkZ)) {
                LOGGER.warn("[CivetNav] Skipping path — entity chunk not loaded");
                return null;
            }
        }

        Path result = super.createPath(pTargets, pRegionOffset, pOffsetUpward, pAccuracy, pFollowRange);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        LOGGER.info("[CivetNav] createPath END {}ms nodes={} entity={}",
                ms, result != null ? result.getNodeCount() : "null", this.mob.getId());

        if (ms >= SLOW_PATH_MS) {
            LOGGER.warn("[CivetNav][PERF] createPath took {}ms | targets={} | range={} | path={} nodes | entity={} | onGround={} | climbing={}",
                    ms, pTargets.size(), pFollowRange,
                    result != null ? result.getNodeCount() : "null",
                    this.mob.getId(), this.mob.onGround(), this.civet.isClimbing());
        }

        return result;
    }

    @Override
    protected boolean canUpdatePath() {
        return this.civet.isClimbing()
                || this.mob.onGround()
                || this.isInLiquid()
                || this.mob.isPassenger();
    }

    @Override
    protected double getGroundY(Vec3 pVec) {
        if (this.civet.isClimbing()) return pVec.y;
        return super.getGroundY(pVec);
    }

    @Override
    protected Vec3 getTempMobPos() {
        if (this.civet.isClimbing()) return this.mob.position();
        return super.getTempMobPos();
    }

    @Override
    public void tick() {
        long __t = System.nanoTime();
        super.tick();
        long __ms = (System.nanoTime() - __t) / 1_000_000;
        if (__ms >= 5) {
            LOGGER.warn("[CivetNav][PERF] nav.tick() took {}ms entity={} climbing={} onGround={}",
                    __ms, this.mob.getId(), this.civet.isClimbing(), this.mob.onGround());
        }
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
