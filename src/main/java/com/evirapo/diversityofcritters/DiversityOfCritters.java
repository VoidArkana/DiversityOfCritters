package com.evirapo.diversityofcritters;

import com.evirapo.diversityofcritters.client.menu.DOCMenuTypes;
import com.evirapo.diversityofcritters.common.entity.DOCEntities;
import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.common.item.DOCItems;
import com.evirapo.diversityofcritters.misc.creative.DOCEntitiesCreativeTab;
import com.evirapo.diversityofcritters.network.DOCNetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DiversityOfCritters.MODID)
public class DiversityOfCritters
{
    public static final String MODID = "diversityofcritters";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DiversityOfCritters()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        DOCItems.register(modEventBus);
        DOCEntities.register(modEventBus);
        DOCMenuTypes.register(modEventBus);
        DOCEntitiesCreativeTab.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        //modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        DOCNetworkHandler.init();

        SpawnPlacements.register(DOCEntities.CIVET.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, CivetEntity::checkCivetSpawnRules);

    }

//    private void addCreative(BuildCreativeModeTabContentsEvent event)
//    {
//
//    }
}
