package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class CivetMoveControl extends MoveControl {
    private final CivetEntity civet;

    public CivetMoveControl(CivetEntity pMob) {
        super(pMob);
        this.civet = pMob;
    }

    @Override
    public void tick() {
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

            if (dy > (double)this.civet.maxUpStep() && dx * dx + dz * dz < (double)Math.max(1.0F, this.civet.getBbWidth())) {
                this.civet.getJumpControl().jump();
            }

        } else {
            this.civet.setZza(0.0F);
        }
    }
}