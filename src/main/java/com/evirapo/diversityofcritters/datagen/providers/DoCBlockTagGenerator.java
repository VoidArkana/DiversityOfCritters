package com.evirapo.diversityofcritters.datagen.providers;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DoCBlockTagGenerator extends BlockTagsProvider {


    public DoCBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, DiversityOfCritters.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(DoCTags.Blocks.CIVET_CLIMBABLE)
                .addTag(BlockTags.DIRT)
                .addTag(BlockTags.LOGS)
                .addTag(BlockTags.STONE_ORE_REPLACEABLES);
    }
}
