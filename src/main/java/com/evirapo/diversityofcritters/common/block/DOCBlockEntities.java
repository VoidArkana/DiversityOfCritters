package com.evirapo.diversityofcritters.common.block;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.common.block.entity.BowlBlockEntity;
import com.evirapo.diversityofcritters.common.block.entity.DigBoxBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DOCBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DiversityOfCritters.MODID);

    public static final RegistryObject<BlockEntityType<BowlBlockEntity>> BOWL_BE =
            BLOCK_ENTITIES.register("bowl",
                    () -> BlockEntityType.Builder.of(
                            BowlBlockEntity::new,
                            DOCBlocks.BOWL.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<DigBoxBlockEntity>> DIG_BOX_BE =
            BLOCK_ENTITIES.register("dig_box_be", () ->
                    BlockEntityType.Builder.of(DigBoxBlockEntity::new,
                            DOCBlocks.DIG_BOX.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
