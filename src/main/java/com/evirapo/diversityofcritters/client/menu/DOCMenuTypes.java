package com.evirapo.diversityofcritters.client.menu;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DOCMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, DiversityOfCritters.MODID);

    public static final RegistryObject<MenuType<DOCStatsMenu>> STATS_MENU = MENUS.register("stats_menu", () -> new MenuType<>(DOCStatsMenu::new, FeatureFlags.REGISTRY.allFlags()));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
