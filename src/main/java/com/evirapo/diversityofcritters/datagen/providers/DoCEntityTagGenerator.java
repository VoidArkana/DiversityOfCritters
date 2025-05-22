package com.evirapo.diversityofcritters.datagen.providers;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class DoCEntityTagGenerator extends EntityTypeTagsProvider {

    public DoCEntityTagGenerator(PackOutput p_256095_, CompletableFuture<HolderLookup.Provider> p_256572_, @Nullable ExistingFileHelper existingFileHelper) {
        super(p_256095_, p_256572_, DiversityOfCritters.MODID, existingFileHelper);
    }

    protected void addTags(HolderLookup.Provider pProvider) {

    }
}
