package com.evirapo.diversityofcritters.common.entity.custom.base;

import net.minecraft.world.SimpleContainer;

public class DiverseInventory extends SimpleContainer {

    private final DiverseCritter animal;

    public DiverseInventory(DiverseCritter animal) {
        super(0);
        this.animal = animal;
    }

    public DiverseCritter getAnimal() {
        return this.animal;
    }
}
