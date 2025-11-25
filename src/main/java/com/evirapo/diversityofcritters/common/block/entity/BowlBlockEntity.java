package com.evirapo.diversityofcritters.common.block.entity;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.common.block.BowlBlock;
import com.evirapo.diversityofcritters.common.block.BowlContent;
import com.evirapo.diversityofcritters.common.block.BowlMenu;
import com.evirapo.diversityofcritters.common.block.DOCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BowlBlockEntity extends BlockEntity implements MenuProvider {

    public static final int WATER_CHARGES_PER_BUCKET = 10;

    private int waterCharges = 0;

    private final SimpleContainer inventory = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            BowlBlockEntity.this.setChanged();
            BowlBlockEntity.this.updateContentState();
        }
    };

    public BowlBlockEntity(BlockPos pos, BlockState state) {
        super(DOCBlockEntities.BOWL_BE.get(), pos, state);
    }

    public Container getInventory() {
        return inventory;
    }

    public int getWaterCharges() {
        return waterCharges;
    }

    public void setWaterCharges(int value) {
        this.waterCharges = Math.max(0, value);
        setChanged();
    }

    public void consumeWaterCharge() {
        if (this.waterCharges > 0) {
            this.waterCharges--;
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        NonNullList<ItemStack> items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);

        tag.putInt("WaterCharges", this.waterCharges);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        NonNullList<ItemStack> items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, items.get(i));
        }

        this.waterCharges = tag.getInt("WaterCharges");

        updateContentState();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            updateContentState();
        }
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + DiversityOfCritters.MODID + ".bowl");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new BowlMenu(id, playerInv, this);
    }

    private void updateContentState() {
        if (level == null || level.isClientSide) return;

        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof BowlBlock)) return;

        int meatCount  = 0;
        int vegCount   = 0;
        int waterCount = 0;
        int waterUnits = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            if (BowlLogic.isWater(stack)) {
                waterCount++;
                waterUnits += stack.getCount();
            } else if (BowlLogic.isMeat(stack)) {
                meatCount++;
            } else if (BowlLogic.isVegOrLeaves(stack)) {
                vegCount++;
            }
        }

        BowlContent newContent;

        if (meatCount > 0 && vegCount > 0) {
            newContent = BowlContent.MIX;
        } else if (waterCount > 0) {
            newContent = BowlContent.WATER;
        } else if (meatCount > 0) {
            newContent = BowlContent.MEAT;
        } else if (vegCount > 0) {
            newContent = BowlContent.VEG;
        } else {
            newContent = BowlContent.EMPTY;
        }

        BowlContent oldContent = state.getValue(BowlBlock.CONTENT);

        if (oldContent != newContent) {
            if (newContent == BowlContent.WATER) {
                this.waterCharges = waterUnits * WATER_CHARGES_PER_BUCKET;
            } else {
                this.waterCharges = 0;
            }
            setChanged();
        }

        if (oldContent != newContent) {
            level.setBlock(worldPosition, state.setValue(BowlBlock.CONTENT, newContent), 3);
        }
    }

    public static class BowlLogic {

        public static boolean isMeat(ItemStack stack) {
            return stack.is(net.minecraft.world.item.Items.BEEF)
                    || stack.is(net.minecraft.world.item.Items.PORKCHOP)
                    || stack.is(net.minecraft.world.item.Items.CHICKEN)
                    || stack.is(net.minecraft.world.item.Items.MUTTON)
                    || stack.is(net.minecraft.world.item.Items.RABBIT)
                    || stack.is(net.minecraft.world.item.Items.SALMON)
                    || stack.is(net.minecraft.world.item.Items.COD);
        }

        public static boolean isVegOrLeaves(ItemStack stack) {
            boolean veg = stack.is(net.minecraft.world.item.Items.CARROT)
                    || stack.is(net.minecraft.world.item.Items.POTATO)
                    || stack.is(net.minecraft.world.item.Items.BEETROOT)
                    || stack.is(net.minecraft.world.item.Items.WHEAT)
                    || stack.is(net.minecraft.world.item.Items.MELON_SLICE)
                    || stack.is(net.minecraft.world.item.Items.APPLE);

            boolean leaves = stack.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.BIRCH_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.JUNGLE_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.ACACIA_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.DARK_OAK_LEAVES.asItem());

            return veg || leaves;
        }

        public static boolean isWater(ItemStack stack) {
            if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                return true;
            }

            if (stack.is(net.minecraft.world.item.Items.POTION)) {
                return net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack)
                        == net.minecraft.world.item.alchemy.Potions.WATER;
            }

            return false;
        }
    }
}
