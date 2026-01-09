package com.evirapo.diversityofcritters.common.item;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.common.block.DOCBlocks;
import com.evirapo.diversityofcritters.common.entity.DOCEntities;
import com.evirapo.diversityofcritters.common.item.custom.BrushItem;
import com.evirapo.diversityofcritters.common.item.custom.GauzeBandageItem;
import net.minecraft.world.item.BlockItem;
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

    public static final RegistryObject<Item> ZOO_BOOK = ITEM_TYPES.register("zoo_book",
            ()-> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ENTITY_TAB_ICON = ITEM_TYPES.register("entity_tab_icon",
            ()-> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TRAINING_STICK = ITEM_TYPES.register("training_stick",
            ()-> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BOWL = ITEM_TYPES.register("bowl",
            () -> new BlockItem(DOCBlocks.BOWL.get(), new Item.Properties()));

    public static final RegistryObject<Item> DIG_BOX = ITEM_TYPES.register("dig_box",
            () -> new BlockItem(DOCBlocks.DIG_BOX.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRUSH = ITEM_TYPES.register("brush",
            () -> new BrushItem(new Item.Properties().stacksTo(1).durability(64))); // Durabilidad 64 usos

    public static final RegistryObject<Item> GAUZE_BANDAGE = ITEM_TYPES.register("gauze_bandage",
            () -> new GauzeBandageItem(new Item.Properties().stacksTo(16))); // Stackeable hasta 16

    public static void register(IEventBus eventBus) {
        ITEM_TYPES.register(eventBus);
    }

}
