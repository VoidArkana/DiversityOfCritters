package com.evirapo.diversityofcritters.client.renderer;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.client.models.LionCubModel;
import com.evirapo.diversityofcritters.client.models.LionModel;
import com.evirapo.diversityofcritters.client.models.ModelLayers;
import com.evirapo.diversityofcritters.common.entity.custom.LionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class LionRenderer<T extends LionEntity> extends MobRenderer<T, HierarchicalModel<T>> {

    private static final ResourceLocation LION_MALE = new ResourceLocation(DiversityOfCritters.MODID,"textures/entity/lion/lion_m.png");
    private static final ResourceLocation LION_FEMALE = new ResourceLocation(DiversityOfCritters.MODID,"textures/entity/lion/lion_f.png");
    private static final ResourceLocation LION_CUB = new ResourceLocation(DiversityOfCritters.MODID,"textures/entity/lion/lion_baby.png");

    public HierarchicalModel<T> adultModel;
    public HierarchicalModel<T> babyModel;

    public LionRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new LionModel<>(pContext.bakeLayer(ModelLayers.LION_LAYER)), 1.0F);
        this.adultModel = new LionModel<>(pContext.bakeLayer(ModelLayers.LION_LAYER));
        this.babyModel = new LionCubModel<>(pContext.bakeLayer(ModelLayers.LION_CUB_LAYER));
    }

    @Override
    public void render(@NotNull T pEntity, float pEntityYaw, float pPartialTicks, @NotNull PoseStack pMatrixStack, @NotNull MultiBufferSource pBuffer, int pPackedLight) {
        if (this.babyModel != null) {
            if(pEntity.isBaby()){
                this.model = this.babyModel;
                pMatrixStack.scale(0.6f, 0.6f, 0.6f);
            }else {
                this.model = this.adultModel;

                if (pEntity.getIsMale()){
                    pMatrixStack.scale(1f, 1f, 1f);
                }else {
                    pMatrixStack.scale(0.9f, 0.9f, 0.9f);
                }

                if (pEntity.isSleeping()){
                    pMatrixStack.translate(0, -0.3, 0);
                }
            }
        }
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LionEntity pEntity) {
        return pEntity.isBaby() ? LION_CUB : pEntity.getIsMale() ? LION_MALE : LION_FEMALE;
    }
}
