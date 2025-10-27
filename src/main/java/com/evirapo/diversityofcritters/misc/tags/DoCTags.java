package com.evirapo.diversityofcritters.misc.tags;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class DoCTags {

    public static class Items {

        public static final TagKey<Item> CIVET_FOOD = create("is_civet_food");

        private static TagKey<Item> create(String pName) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(DiversityOfCritters.MODID, pName));
        }
    }

    public static class Blocks {

        public static final TagKey<Block> CIVET_CLIMBABLE = create("civet_climbable");

        private static TagKey<Block> create(String pName) {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(DiversityOfCritters.MODID, pName));
        }
    }

    public static class Biomes {

        public static final TagKey<Biome> CIVET_BIOMES = create("is_civet_biome");

        private static TagKey<Biome> create(String pName) {
            return TagKey.create(Registries.BIOME, new ResourceLocation(DiversityOfCritters.MODID, pName));
        }
    }

}
