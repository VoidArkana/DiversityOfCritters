package com.evirapo.diversityofcritters.common.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GauzeBandageItem extends Item {
    public GauzeBandageItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        if (pInteractionTarget.getHealth() < pInteractionTarget.getMaxHealth()) {
            if (!pPlayer.level().isClientSide) {
                pInteractionTarget.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1));

                pPlayer.level().playSound(null, pInteractionTarget.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 1.0f);

                if (!pPlayer.getAbilities().instabuild) {
                    pStack.shrink(1);
                }

                pPlayer.getCooldowns().addCooldown(this, 20);
            }
            return InteractionResult.sidedSuccess(pPlayer.level().isClientSide);
        }
        return super.interactLivingEntity(pStack, pPlayer, pInteractionTarget, pUsedHand);
    }
}