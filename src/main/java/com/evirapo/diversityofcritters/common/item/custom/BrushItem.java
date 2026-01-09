package com.evirapo.diversityofcritters.common.item.custom;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BrushItem extends Item {
    public BrushItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        if (pInteractionTarget instanceof DiverseCritter critter) {
            if (critter.getHygiene() < critter.maxHygiene()) {
                if (!pPlayer.level().isClientSide) {
                    critter.setHygiene(Math.min(critter.maxHygiene(), critter.getHygiene() + 500));

                    pPlayer.level().playSound(null, critter.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.5f, 1.2f);

                    pStack.hurtAndBreak(1, pPlayer, (player) -> player.broadcastBreakEvent(pUsedHand));

                }
                return InteractionResult.sidedSuccess(pPlayer.level().isClientSide);
            }
        }
        return super.interactLivingEntity(pStack, pPlayer, pInteractionTarget, pUsedHand);
    }
}