package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class CivetNavigation extends WallClimberNavigation {

    private final CivetEntity civet;

    public CivetNavigation(Mob mob, Level level) {
        super(mob, level);
        this.civet = (CivetEntity) mob;
    }

    @Override
    protected PathFinder createPathFinder(int pMaxVisitedNodes) {
        this.nodeEvaluator = new CivetNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, pMaxVisitedNodes);
    }

    @Override
    protected Path createPath(Set<BlockPos> pTargets, int pRegionOffset,
                              boolean pOffsetUpward, int pAccuracy, float pFollowRange) {
        for (BlockPos target : pTargets) {
            if (!this.level.hasChunk(target.getX() >> 4, target.getZ() >> 4)) return null;
        }
        BlockPos self = this.mob.blockPosition();
        if (!this.level.hasChunk(self.getX() >> 4, self.getZ() >> 4)) return null;

        return super.createPath(pTargets, pRegionOffset, pOffsetUpward, pAccuracy, pFollowRange);
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
}
