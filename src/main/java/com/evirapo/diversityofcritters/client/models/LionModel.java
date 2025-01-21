package com.evirapo.diversityofcritters.client.models;// Made with Blockbench 4.11.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.evirapo.diversityofcritters.DiversityOfCritters;
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

public class LionModel<T extends LionEntity> extends HierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(DiversityOfCritters.MODID, "lion"), "main");
	private final ModelPart Lion;
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
	private final ModelPart Mane_Torse;
	private final ModelPart Tail;
	private final ModelPart Second_Tail;
	private final ModelPart Leg4;
	private final ModelPart Leg3;
	private final ModelPart Leg2;
	private final ModelPart Leg;

	public LionModel(ModelPart root) {
		this.Lion = root.getChild("Lion");
		this.Body = this.Lion.getChild("Body");
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
		this.Mane_Torse = this.Torse.getChild("Mane_Torse");
		this.Tail = this.Body.getChild("Tail");
		this.Second_Tail = this.Tail.getChild("Second_Tail");
		this.Leg4 = this.Lion.getChild("Leg4");
		this.Leg3 = this.Lion.getChild("Leg3");
		this.Leg2 = this.Lion.getChild("Leg2");
		this.Leg = this.Lion.getChild("Leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Lion = partdefinition.addOrReplaceChild("Lion", CubeListBuilder.create(), PartPose.offset(0.0F, 18.0F, -6.0F));

		PartDefinition Body = Lion.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.offset(0.0F, 6.0F, 6.0F));

		PartDefinition Head2 = Body.addOrReplaceChild("Head2", CubeListBuilder.create(), PartPose.offset(0.0F, -19.0F, -13.0F));

		PartDefinition Head = Head2.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 67).addBox(-5.5F, -4.0F, -3.5F, 11.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -3.5F));

		PartDefinition Ear2 = Head2.addOrReplaceChild("Ear2", CubeListBuilder.create().texOffs(84, 89).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -4.0F, -3.5F));

		PartDefinition Ear = Head2.addOrReplaceChild("Ear", CubeListBuilder.create().texOffs(84, 32).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -4.0F, -3.5F));

		PartDefinition Jaw = Head2.addOrReplaceChild("Jaw", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 13.0F));

		PartDefinition TopJaw = Jaw.addOrReplaceChild("TopJaw", CubeListBuilder.create().texOffs(70, 24).addBox(-2.5F, -1.5F, -5.0F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(58, 97).addBox(0.25F, 1.5F, -4.75F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(49, 97).addBox(-2.25F, 1.5F, -4.75F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -18.5F, -20.0F));

		PartDefinition BottomJaw = Jaw.addOrReplaceChild("BottomJaw", CubeListBuilder.create().texOffs(48, 89).addBox(-2.5F, 0.0F, -5.0F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(1, 102).addBox(-2.05F, -2.0F, -4.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -17.0F, -20.0F));

		PartDefinition Tongue = BottomJaw.addOrReplaceChild("Tongue", CubeListBuilder.create().texOffs(70, 32).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 0.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.25F, 0.0F));

		PartDefinition Eyes = Head2.addOrReplaceChild("Eyes", CubeListBuilder.create(), PartPose.offset(-0.5F, 3.0F, -4.025F));

		PartDefinition Eye = Eyes.addOrReplaceChild("Eye", CubeListBuilder.create().texOffs(70, 37).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -3.5F, -3.0F));

		PartDefinition Eye2 = Eyes.addOrReplaceChild("Eye2", CubeListBuilder.create().texOffs(70, 36).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -3.5F, -3.0F));

		PartDefinition Mane = Head2.addOrReplaceChild("Mane", CubeListBuilder.create().texOffs(52, 66).addBox(-7.5F, -6.5F, -5.0F, 15.0F, 13.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.5F, 0.0F));

		PartDefinition Torse = Body.addOrReplaceChild("Torse", CubeListBuilder.create().texOffs(0, 38).addBox(0.0F, -7.5F, -13.0F, 0.0F, 3.0F, 26.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.5F, -6.0F, -13.0F, 9.0F, 12.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -14.0F, 0.0F));

		PartDefinition Mane_Torse = Torse.addOrReplaceChild("Mane_Torse", CubeListBuilder.create().texOffs(52, 38).addBox(-5.5F, -7.0F, -7.0F, 11.0F, 14.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -7.0F));

		PartDefinition Tail = Body.addOrReplaceChild("Tail", CubeListBuilder.create().texOffs(70, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -18.0F, 13.0F));

		PartDefinition Second_Tail = Tail.addOrReplaceChild("Second_Tail", CubeListBuilder.create().texOffs(70, 12).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(68, 89).addBox(-1.5F, -1.5F, 10.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 10.0F));

		PartDefinition Leg4 = Lion.addOrReplaceChild("Leg4", CubeListBuilder.create().texOffs(36, 67).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.75F, -9.0F, -3.0F));

		PartDefinition Leg3 = Lion.addOrReplaceChild("Leg3", CubeListBuilder.create().texOffs(0, 82).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(2.75F, -9.0F, -3.0F));

		PartDefinition Leg2 = Lion.addOrReplaceChild("Leg2", CubeListBuilder.create().texOffs(32, 86).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(2.75F, -9.0F, 15.0F));

		PartDefinition Leg = Lion.addOrReplaceChild("Leg", CubeListBuilder.create().texOffs(16, 82).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.75F, -9.0F, 15.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
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
		Lion.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart root() {
		return Lion;
	}
}