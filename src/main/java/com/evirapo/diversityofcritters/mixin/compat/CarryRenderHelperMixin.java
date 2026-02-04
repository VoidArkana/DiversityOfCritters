package com.evirapo.diversityofcritters.mixin.compat;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tschipp.carryon.client.render.CarryRenderHelper;


@Mixin(CarryRenderHelper.class)
public class CarryRenderHelperMixin {

    @Inject(method = "applyEntityTransformations", at = @At("RETURN"), remap = false)
    private static void adjustDiverseCritterHeight(Player player, float partialticks, PoseStack matrix, Entity entity, CallbackInfo ci) {

        if (entity instanceof DiverseCritter critter) {

            if (critter.isNewborn()) {
                matrix.translate(0.0, 0.30, 0.0);
            } else if (critter.isJuvenile()) {
                matrix.translate(0.0, 0.05, 0.0);
            }
        }
    }
}