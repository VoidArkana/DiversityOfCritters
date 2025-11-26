package com.evirapo.diversityofcritters.client.models;

import com.evirapo.diversityofcritters.client.animations.CivetAnims;
import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import org.joml.Vector3f;

public class CivetModel<T extends CivetEntity> extends HierarchicalModel<T> {
	private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();

	private final ModelPart BandedPlamCivet;
	private final ModelPart Body;
	private final ModelPart Head;
	private final ModelPart Jaw;
	private final ModelPart BottomJaw;
	private final ModelPart Tongue;
	private final ModelPart TopJaw;
	private final ModelPart Eyes;
	private final ModelPart bone3;
	private final ModelPart bone2;
	private final ModelPart Ear;
	private final ModelPart Ear2;
	private final ModelPart Torse;
	private final ModelPart Tail;
	private final ModelPart Leg4;
	private final ModelPart Leg;
	private final ModelPart Leg2;
	private final ModelPart Leg3;

	public boolean climbing = false;

	public CivetModel(ModelPart root) {
		this.BandedPlamCivet = root.getChild("BandedPlamCivet");
		this.Body = this.BandedPlamCivet.getChild("Body");
		this.Head = this.Body.getChild("Head");
		this.Jaw = this.Head.getChild("Jaw");
		this.BottomJaw = this.Jaw.getChild("BottomJaw");
		this.Tongue = this.BottomJaw.getChild("Tongue");
		this.TopJaw = this.Jaw.getChild("TopJaw");
		this.Eyes = this.Head.getChild("Eyes");
		this.bone3 = this.Eyes.getChild("bone3");
		this.bone2 = this.Eyes.getChild("bone2");
		this.Ear = this.Head.getChild("Ear");
		this.Ear2 = this.Head.getChild("Ear2");
		this.Torse = this.Body.getChild("Torse");
		this.Tail = this.Body.getChild("Tail");
		this.Leg4 = this.BandedPlamCivet.getChild("Leg4");
		this.Leg = this.BandedPlamCivet.getChild("Leg");
		this.Leg2 = this.BandedPlamCivet.getChild("Leg2");
		this.Leg3 = this.BandedPlamCivet.getChild("Leg3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition BandedPlamCivet = partdefinition.addOrReplaceChild("BandedPlamCivet", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition Body = BandedPlamCivet.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.offset(0.25F, -11.0F, 0.0F));

		PartDefinition Head = Body.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(48, 29).addBox(-3.5F, -2.5F, -7.0F, 7.0F, 5.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.2F, -3.5F, -9.0F));

		PartDefinition Jaw = Head.addOrReplaceChild("Jaw", CubeListBuilder.create(), PartPose.offset(1.5F, 1.5F, -13.0F));

		PartDefinition BottomJaw = Jaw.addOrReplaceChild("BottomJaw", CubeListBuilder.create().texOffs(54, 5).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(51, 9).addBox(-1.25F, -0.5F, -3.75F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(51, 10).addBox(1.25F, -0.5F, -3.75F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, -1.0F, 6.0F));

		PartDefinition Tongue = BottomJaw.addOrReplaceChild("Tongue", CubeListBuilder.create().texOffs(46, 1).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.25F, 0.0F));

		PartDefinition TopJaw = Jaw.addOrReplaceChild("TopJaw", CubeListBuilder.create().texOffs(54, 0).addBox(-1.5F, -0.5F, -4.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(51, 10).addBox(1.25F, 0.0F, -3.75F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(51, 9).addBox(-1.25F, 0.0F, -3.75F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, -1.5F, 6.0F));

		PartDefinition Eyes = Head.addOrReplaceChild("Eyes", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -7.0F));

		PartDefinition bone3 = Eyes.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(54, 10).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.5F, 0.0F, -0.025F));

		PartDefinition bone2 = Eyes.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(54, 11).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, 0.0F, -0.025F));

		PartDefinition Ear = Head.addOrReplaceChild("Ear", CubeListBuilder.create().texOffs(36, 53).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.5F, -2.5F, -2.5F));

		PartDefinition Ear2 = Head.addOrReplaceChild("Ear2", CubeListBuilder.create().texOffs(42, 53).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.5F, -2.5F, -2.5F));

		PartDefinition Torse = Body.addOrReplaceChild("Torse", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -4.5F, -10.0F, 7.0F, 9.0F, 20.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.2681F, 0.436F, 1.0F));

		PartDefinition Tail = Body.addOrReplaceChild("Tail", CubeListBuilder.create().texOffs(0, 29).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 20.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, 11.0F));

		PartDefinition Leg4 = BandedPlamCivet.addOrReplaceChild("Leg4", CubeListBuilder.create().texOffs(48, 41).addBox(-1.75F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -11.0F, -6.5F));

		PartDefinition Leg = BandedPlamCivet.addOrReplaceChild("Leg", CubeListBuilder.create().texOffs(24, 53).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.25F, -11.0F, 6.5F));

		PartDefinition Leg2 = BandedPlamCivet.addOrReplaceChild("Leg2", CubeListBuilder.create().texOffs(12, 53).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.25F, -11.0F, 6.5F));

		PartDefinition Leg3 = BandedPlamCivet.addOrReplaceChild("Leg3", CubeListBuilder.create().texOffs(0, 53).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.25F, -11.0F, -6.5F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(CivetEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		boolean sleepingLike = entity.isPreparingSleep() || entity.isSleeping() || entity.isAwakeing();

		this.animate(entity.preparingSleepState, CivetAnims.PREPARING_SLEEP, ageInTicks, 1f);
		this.animate(entity.sleepState,          CivetAnims.SLEEP,          ageInTicks, 1f);
		this.animate(entity.awakeningState,      CivetAnims.AWAKENING,      ageInTicks, 1f);

		this.Head.xRot = headPitch * ((float)Math.PI / 180F);
		this.Head.yRot = netHeadYaw * ((float)Math.PI / 180F);

		if (sleepingLike) {
			limbSwing = 0f;
			limbSwingAmount = 0f;
			this.climbing = false;
			this.animate(entity.drinkingAnimationState, CivetAnims.DRINK, ageInTicks, 1.0F);
			this.animate(entity.attackAnimationState,   CivetAnims.ATTACK, ageInTicks, 1.0F);
		}

		this.climbing = entity.isClimbing();

		if (entity.isClimbing()){
			this.animate(entity.climbingUpState, CivetAnims.CLIMBING_UP, ageInTicks, 1.0F);

			if (entity.isClimbingUp()) {
				this.animateWalk(CivetAnims.CLIMBING_UP, limbSwing, Math.max(0.1f, limbSwingAmount), 2.0f, 2.5f);
			}
		} else {
			if (entity.isInWaterOrBubble()){
				this.animate(entity.idleAnimationState, CivetAnims.SWIM, ageInTicks, 1.0F);
			} else {
				// En tierra
				if (limbSwingAmount > 0.01f) {
					this.animateWalk(entity.isSprinting() ? CivetAnims.RUN : CivetAnims.WALK, limbSwing, limbSwingAmount, 2.0F, 2.5F);
				}

				this.animate(entity.idleAnimationState, CivetAnims.IDLE, ageInTicks, 1.0F);

				this.animate(entity.idleStandUpState,    CivetAnims.STAND_UP,   ageInTicks, 1.0F);
				this.animate(entity.idleSniffLeftState,  CivetAnims.SNIFF_LEFT, ageInTicks, 1.0F);
				this.animate(entity.idleSniffRightState, CivetAnims.SNIFF_RIGHT,ageInTicks, 1.0F);
				this.animate(entity.idleSitState,        CivetAnims.SIT,        ageInTicks, 1.0F);
				this.animate(entity.idleLayState,        CivetAnims.LAY,        ageInTicks, 1.0F);
			}
		}

		this.animate(entity.drinkingAnimationState, CivetAnims.DRINK, ageInTicks, 1.0F);
		this.animate(entity.attackAnimationState,   CivetAnims.ATTACK, ageInTicks, 1.0F);
		this.animate(entity.sitState,   CivetAnims.SIT, ageInTicks, 1.0F);
		this.animate(entity.diggingAnimationState,   CivetAnims.DIGGING, ageInTicks, 1.0F);

		if (this.young){
			this.applyStatic(CivetAnims.BABY);
		}
	}



	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		poseStack.pushPose();

		if (this.young){
			poseStack.scale(0.2f, 0.2f, 0.2f);
			poseStack.translate(0, 6, 0);
		}else {
			poseStack.scale(0.4f, 0.4f, 0.4f);
			poseStack.translate(0, 2.25, 0);
		}

		if (this.climbing){
			poseStack.mulPose(Axis.XP.rotationDegrees(-90));
			poseStack.translate(0, -0.4f, 0.25f);
		}

		BandedPlamCivet.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);

		poseStack.popPose();
	}

	@Override
	public ModelPart root() {
		return BandedPlamCivet;
	}
}