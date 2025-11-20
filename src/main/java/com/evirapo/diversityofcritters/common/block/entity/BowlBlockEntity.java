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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BowlBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {

    // Inventario simple de 2 slots
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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        // Guardar los items del inventario
        NonNullList<ItemStack> items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        NonNullList<ItemStack> items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, items.get(i));
        }

        // Por si el contenido cambia el modelo al cargar
        updateContentState();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            updateContentState();
        }
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + DiversityOfCritters.MODID + ".bowl");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new BowlMenu(id, playerInv, this);
    }

    // ---- Lógica para cambiar el modelo según el inventario ----

    private void updateContentState() {
        if (level == null || level.isClientSide) return;

        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof BowlBlock)) return;

        int meatCount  = 0;
        int vegCount   = 0;
        int waterCount = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            if (BowlLogic.isWater(stack)) {
                waterCount++;
            } else if (BowlLogic.isMeat(stack)) {
                meatCount++;
            } else if (BowlLogic.isVegOrLeaves(stack)) {
                vegCount++;
            }
        }

        BowlContent newContent;

        // 1) Carne + verdura/hojas -> MIX
        if (meatCount > 0 && vegCount > 0) {
            newContent = BowlContent.MIX;

            // 2) Cualquier agua (con o sin comida) -> WATER
        } else if (waterCount > 0) {
            newContent = BowlContent.WATER;

            // 3) Solo carne
        } else if (meatCount > 0) {
            newContent = BowlContent.MEAT;

            // 4) Solo verduras/hojas
        } else if (vegCount > 0) {
            newContent = BowlContent.VEG;

            // 5) Vacío
        } else {
            newContent = BowlContent.EMPTY;
        }

        if (state.getValue(BowlBlock.CONTENT) != newContent) {
            level.setBlock(worldPosition, state.setValue(BowlBlock.CONTENT, newContent), 3);
        }
    }


    // Utilidades de clasificación
    public static class BowlLogic {

        public static boolean isMeat(ItemStack stack) {
            return stack.is(net.minecraft.world.item.Items.BEEF)
                    || stack.is(net.minecraft.world.item.Items.PORKCHOP)
                    || stack.is(net.minecraft.world.item.Items.CHICKEN)
                    || stack.is(net.minecraft.world.item.Items.MUTTON)
                    || stack.is(net.minecraft.world.item.Items.RABBIT)
                    || stack.is(net.minecraft.world.item.Items.SALMON)
                    || stack.is(net.minecraft.world.item.Items.COD);
            // añade aquí tus carnes custom si quieres
        }

        public static boolean isVegOrLeaves(ItemStack stack) {
            // Verduras / frutas
            boolean veg = stack.is(net.minecraft.world.item.Items.CARROT)
                    || stack.is(net.minecraft.world.item.Items.POTATO)
                    || stack.is(net.minecraft.world.item.Items.BEETROOT)
                    || stack.is(net.minecraft.world.item.Items.WHEAT)
                    || stack.is(net.minecraft.world.item.Items.MELON_SLICE)
                    || stack.is(net.minecraft.world.item.Items.APPLE);

            // Hojas (bloques convertidos a ítems)
            boolean leaves = stack.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.BIRCH_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.JUNGLE_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.ACACIA_LEAVES.asItem())
                    || stack.is(net.minecraft.world.level.block.Blocks.DARK_OAK_LEAVES.asItem());

            return veg || leaves;
        }

        public static boolean isWater(ItemStack stack) {
            // Cubo de agua
            if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                return true;
            }

            // Botella de agua (poción de agua)
            if (stack.is(net.minecraft.world.item.Items.POTION)) {
                return net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack)
                        == net.minecraft.world.item.alchemy.Potions.WATER;
            }

            return false;
        }
    }
}
