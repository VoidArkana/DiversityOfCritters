package com.evirapo.diversityofcritters.common.block;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DOCBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DiversityOfCritters.MODID);

    public static final RegistryObject<Block> BOWL = BLOCKS.register("bowl",
            () -> new BowlBlock(
                    BlockBehaviour.Properties
                            .copy(Blocks.OAK_PLANKS)
                            .noOcclusion()   // importante para modelos “huecos”
            )
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

