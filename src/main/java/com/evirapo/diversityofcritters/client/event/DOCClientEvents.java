package com.evirapo.diversityofcritters.client.event;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.client.models.*;
import com.evirapo.diversityofcritters.client.renderer.CivetRenderer;
import com.evirapo.diversityofcritters.client.renderer.LionRenderer;
import com.evirapo.diversityofcritters.common.entity.DOCEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = DiversityOfCritters.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DOCClientEvents {

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModelLayers.LION_LAYER, LionModel::createBodyLayer);
        event.registerLayerDefinition(ModelLayers.LION_CUB_LAYER, LionCubModel::createBodyLayer);

        event.registerLayerDefinition(ModelLayers.CIVET_LAYER, CivetModel::createBodyLayer);
        event.registerLayerDefinition(ModelLayers.CIVET_BABY_LAYER, CivetBabyModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DOCEntities.LION.get(), LionRenderer::new);
        event.registerEntityRenderer(DOCEntities.CIVET.get(), CivetRenderer::new);
    }
}
