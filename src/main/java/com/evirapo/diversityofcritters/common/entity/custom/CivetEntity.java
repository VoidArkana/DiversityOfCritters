package com.evirapo.diversityofcritters.common.entity.custom;

import com.evirapo.diversityofcritters.common.entity.DOCEntities;
import com.evirapo.diversityofcritters.common.entity.ai.AnimatedAttackGoal;
import com.evirapo.diversityofcritters.common.entity.ai.CritterDrinkGoal;
import com.evirapo.diversityofcritters.common.entity.ai.CustomFloatGoal;
import com.evirapo.diversityofcritters.common.entity.ai.LookForFoodItems;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.custom.base.IAnimatedAttacker;
import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CivetEntity extends DiverseCritter implements IAnimatedAttacker {

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState drinkingAnimationState = new AnimationState();
    private LookForFoodItems forFoodGoal;

    private static final EntityDataAccessor<Boolean> IS_ATTACKING = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.BOOLEAN);

    public int attackAnimationTimeout;
    public final AnimationState attackAnimationState = new AnimationState();

    public CivetEntity(EntityType<? extends Animal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(Items.BEEF) || pStack.is(Items.COOKED_BEEF);
    }

    protected void registerGoals() {
        this.forFoodGoal = new LookForFoodItems(this, DoCTags.Items.CIVET_FOOD);
        this.goalSelector.addGoal(3, this.forFoodGoal);
        this.goalSelector.addGoal(0, new CustomFloatGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new CritterDrinkGoal(this));
        this.goalSelector.addGoal(1, new AnimatedAttackGoal(this, 1.25D, true, 7, 3));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15D, Ingredient.of(Items.BEEF, Items.COOKED_BEEF), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.15D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, Rabbit.class, false, (living) -> this.isHungry()));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, Chicken.class, false, (living) -> this.isHungry()));
    }

    private void triggerFoodSearch() {
        if (this.forFoodGoal != null) {
            this.forFoodGoal.trigger();
        } else {
            this.navigation.stop();
            Predicate<ItemEntity> predicate = (p_25258_) -> p_25258_.getItem().is(DoCTags.Items.CIVET_FOOD);
            List<? extends ItemEntity> list = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate((double)32.0F, (double)8.0F, (double)32.0F), predicate);
            if (!list.isEmpty()) {
                this.navigation.moveTo((Entity)list.get(0), 1.1);
            }
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public boolean canAttack(LivingEntity pTarget) {
        return super.canAttack(pTarget) && ((this.getLastHurtByMob() != null && this.getLastHurtByMob() == pTarget) || this.isHungry());
    }

    //attributes
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, (double)0.2F)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_ATTACKING, false);
    }

    @Override
    public void jumpInFluid(FluidType type) {
        self().setDeltaMovement(self().getDeltaMovement().add(0.0D, (double)0.04F * self().getAttributeValue(ForgeMod.SWIM_SPEED.get())/3, 0.0D));
    }

    @Override
    public void sinkInFluid(FluidType type) {
        self().setDeltaMovement(self().getDeltaMovement().add(0.0D, (double)-0.04F * self().getAttributeValue(ForgeMod.SWIM_SPEED.get())/6, 0.0D));
    }

    @Override
    public void aiStep() {

        ItemStack stack = this.getItemBySlot(EquipmentSlot.MAINHAND);

        if (stack != null && stack.is(DoCTags.Items.CIVET_FOOD)) {
            this.setHunger(Math.min(this.getHunger()+this.maxHunger()/4, this.maxHunger()));
            this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), this.getX(), this.getY(), this.getZ(), (double)0.0F, (double)0.0F, (double)0.0F);
            this.level().playSound(null, new BlockPos((int) this.getX(), (int) this.getY(), (int) this.getZ()), SoundEvents.GENERIC_EAT, SoundSource.AMBIENT);
            stack.setCount(0);
            this.setItemInHand(InteractionHand.MAIN_HAND, Items.AIR.getDefaultInstance());
        }

        if (isHungry() && (double)this.random.nextFloat() <= 0.2) {
            this.triggerFoodSearch();
        }

        super.aiStep();
    }

    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack item = itemEntity.getItem();
        if (item.is(DoCTags.Items.CIVET_FOOD)) {
            ItemStack itemstack = itemEntity.getItem();
            ItemStack itemstack1 = this.equipItemIfPossible(itemstack.copy());
            if (!itemstack1.isEmpty()) {
                this.onItemPickup(itemEntity);
                this.take(itemEntity, 1);
                itemstack.shrink(1);
                if (itemstack.isEmpty()) {
                    itemEntity.discard();
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (IsDrinking()) {
            if (getDrinkPos() != null) {
                BlockPos washingPos = getDrinkPos();
                if (this.distanceToSqr(washingPos.getX() + 0.5D, washingPos.getY() + 0.5D, washingPos.getZ() + 0.5D) < 3) {
                    if (this.getRandom().nextInt(3)==0){
                        for (int j = 0; (float) j < 4; ++j) {
                            double d2 = (this.random.nextDouble()/2);
                            double d3 = (this.random.nextDouble()/2);
                            Vec3 vector3d = this.getDeltaMovement();

                            this.level().addParticle(ParticleTypes.SPLASH, washingPos.getX() + d2, (double) (washingPos.getY() + 0.8F), washingPos.getZ() + d3, vector3d.x, vector3d.y, vector3d.z);
                        }
                    }

                } else {
                    setIsDrinking(false);
                }
            }
        }

        if (this.isEffectiveAi()) {
            boolean flag = this.isInWaterOrBubble();
            if (flag || this.getTarget() != null || this.level().isThundering()) {
                this.wakeUp();
            }
        }
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }
    }

    public void customServerAiStep() {
        if (this.getMoveControl().hasWanted()) {
            double d0 = this.getMoveControl().getSpeedModifier();
            this.setPose(Pose.STANDING);
            this.setSprinting(d0 >= 1.25D);
        } else {
            this.setPose(Pose.STANDING);
            this.setSprinting(false);
        }

    }

    private void setupAnimationStates() {
        this.idleAnimationState.animateWhen(this.isAlive(), this.tickCount);
        this.drinkingAnimationState.animateWhen(this.isAlive() && this.IsDrinking(), this.tickCount);


        if(this.isAttacking() && attackAnimationTimeout <= 0) {
            attackAnimationTimeout = 10;
            attackAnimationState.start(this.tickCount);
        } else {
            --this.attackAnimationTimeout;
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        this.setIsMale(this.random.nextBoolean());
        if (pReason == MobSpawnType.COMMAND && pDataTag == null){
            if (this.getHunger() == 0 && this.getThirst() == 0){
                this.setHunger(this.maxHunger());
                this.setThirst(this.maxThirst());
            }
        }
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public boolean canMate(Animal pOtherAnimal) {
        CivetEntity otherLion = (CivetEntity) pOtherAnimal;
        return otherLion.getIsMale() != this.getIsMale() && super.canMate(pOtherAnimal);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        CivetEntity lion = DOCEntities.CIVET.get().create(pLevel);
        lion.setIsMale(this.random.nextBoolean());
        return lion;
    }

    @Override
    public int maxHunger() {
        //return 20*60*100;//1 second, 1 minute, 100 minutes, 1% per minute
        return 20*30;
    }

    @Override
    public int maxThirst() {
        return 20*60*100;
    }

    @Override
    public boolean isAttacking() {
        return this.entityData.get(IS_ATTACKING);
    }

    @Override
    public void setAttacking(boolean pFromBucket) {
        this.entityData.set(IS_ATTACKING, pFromBucket);
    }

    @Override
    public int attackAnimationTimeout() {
        return this.attackAnimationTimeout;
    }

    @Override
    public void setAttackAnimationTimeout(int attackAnimationTimeout) {
        this.attackAnimationTimeout = attackAnimationTimeout;
    }

    public static boolean checkCivetSpawnRules(EntityType<CivetEntity> pOcelot, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        return pRandom.nextInt(3) != 0;
    }

}
