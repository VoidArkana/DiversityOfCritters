package com.evirapo.diversityofcritters.common.entity;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.common.entity.custom.LionEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DOCEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DiversityOfCritters.MODID);

    public static final RegistryObject<EntityType<LionEntity>> LION =
            ENTITY_TYPES.register("lion",
                    () -> EntityType.Builder.of(LionEntity::new, MobCategory.CREATURE)
                            .sized(1f, 1f)
                            .build(new ResourceLocation(DiversityOfCritters.MODID, "lion").toString()));

    public static final RegistryObject<EntityType<CivetEntity>> CIVET =
            ENTITY_TYPES.register("civet",
                    () -> EntityType.Builder.of(CivetEntity::new, MobCategory.CREATURE)
                            .sized(0.5f, 0.4f)
                            .build(new ResourceLocation(DiversityOfCritters.MODID, "civet").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
