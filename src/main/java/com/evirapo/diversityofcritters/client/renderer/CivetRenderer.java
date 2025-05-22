package com.evirapo.diversityofcritters.client.renderer;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.client.models.CivetModel;
import com.evirapo.diversityofcritters.client.models.LionCubModel;
import com.evirapo.diversityofcritters.client.models.LionModel;
import com.evirapo.diversityofcritters.client.models.ModelLayers;
import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.common.entity.custom.LionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CivetRenderer<T extends CivetEntity> extends MobRenderer<T, CivetModel<T>> {

    private static final ResourceLocation LION_MALE = new ResourceLocation(DiversityOfCritters.MODID,"textures/entity/civet/civet_tan.png");
    public CivetRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new CivetModel<>(pContext.bakeLayer(ModelLayers.CIVET_LAYER)), 0.35F);
    }

    @Override
    public void render(@NotNull T pEntity, float pEntityYaw, float pPartialTicks, @NotNull PoseStack pMatrixStack, @NotNull MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CivetEntity pEntity) {
        return LION_MALE;
    }
}
