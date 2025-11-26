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
    private static final int DIG_DURATION = 80; // 4 segundos
    private static final double MAX_DIST_SQ = 2.0D;

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

        if (boxPos != null) {
            if (DiverseCritter.DEBUG_BOWL_GOALS) {
                System.out.println("[DIG-BOX-GOAL] canUse=TRUE pos=" + critter.blockPosition()
                        + " enrichment=" + critter.getEnrichment()
                        + " box=" + boxPos.toShortString());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (critter.level().isClientSide()) return false;
        if (boxPos == null) return false;
        if (!critter.isEnrichmentNeeded()) return false;
        return isValidDigBox(boxPos);
    }

    @Override
    public void start() {
        digTimer = 0;
        if (boxPos != null) {
            critter.getNavigation().moveTo(boxPos.getX() + 0.5, boxPos.getY() + 0.5, boxPos.getZ() + 0.5, speed);
        }
        critter.debugGoalMessage("FindDigBoxGoal", "START");
    }

    @Override
    public void stop() {
        digTimer = 0;
        boxPos = null;
        critter.getNavigation().stop();
        setCritterDigging(false);
        critter.debugGoalMessage("FindDigBoxGoal", "STOP");
    }

    @Override
    public void tick() {
        if (boxPos == null) return;

        Vec3 boxCenter = new Vec3(boxPos.getX() + 0.5, boxPos.getY() + 0.5, boxPos.getZ() + 0.5);
        double distSq = critter.distanceToSqr(boxCenter);

        // Verificamos si está lo suficientemente cerca y a la altura correcta
        boolean isOnTop = (distSq < MAX_DIST_SQ) && (critter.getY() >= boxPos.getY() + 0.4);

        if (!isOnTop) {
            setCritterDigging(false);
            if (!critter.getNavigation().isInProgress()) {
                critter.getNavigation().moveTo(boxCenter.x, boxCenter.y, boxCenter.z, speed);
            }
            // Saltar si está pegado al bloque pero abajo
            if (distSq < MAX_DIST_SQ && critter.getY() < boxPos.getY() + 0.4) {
                critter.getJumpControl().jump();
            }
            return;
        }

        // --- ESTÁ EN POSICIÓN ---

        critter.getNavigation().stop();
        critter.getLookControl().setLookAt(boxCenter.x, boxCenter.y - 0.5, boxCenter.z);

        // CENTRADO AUTOMÁTICO:
        // Si acabamos de llegar (digTimer es 0), forzamos la posición al centro exacto
        if (digTimer == 0) {
            critter.setPos(boxCenter.x, critter.getY(), boxCenter.z);
            critter.setDeltaMovement(Vec3.ZERO); // Frenar en seco para que no resbale
        }

        setCritterDigging(true);
        digTimer++;

        if (DiverseCritter.DEBUG_BOWL_GOALS && digTimer % 20 == 0) {
            System.out.println("[DIG-BOX-GOAL] Digging... Timer=" + digTimer + "/" + DIG_DURATION
                    + " enrichment=" + critter.getEnrichment());
        }

        if (digTimer >= DIG_DURATION) {
            performDigDrop();
            this.stop();
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

                            critter.debugGoalMessage("FindDigBoxGoal", "DIG SUCCESS: Dropped " + extracted);
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