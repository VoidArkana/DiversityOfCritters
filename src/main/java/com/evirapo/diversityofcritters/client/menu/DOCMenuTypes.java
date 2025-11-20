package com.evirapo.diversityofcritters.client.menu;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.common.block.BowlMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DOCMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, DiversityOfCritters.MODID);

    public static final RegistryObject<MenuType<DOCStatsMenu>> STATS_MENU = MENUS.register("stats_menu", () -> new MenuType<>(DOCStatsMenu::new, FeatureFlags.REGISTRY.allFlags()));

    public static final RegistryObject<MenuType<BowlMenu>> BOWL =
            MENUS.register("bowl",
                    () -> IForgeMenuType.create(BowlMenu::new)
            );

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
