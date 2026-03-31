package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.block.DOCBlocks;
import com.evirapo.diversityofcritters.common.block.entity.DigBoxBlockEntity;
import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

public class FindDigBoxGoal extends Goal {

    private final DiverseCritter critter;
    private final double speed;
    private final int searchRadius;

    private BlockPos boxPos;
    private int digTimer = 0;
    private static final int DIG_DURATION = 80;
    private static final double MAX_DIST_SQ = 2.0D;

    private boolean isFinishing = false;
    private int finishingTimer = 0;

    public FindDigBoxGoal(DiverseCritter critter, double speed, int searchRadius) {
        this.critter = critter;
        this.speed = speed;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (critter.level().isClientSide()) return false;
        if (!critter.isEnrichmentNeeded()) return false;

        boxPos = findNearestValidDigBox();
        return boxPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (critter.level().isClientSide()) return false;
        if (boxPos == null) return false;

        if (isFinishing) return finishingTimer < 10;

        if (!critter.isEnrichmentNeeded()) return false;
        return isValidDigBox(boxPos);
    }

    @Override
    public void start() {
        digTimer = 0;
        isFinishing = false;
        finishingTimer = 0;
        if (boxPos != null) {
            critter.getNavigation().moveTo(boxPos.getX() + 0.5, boxPos.getY() + 0.5, boxPos.getZ() + 0.5, speed);
        }
    }

    @Override
    public void stop() {
        digTimer = 0;
        boxPos = null;
        isFinishing = false;
        finishingTimer = 0;
        critter.getNavigation().stop();
        setCritterDigging(false);
    }

    @Override
    public void tick() {
        if (isFinishing) {
            finishingTimer++;
            if (finishingTimer >= 10) {
                this.stop();
            }
            return;
        }

        if (boxPos == null) return;

        Vec3 boxCenter = new Vec3(boxPos.getX() + 0.5, boxPos.getY() + 0.5, boxPos.getZ() + 0.5);
        double distSq = critter.distanceToSqr(boxCenter);

        boolean isOnTop = (distSq < MAX_DIST_SQ) && (critter.getY() >= boxPos.getY() + 0.4);

        if (!isOnTop) {
            setCritterDigging(false);
            if (!critter.getNavigation().isInProgress()) {
                critter.getNavigation().moveTo(boxCenter.x, boxCenter.y, boxCenter.z, speed);
            }
            if (distSq < MAX_DIST_SQ && critter.getY() < boxPos.getY() + 0.4) {
                critter.getJumpControl().jump();
            }
            return;
        }

        critter.getNavigation().stop();
        critter.getLookControl().setLookAt(boxCenter.x, boxCenter.y - 0.5, boxCenter.z);

        if (digTimer == 0) {
            critter.setPos(boxCenter.x, critter.getY(), boxCenter.z);
            critter.setDeltaMovement(Vec3.ZERO);
        }

        setCritterDigging(true);
        digTimer++;

        if (digTimer >= DIG_DURATION) {
            performDigDrop();
            setCritterDigging(false);
            isFinishing = true;
        }
    }

    private void setCritterDigging(boolean isDigging) {
        if (critter instanceof CivetEntity civet) {
            civet.setDigging(isDigging);
        }
    }

    private void performDigDrop() {
        Level level = (Level) critter.level();
        BlockEntity be = level.getBlockEntity(boxPos);

        if (be instanceof DigBoxBlockEntity) {
            be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stackInSlot = handler.getStackInSlot(i);
                    if (!stackInSlot.isEmpty()) {
                        ItemStack extracted = handler.extractItem(i, 1, false);
                        if (!extracted.isEmpty()) {
                            ItemEntity itemEntity = new ItemEntity(level, critter.getX(), critter.getY() + 0.5, critter.getZ(), extracted);
                            itemEntity.setDeltaMovement(0, 0.2, 0);
                            level.addFreshEntity(itemEntity);

                            critter.setEnrichment(critter.maxEnrichment());
                            return;
                        }
                    }
                }
            });
        }
    }

    @Nullable
    private BlockPos findNearestValidDigBox() {
        BlockPos origin = critter.blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;
        int r = this.searchRadius;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);

                    if (isValidDigBox(pos)) {
                        double distSq = origin.distSqr(pos);
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestPos = pos;
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    private boolean isValidDigBox(BlockPos pos) {
        Level level = (Level) critter.level();
        if (!level.getBlockState(pos).is(DOCBlocks.DIG_BOX.get())) return false;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DigBoxBlockEntity)) return false;

        AtomicReference<Boolean> hasItems = new AtomicReference<>(false);
        be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    hasItems.set(true);
                    break;
                }
            }
        });

        return hasItems.get();
    }
}