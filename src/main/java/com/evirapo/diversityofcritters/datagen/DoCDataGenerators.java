package com.evirapo.diversityofcritters.datagen;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.datagen.providers.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = DiversityOfCritters.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DoCDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new DoCItemModelProvider(packOutput, existingFileHelper));

        DoCBlockTagGenerator blockTagGenerator = generator.addProvider(event.includeServer(),
                new DoCBlockTagGenerator(packOutput, lookupProvider, existingFileHelper));

        generator.addProvider(event.includeServer(),new DoCItemTagGenerator(packOutput, lookupProvider, blockTagGenerator.contentsGetter(), existingFileHelper));

        generator.addProvider(true, new DoCEntityTagGenerator(packOutput, lookupProvider, existingFileHelper));

        generator.addProvider(true, new DoCBiomeTagGenerator(packOutput, lookupProvider, existingFileHelper));
    }
}
