package com.evirapo.diversityofcritters.common.entity.util;

import com.evirapo.diversityofcritters.common.block.BowlBlock;
import com.evirapo.diversityofcritters.common.block.BowlContent;
import com.evirapo.diversityofcritters.common.block.entity.BowlBlockEntity;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        if (!(state.getBlock() instanceof BowlBlock)) return false;

        BowlContent content = state.getValue(BowlBlock.CONTENT);
        CritterDietConfig diet = critter.getDietConfig();

        boolean accepts = switch (content) {
            case MEAT -> diet.acceptsMeat;
            case VEG  -> diet.acceptsVeg;
            case MIX  -> diet.acceptsMix;
            default   -> false;
        };
        if (!accepts) return false;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BowlBlockEntity bowlBe)) return false;

        Container inv = bowlBe.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            boolean isMeat = BowlBlockEntity.BowlLogic.isMeat(stack);
            boolean isVeg  = BowlBlockEntity.BowlLogic.isVegOrLeaves(stack);

            boolean matches = switch (content) {
                case MEAT -> isMeat;
                case VEG  -> isVeg;
                case MIX  -> (isMeat || isVeg);
                default   -> false;
            };
            if (!matches) continue;

            var foodProps = stack.isEdible() ? stack.getFoodProperties(critter) : null;
            stack.shrink(1);
            if (stack.isEmpty()) inv.setItem(i, ItemStack.EMPTY);

            critter.setHunger(critter.maxHunger());
            if (foodProps != null) critter.heal((float) foodProps.getNutrition());
            else critter.heal(1.0F);

            inv.setChanged();
            return true;
        }
        return false;
    }

    public static boolean consumeWaterFor(DiverseCritter critter, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BowlBlock)) return false;

        BowlContent content = state.getValue(BowlBlock.CONTENT);
        if (content != BowlContent.WATER) return false;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BowlBlockEntity bowlBe)) return false;

        int chargesBefore = bowlBe.getWaterCharges();
        if (chargesBefore <= 0) {
            level.setBlock(pos, state.setValue(BowlBlock.CONTENT, BowlContent.EMPTY), 3);
            bowlBe.setWaterCharges(0);
            return false;
        }

        critter.setThirst(critter.maxThirst());
        bowlBe.consumeWaterCharge();
        int chargesAfter = bowlBe.getWaterCharges();

        if (chargesAfter < chargesBefore
                && chargesAfter % BowlBlockEntity.WATER_CHARGES_PER_BOTTLE == 0) {
            consumeOneWaterContainer(bowlBe, level, pos);
        }

        if (chargesAfter <= 0) {
            Container inv = bowlBe.getInventory();
            int waterUnits = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && BowlBlockEntity.BowlLogic.isWater(s)) waterUnits += s.getCount();
            }
            if (waterUnits <= 0) {
                level.setBlock(pos, state.setValue(BowlBlock.CONTENT, BowlContent.EMPTY), 3);
                bowlBe.setWaterCharges(0);
            } else {
                int newCharges = waterUnits * BowlBlockEntity.WATER_CHARGES_PER_BOTTLE;
                bowlBe.setWaterCharges(newCharges);
                level.setBlock(pos, state.setValue(BowlBlock.CONTENT, BowlContent.WATER), 3);
            }
        }
        return true;
    }


    private static void consumeOneWaterContainer(BowlBlockEntity bowlBe, Level level, BlockPos pos) {
        Container inv = bowlBe.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (!BowlBlockEntity.BowlLogic.isWater(stack)) continue;

            if (stack.is(Items.POTION)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
                placeOneItem(inv, new ItemStack(Items.GLASS_BOTTLE));
                inv.setChanged();
                return;
            }
        }
    }

    private static void placeOneItem(Container inv, ItemStack stack) {
        if (stack.isEmpty()) return;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack current = inv.getItem(i);
            if (current.isEmpty()) {
                inv.setItem(i, stack);
                return;
            }
        }
    }
}
