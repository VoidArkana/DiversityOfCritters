package com.evirapo.diversityofcritters.common.item;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.common.entity.DOCEntities;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DOCItems {
    public static final DeferredRegister<Item> ITEM_TYPES =
        DeferredRegister.create(ForgeRegistries.ITEMS, DiversityOfCritters.MODID);

    public static final RegistryObject<Item> LION_SPAWN_EGG = ITEM_TYPES.register("lion_spawn_egg",
            ()-> new ForgeSpawnEggItem(DOCEntities.LION, 0xffffff, 0xffffff, new Item.Properties()));

    public static final RegistryObject<Item> CIVET_SPAWN_EGG = ITEM_TYPES.register("civet_spawn_egg",
            ()-> new ForgeSpawnEggItem(DOCEntities.CIVET, 0xffffff, 0xffffff, new Item.Properties()));

    public static final RegistryObject<Item> ENTITY_TAB_ICON = ITEM_TYPES.register("entity_tab_icon",
            ()-> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEM_TYPES.register(eventBus);
    }

}
