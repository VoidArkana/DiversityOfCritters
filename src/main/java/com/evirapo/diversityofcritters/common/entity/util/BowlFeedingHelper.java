package com.evirapo.diversityofcritters.common.entity.util;

import com.evirapo.diversityofcritters.common.block.BowlBlock;
import com.evirapo.diversityofcritters.common.block.BowlContent;
import com.evirapo.diversityofcritters.common.block.entity.BowlBlockEntity;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BowlFeedingHelper {

    public static boolean hasFoodFor(DiverseCritter critter, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BowlBlock)) return false;

        BowlContent content = state.getValue(BowlBlock.CONTENT);
        CritterDietConfig diet = critter.getDietConfig();

        return switch (content) {
            case MEAT -> diet.acceptsMeat;
            case VEG  -> diet.acceptsVeg;
            case MIX  -> diet.acceptsMix;
            default   -> false;
        };
    }

    public static boolean hasWaterFor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BowlBlock)) return false;

        BowlContent content = state.getValue(BowlBlock.CONTENT);
        return content == BowlContent.WATER;
    }

    public static boolean consumeFoodFor(DiverseCritter critter, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BowlBlock)) {
            System.out.println("[BOWL-FOOD] " + pos.toShortString() + " no es BowlBlock");
            return false;
        }

        BowlContent content = state.getValue(BowlBlock.CONTENT);
        CritterDietConfig diet = critter.getDietConfig();

        System.out.println("[BOWL-FOOD] content=" + content +
                " hunger=" + critter.getHunger() +
                " at " + pos.toShortString());

        // 1) Verificar si la dieta acepta este tipo de bowl
        boolean accepts;
        switch (content) {
            case MEAT -> accepts = diet.acceptsMeat;
            case VEG  -> accepts = diet.acceptsVeg;
            case MIX  -> accepts = diet.acceptsMix;
            default   -> {
                System.out.println("[BOWL-FOOD] content no es comida: " + content);
                return false;
            }
        }

        if (!accepts) {
            System.out.println("[BOWL-FOOD] diet no acepta este tipo de bowl: " + content);
            return false;
        }

        // 2) Cuánto restaura cada ítem, según el tipo de bowl
        int restorePerItem = switch (content) {
            case MEAT -> diet.hungerPerMeatBowl;  // 32 en tu civeta
            case VEG  -> diet.hungerPerVegBowl;   // 10
            case MIX  -> diet.hungerPerMixBowl;   // 30
            default   -> 0;
        };

        if (restorePerItem <= 0) {
            System.out.println("[BOWL-FOOD] restorePerItem <= 0, nada que curar");
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BowlBlockEntity bowlBe)) {
            System.out.println("[BOWL-FOOD] no hay BowlBlockEntity en " + pos.toShortString());
            return false;
        }

        Container inv = bowlBe.getInventory();

        System.out.println("[BOWL-FOOD] Escaneando inventario del bowl...");
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            boolean isMeat = BowlBlockEntity.BowlLogic.isMeat(stack);
            boolean isVeg  = BowlBlockEntity.BowlLogic.isVegOrLeaves(stack);

            System.out.println("  Slot " + i + " -> " + stack + " isMeat=" + isMeat + " isVeg=" + isVeg);

            boolean matches = switch (content) {
                case MEAT -> isMeat;
                case VEG  -> isVeg;
                case MIX  -> (isMeat || isVeg);
                default   -> false;
            };

            if (!matches) {
                System.out.println("    no coincide con el content=" + content);
                continue;
            }

            System.out.println("    MATCH, consumiendo 1 ítem en slot " + i);

            // Consumir 1 ítem
            stack.shrink(1);
            if (stack.isEmpty()) {
                inv.setItem(i, ItemStack.EMPTY);
            }

            int before = critter.getHunger();
            critter.setHunger(before + restorePerItem);
            int after = critter.getHunger();

            System.out.println("[BOWL-FOOD] Hunger " + before + " -> " + after);

            inv.setChanged(); // BowlBlockEntity recalcula CONTENT

            return true;
        }

        System.out.println("[BOWL-FOOD] No se encontró ítem compatible en el inventario");
        return false;
    }


    public static boolean consumeWaterFor(DiverseCritter critter, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BowlBlock)) {
            System.out.println("[BOWL-WATER] " + pos.toShortString() + " no es BowlBlock");
            return false;
        }

        BowlContent content = state.getValue(BowlBlock.CONTENT);
        System.out.println("[BOWL-WATER] content=" + content +
                " thirst=" + critter.getThirst() +
                " at " + pos.toShortString());

        if (content != BowlContent.WATER) {
            System.out.println("[BOWL-WATER] content no es WATER, es " + content);
            return false;
        }

        CritterDietConfig diet = critter.getDietConfig();
        int restore = diet.thirstPerWaterBowl;

        if (restore <= 0) {
            System.out.println("[BOWL-WATER] thirstPerWaterBowl <= 0, nada que curar");
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BowlBlockEntity bowlBe)) {
            System.out.println("[BOWL-WATER] no hay BowlBlockEntity en " + pos.toShortString());
            return false;
        }

        int charges = bowlBe.getWaterCharges();
        if (charges <= 0) {
            System.out.println("[BOWL-WATER] Sin cargas de agua, vaciando bowl.");
            level.setBlock(pos, state.setValue(BowlBlock.CONTENT, BowlContent.EMPTY), 3);
            bowlBe.setWaterCharges(0);
            return false;
        }

        int before = critter.getThirst();
        critter.setThirst(before + restore);
        int after = critter.getThirst();
        System.out.println("[BOWL-WATER] Thirst " + before + " -> " + after);

        // Consumir 1 carga
        bowlBe.consumeWaterCharge();
        charges = bowlBe.getWaterCharges();
        System.out.println("[BOWL-WATER] Carga consumida. Cargas restantes=" + charges);

        if (charges <= 0) {
            // Se agotó "un balde": convertir el agua del inventario en recipientes vacíos
            Container inv = bowlBe.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty()) continue;

                if (BowlBlockEntity.BowlLogic.isWater(stack)) {
                    // Si es balde de agua -> balde vacío
                    if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                        int count = stack.getCount();
                        inv.setItem(i, new ItemStack(net.minecraft.world.item.Items.BUCKET, count));
                    }
                    // Si es poción de agua -> botella de vidrio
                    else if (stack.is(net.minecraft.world.item.Items.POTION)) {
                        int count = stack.getCount();
                        inv.setItem(i, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, count));
                    }
                }
            }
            inv.setChanged();

            // Vaciar visualmente el bowl
            level.setBlock(pos, state.setValue(BowlBlock.CONTENT, BowlContent.EMPTY), 3);
            bowlBe.setWaterCharges(0);
            System.out.println("[BOWL-WATER] Bowl vacío en " + pos.toShortString());
        }

        return true;
    }
}
