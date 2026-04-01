package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.common.item.DOCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class DigDirtGoal extends Goal {
    private final CivetEntity civet;
    private final Level level;
    private BlockPos targetBlock = null;
    private int digTimer = 0;
    private final int DIG_DURATION = 80;

    private boolean isFinishing = false;
    private int finishingTimer = 0;

    public DigDirtGoal(CivetEntity civet) {
        this.civet = civet;
        this.level = civet.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (civet.isNewborn() || level.isClientSide) return false;

        if (!civet.isEnrichmentNeeded()) return false;

        targetBlock = findDirtBlock();
        return targetBlock != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (isFinishing) return finishingTimer < 10;
        return targetBlock != null && !civet.isNewborn();
    }

    @Override
    public void start() {
        digTimer = 0;
        isFinishing = false;
        finishingTimer = 0;
    }

    @Override
    public void stop() {
        targetBlock = null;
        digTimer = 0;
        isFinishing = false;
        finishingTimer = 0;
        this.civet.setDigging(false);
        this.civet.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (isFinishing) {
            finishingTimer++;
            if (finishingTimer >= 10) this.stop();
            return;
        }

        if (targetBlock == null) return;

        double distSq = civet.distanceToSqr(targetBlock.getX() + 0.5, civet.getY(), targetBlock.getZ() + 0.5);

        if (distSq > 1.8D) {
            civet.setDigging(false);
            if (!civet.getNavigation().isInProgress()) {
                this.civet.getNavigation().moveTo(targetBlock.getX() + 0.5, targetBlock.getY() + 1.0, targetBlock.getZ() + 0.5, 1.2D);
            }
        } else {
            civet.getNavigation().stop();
            civet.getLookControl().setLookAt(targetBlock.getX() + 0.5, targetBlock.getY(), targetBlock.getZ() + 0.5);

            civet.setDigging(true);
            digTimer++;

            if (digTimer >= DIG_DURATION) {
                spawnWorms();
                // RECOMPENSA: Recupera todo el enriquecimiento
                civet.setEnrichment(civet.maxEnrichment());
                civet.setDigging(false);
                isFinishing = true;
            }
        }
    }

    private void spawnWorms() {
        if (!level.isClientSide) {
            // Siempre saca 1, y tiene 50% de probabilidad de sacar un segundo (1 o 2)
            int count = level.random.nextInt(2) + 1;

            for (int i = 0; i < count; i++) {
                ItemStack worm = new ItemStack(DOCItems.WORM.get());
                ItemEntity item = new ItemEntity(level, civet.getX(), civet.getY() + 0.3, civet.getZ(), worm);
                item.setDeltaMovement(level.random.nextGaussian() * 0.05, 0.2, level.random.nextGaussian() * 0.05);
                level.addFreshEntity(item);
            }
        }
    }

    private BlockPos findDirtBlock() {
        BlockPos origin = civet.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -4), origin.offset(4, 1, 4))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
                if (level.isEmptyBlock(pos.above())) return pos.immutable();
            }
        }
        return null;
    }
}