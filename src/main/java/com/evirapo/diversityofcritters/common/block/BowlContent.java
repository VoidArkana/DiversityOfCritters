package com.evirapo.diversityofcritters.common.block;

import net.minecraft.util.StringRepresentable;

public enum BowlContent implements StringRepresentable {
    EMPTY("empty"),
    MEAT("meat"),
    VEG("veg"),
    MIX("mix"),
    WATER("water");

    private final String name;

    BowlContent(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
