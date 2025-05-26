package com.evirapo.diversityofcritters.client.menu;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseInventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class DOCStatsMenu extends AbstractContainerMenu {
    private final Container ratInventory;

    public DOCStatsMenu(int pContainerId, Container ratInventory, Inventory playerInventory) {
        super(DOCMenuTypes.STATS_MENU.get(), pContainerId);
        this.ratInventory = ratInventory;
        ratInventory.startOpen(playerInventory.player);
    }

    public DOCStatsMenu(int i, Inventory inventory) {
        this(i, new SimpleContainer(0), inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.ratInventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.ratInventory.stopOpen(player);
        if (this.ratInventory instanceof DiverseInventory container) {
            container.getAnimal().closeInventory();
        }
    }
}
