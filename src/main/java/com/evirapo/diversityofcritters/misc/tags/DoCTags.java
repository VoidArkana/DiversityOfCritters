package com.evirapo.diversityofcritters.misc.tags;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

public class DoCTags {

    public static class Items {

        public static final TagKey<Item> CIVET_FOOD = create("is_civet_food");

        private static TagKey<Item> create(String pName) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(DiversityOfCritters.MODID, pName));
        }
    }

}
