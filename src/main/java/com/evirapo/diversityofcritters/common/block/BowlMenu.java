package com.evirapo.diversityofcritters.common.block;

import com.evirapo.diversityofcritters.client.menu.DOCMenuTypes;
import com.evirapo.diversityofcritters.common.block.entity.BowlBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BowlMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerLevelAccess access;

    public BowlMenu(int id, Inventory playerInv, BowlBlockEntity be) {
        this(id, playerInv, be.getInventory(), be.getBlockPos());
    }

    public BowlMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, getContainerFromBuf(playerInv, extraData));
    }

    private BowlMenu(int id, Inventory playerInv, Container container, BlockPos pos) {
        super(DOCMenuTypes.BOWL.get(), id);
        this.container = container;
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);

        checkContainerSize(container, 9);
        container.startOpen(playerInv.player);

        int startX = 111;
        int startYSlots = 17;
        int index = 0;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new BowlSlot(container, index++,
                        startX + col * 18,
                        startYSlots + row * 18));
            }
        }

        int invStartY = 84;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18,
                        invStartY + row * 18));  // 84, 102, 120
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col,
                    8 + col * 18,
                    142));
        }
    }

    private static Container getContainerFromBuf(Inventory playerInv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);

        if (be instanceof BowlBlockEntity bowlBe) {
            return bowlBe.getInventory();
        }

        return new SimpleContainer(9);
    }

    private BowlMenu(int id, Inventory playerInv, Container container) {
        this(id, playerInv, container, BlockPos.ZERO);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            int containerSlots = this.container.getContainerSize(); // ahora 9

            if (index < containerSlots) {
                if (!this.moveItemStackTo(stack, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, containerSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public static class BowlSlot extends Slot {

        public BowlSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            boolean isMeat  = BowlBlockEntity.BowlLogic.isMeat(stack);
            boolean isVeg   = BowlBlockEntity.BowlLogic.isVegOrLeaves(stack);
            boolean isWater = BowlBlockEntity.BowlLogic.isWater(stack);

            if (!isMeat && !isVeg && !isWater) {
                return false;
            }

            boolean hasMeat  = false;
            boolean hasVeg   = false;
            boolean hasWater = false;

            Container c = this.container;
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (s.isEmpty()) continue;

                if (BowlBlockEntity.BowlLogic.isWater(s)) {
                    hasWater = true;
                } else if (BowlBlockEntity.BowlLogic.isMeat(s)) {
                    hasMeat = true;
                } else if (BowlBlockEntity.BowlLogic.isVegOrLeaves(s)) {
                    hasVeg = true;
                }
            }

            if (isWater) hasWater = true;
            if (isMeat)  hasMeat  = true;
            if (isVeg)   hasVeg   = true;

            if (hasWater && (hasMeat || hasVeg)) {
                return false;
            }

            return true;
        }
    }

}
