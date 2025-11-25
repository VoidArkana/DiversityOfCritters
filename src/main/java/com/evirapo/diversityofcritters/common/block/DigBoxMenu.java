package com.evirapo.diversityofcritters.common.block;

import com.evirapo.diversityofcritters.client.menu.DOCMenuTypes;
import com.evirapo.diversityofcritters.common.block.entity.DigBoxBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class DigBoxMenu extends AbstractContainerMenu {
    public final DigBoxBlockEntity blockEntity;
    private final Level level;
    private final IItemHandler internalItemHandler;

    private static final int DIGBOX_SLOT_COUNT = 9;

    private static final int SLOT_X_START = 111;
    private static final int SLOT_Y_START = 17;
    private static final int SLOT_SIZE = 18;

    public DigBoxMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public DigBoxMenu(int pContainerId, Inventory inv, BlockEntity entity) {
        super(DOCMenuTypes.DIG_BOX_MENU.get(), pContainerId);
        checkContainerSize(inv, DIGBOX_SLOT_COUNT);
        blockEntity = ((DigBoxBlockEntity) entity);
        this.level = inv.player.level();

        this.internalItemHandler = this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);

        if (internalItemHandler != null) {
            int index = 0;
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 3; ++col) {
                    this.addSlot(new SlotItemHandler(internalItemHandler, index++,
                            SLOT_X_START + col * SLOT_SIZE,
                            SLOT_Y_START + row * SLOT_SIZE) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return internalItemHandler.isItemValid(0, stack);
                        }
                    });
                }
            }
        }

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    private static final int VANILLA_FIRST_SLOT_INDEX = DIGBOX_SLOT_COUNT; // 9
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_END_SLOT_INDEX = DIGBOX_SLOT_COUNT; // 9

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < TE_INVENTORY_END_SLOT_INDEX) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            if (this.internalItemHandler != null && this.internalItemHandler.isItemValid(0, sourceStack)) { // check genérico
                if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_END_SLOT_INDEX, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer, com.evirapo.diversityofcritters.common.block.DOCBlocks.DIG_BOX.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}