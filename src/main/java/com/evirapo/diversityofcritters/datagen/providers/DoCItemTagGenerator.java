package com.evirapo.diversityofcritters.datagen.providers;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DoCItemTagGenerator extends ItemTagsProvider {
    public DoCItemTagGenerator(PackOutput p_275343_, CompletableFuture<HolderLookup.Provider> p_275729_, CompletableFuture<TagLookup<Block>> p_275322_, @Nullable ExistingFileHelper existingFileHelper) {
        super(p_275343_, p_275729_, p_275322_, DiversityOfCritters.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        this.tag(DoCTags.Items.CIVET_FOOD).add(
                Items.RABBIT,
                Items.COOKED_RABBIT,
                Items.CHICKEN,
                Items.COOKED_CHICKEN,
                Items.BEEF,
                Items.COOKED_BEEF,
                Items.MUTTON,
                Items.COOKED_MUTTON,
                Items.PORKCHOP,
                Items.COOKED_PORKCHOP
        );
    }
}
