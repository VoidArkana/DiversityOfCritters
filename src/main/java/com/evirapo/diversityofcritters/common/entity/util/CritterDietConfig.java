package com.evirapo.diversityofcritters.common.entity.util;

public class CritterDietConfig {

    public boolean acceptsMeat;
    public boolean acceptsVeg;
    public boolean acceptsMix;

    public int hungerPerMeatBowl;
    public int hungerPerVegBowl;
    public int hungerPerMixBowl;

    public int thirstPerWaterBowl;

    public CritterDietConfig(boolean acceptsMeat, boolean acceptsVeg, boolean acceptsMix,
                             int hungerPerMeatBowl, int hungerPerVegBowl, int hungerPerMixBowl,
                             int thirstPerWaterBowl) {
        this.acceptsMeat = acceptsMeat;
        this.acceptsVeg = acceptsVeg;
        this.acceptsMix = acceptsMix;
        this.hungerPerMeatBowl = hungerPerMeatBowl;
        this.hungerPerVegBowl = hungerPerVegBowl;
        this.hungerPerMixBowl = hungerPerMixBowl;
        this.thirstPerWaterBowl = thirstPerWaterBowl;
    }
}

