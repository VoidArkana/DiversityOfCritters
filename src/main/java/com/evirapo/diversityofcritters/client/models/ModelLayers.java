package com.evirapo.diversityofcritters.client.models;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class ModelLayers {
    public static final ModelLayerLocation LION_LAYER = new ModelLayerLocation(new ResourceLocation(DiversityOfCritters.MODID, "lion"), "main");
    public static final ModelLayerLocation LION_CUB_LAYER = new ModelLayerLocation(new ResourceLocation(DiversityOfCritters.MODID, "lion_cub"), "main");

    public static final ModelLayerLocation CIVET_LAYER = new ModelLayerLocation(new ResourceLocation(DiversityOfCritters.MODID, "civet"), "main");
}
