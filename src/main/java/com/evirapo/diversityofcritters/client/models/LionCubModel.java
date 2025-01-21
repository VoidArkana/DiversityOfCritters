package com.evirapo.diversityofcritters.client.models;// Made with Blockbench 4.11.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.evirapo.diversityofcritters.client.animations.LionAnimations;
import com.evirapo.diversityofcritters.common.entity.custom.LionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class LionCubModel<T extends LionEntity> extends HierarchicalModel<T> {
	private final ModelPart LionCub;
	private final ModelPart Body;
	private final ModelPart Head2;
	private final ModelPart Head;
	private final ModelPart Ear2;
	private final ModelPart Ear;
	private final ModelPart Jaw;
	private final ModelPart TopJaw;
	private final ModelPart BottomJaw;
	private final ModelPart Tongue;
	private final ModelPart Eyes;
	private final ModelPart Eye;
	private final ModelPart Eye2;
	private final ModelPart Mane;
	private final ModelPart Torse;
	private final ModelPart Tail;
	private final ModelPart Leg4;
	private final ModelPart Leg3;
	private final ModelPart Leg2;
	private final ModelPart Leg;

	public LionCubModel(ModelPart root) {
		this.LionCub = root.getChild("LionCub");
		this.Body = this.LionCub.getChild("Body");
		this.Head2 = this.Body.getChild("Head2");
		this.Head = this.Head2.getChild("Head");
		this.Ear2 = this.Head2.getChild("Ear2");
		this.Ear = this.Head2.getChild("Ear");
		this.Jaw = this.Head2.getChild("Jaw");
		this.TopJaw = this.Jaw.getChild("TopJaw");
		this.BottomJaw = this.Jaw.getChild("BottomJaw");
		this.Tongue = this.BottomJaw.getChild("Tongue");
		this.Eyes = this.Head2.getChild("Eyes");
		this.Eye = this.Eyes.getChild("Eye");
		this.Eye2 = this.Eyes.getChild("Eye2");
		this.Mane = this.Head2.getChild("Mane");
		this.Torse = this.Body.getChild("Torse");
		this.Tail = this.Body.getChild("Tail");
		this.Leg4 = this.LionCub.getChild("Leg4");
		this.Leg3 = this.LionCub.getChild("Leg3");
		this.Leg2 = this.LionCub.getChild("Leg2");
		this.Leg = this.LionCub.getChild("Leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition LionCub = partdefinition.addOrReplaceChild("LionCub", CubeListBuilder.create(), PartPose.offset(1.0F, 20.0F, -3.0F));

		PartDefinition Body = LionCub.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.offset(-1.0F, 4.0F, 3.0F));

		PartDefinition Head2 = Body.addOrReplaceChild("Head2", CubeListBuilder.create(), PartPose.offset(0.0F, -11.5F, -7.0F));

		PartDefinition Head = Head2.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(28, 21).addBox(-4.5F, -2.0F, -2.5F, 7.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, -0.5F, -3.5F));

		PartDefinition Ear2 = Head2.addOrReplaceChild("Ear2", CubeListBuilder.create().texOffs(25, 9).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -2.5F, -2.5F));

		PartDefinition Ear = Head2.addOrReplaceChild("Ear", CubeListBuilder.create().texOffs(25, 2).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -2.5F, -2.5F));

		PartDefinition Jaw = Head2.addOrReplaceChild("Jaw", CubeListBuilder.create(), PartPose.offset(1.0F, 18.5F, 14.0F));

		PartDefinition TopJaw = Jaw.addOrReplaceChild("TopJaw", CubeListBuilder.create().texOffs(38, 0).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(39, 9).addBox(-1.25F, 0.25F, -1.75F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(39, 10).addBox(1.25F, 0.25F, -1.75F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, -18.0F, -20.0F));

		PartDefinition BottomJaw = Jaw.addOrReplaceChild("BottomJaw", CubeListBuilder.create().texOffs(38, 4).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, -17.0F, -20.0F));

		PartDefinition Tongue = BottomJaw.addOrReplaceChild("Tongue", CubeListBuilder.create().texOffs(38, 7).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.25F, 0.0F));

		PartDefinition Eyes = Head2.addOrReplaceChild("Eyes", CubeListBuilder.create(), PartPose.offset(0.0F, 11.5F, 7.0F));

		PartDefinition Eye = Eyes.addOrReplaceChild("Eye", CubeListBuilder.create().texOffs(24, 35).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, -11.5F, -13.025F));

		PartDefinition Eye2 = Eyes.addOrReplaceChild("Eye2", CubeListBuilder.create().texOffs(24, 36).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.5F, -11.5F, -13.025F));

		PartDefinition Mane = Head2.addOrReplaceChild("Mane", CubeListBuilder.create(), PartPose.offset(1.0F, 0.0F, 0.0F));

		PartDefinition Torse = Body.addOrReplaceChild("Torse", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -4.0F, -7.0F, 5.0F, 7.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.5F, -8.0F, 0.0F));

		PartDefinition Tail = Body.addOrReplaceChild("Tail", CubeListBuilder.create().texOffs(0, 21).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -10.0F, 7.0F));

		PartDefinition Leg4 = LionCub.addOrReplaceChild("Leg4", CubeListBuilder.create().texOffs(0, 35).addBox(-0.5F, 0.0F, -0.5F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.25F, -5.0F, -1.5F));

		PartDefinition Leg3 = LionCub.addOrReplaceChild("Leg3", CubeListBuilder.create().texOffs(8, 35).addBox(-0.5F, 0.0F, -0.5F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.25F, -5.0F, -1.5F));

		PartDefinition Leg2 = LionCub.addOrReplaceChild("Leg2", CubeListBuilder.create().texOffs(30, 33).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.75F, -5.0F, 8.0F));

		PartDefinition Leg = LionCub.addOrReplaceChild("Leg", CubeListBuilder.create().texOffs(16, 35).addBox(-0.5F, 0.0F, -0.5F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.25F, -5.0F, 7.5F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(LionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		if (entity.isSleeping()){

			this.animate(entity.idleAnimationState, LionAnimations.Sleep, ageInTicks, 1.0F);

		}else {
			this.Head2.xRot = headPitch * ((float)Math.PI / 180F);
			this.Head2.yRot = netHeadYaw * ((float)Math.PI / 180F);

			this.animateWalk(LionAnimations.Walk, limbSwing, limbSwingAmount, 2.0F, 2.5F);

			this.animate(entity.idleAnimationState, LionAnimations.Idle, ageInTicks, 1.0F);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		LionCub.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart root() {
		return LionCub;
	}
}