package com.evirapo.diversityofcritters.misc.creative;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.common.item.DOCItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class DOCEntitiesCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DiversityOfCritters.MODID);

    public static final RegistryObject<CreativeModeTab> MARVELOUS_MENAGERIE_TAB =
            CREATIVE_MODE_TABS.register("docentities_tab", ()-> CreativeModeTab.builder().icon(() -> new ItemStack(DOCItems.ENTITY_TAB_ICON.get()))
                    .title(Component.translatable("creativetab.docentities_tab"))
                    .displayItems((itemDisplayParameters, output) -> {

                        output.accept(DOCItems.LION_SPAWN_EGG.get());

                    })
                    .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
