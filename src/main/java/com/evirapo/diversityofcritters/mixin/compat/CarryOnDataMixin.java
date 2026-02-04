package com.evirapo.diversityofcritters.mixin.compat;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tschipp.carryon.common.carry.CarryOnData;

@Mixin(CarryOnData.class)
public class CarryOnDataMixin {

    @Shadow private CompoundTag nbt;

    @Inject(method = "getEntity", at = @At("RETURN"))
    private void fixDiverseCritterAge(Level level, CallbackInfoReturnable<Entity> cir) {
        Entity entity = cir.getReturnValue();

        if (entity == null) return;

        if (entity instanceof DiverseCritter critter) {

            if (this.nbt != null && this.nbt.contains("entity")) {
                CompoundTag entityTag = this.nbt.getCompound("entity");

                if (entityTag.contains("DiverseAge")) {
                    int correctAge = entityTag.getInt("DiverseAge");
                    int currentAge = critter.getAge();

                    if (currentAge != correctAge) {
                        System.out.println("[DOC-DEBUG] ¡CONFLICTO DETECTADO!");
                        System.out.println("   - Edad guardada (NBT): " + correctAge);
                        System.out.println("   - Edad actual (Vanilla): " + currentAge);

                        critter.setAge(correctAge);
                        System.out.println("   -> CORRECCIÓN APLICADA. Nueva edad: " + critter.getAge());
                    }
                } else {
                    System.out.println("[DOC-DEBUG] ERRO: El NBT de CarryOn NO tiene 'DiverseAge'.");
                }
            } else {
                System.out.println("[DOC-DEBUG] ERROR: CarryOnData nbt es null o no tiene tag 'entity'.");
            }
        }
    }
}