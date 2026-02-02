package com.evirapo.diversityofcritters.common.entity.custom;

import com.evirapo.diversityofcritters.common.entity.DOCEntities;
import com.evirapo.diversityofcritters.common.entity.ai.*;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.util.CritterDietConfig;
import com.evirapo.diversityofcritters.common.item.DOCItems;
import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class CivetEntity extends DiverseCritter {

    // --- ANIMATION STATES ---
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState idleStandUpState   = new AnimationState();
    public final AnimationState idleSniffLeftState = new AnimationState();
    public final AnimationState idleSniffRightState= new AnimationState();
    public final AnimationState idleSitState       = new AnimationState();
    public final AnimationState idleLayState       = new AnimationState();
    public final AnimationState climbingUpState    = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState drinkingAnimationState = new AnimationState();
    public final AnimationState diggingAnimationState = new AnimationState();
    public final AnimationState cleanAnimationState = new AnimationState();

    // --- IDLE VARIANTS ENUM ---
    private enum IdleVariant { NONE, STAND_UP, SNIFF_LEFT, SNIFF_RIGHT, SIT, LAY }

    // --- DATA ---
    private static final EntityDataAccessor<Byte> IDLE_VARIANT = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> IDLE_LOCK_UNTIL = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_CLIMBING  = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_DIGGING = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TICKS_CLIMBING = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.INT);

    // --- VARIABLES ---
    private int idleVariantCooldown = 0;
    // attackAnimationTimeout movido a DiverseCritter
    public boolean isClimbableX;
    public boolean isClimbableZ;
    private LookForFoodItems forFoodGoal;
    private int tamingFeedsLeft = 0;

    // --- CONSTRUCTOR ---
    public CivetEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setMaxUpStep(1);
    }

    // --- SETUP ---
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // IS_ATTACKING movido a DiverseCritter
        this.entityData.define(IS_CLIMBING, false);
        this.entityData.define(IS_DIGGING, false);
        this.entityData.define(TICKS_CLIMBING, 0);
        this.entityData.define(IDLE_VARIANT, toByte(IdleVariant.NONE));
        this.entityData.define(IDLE_LOCK_UNTIL, -1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    protected PathNavigation createNavigation(Level pLevel) {
        return new WallClimberNavigation(this, pLevel);
    }

    // --- GOALS ---
    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(0, new CustomFloatGoal(this));
        this.goalSelector.addGoal(1, new SleepBehaviorGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));

        this.goalSelector.addGoal(4, new TemptGoal(this, 1.15D, Ingredient.of(Items.BEEF), false));

        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.2D, 8.0F, 2.0F, false) {
            @Override public boolean canUse() { return isFollowing() && super.canUse(); }
            @Override public boolean canContinueToUse() { return isFollowing() && super.canContinueToUse(); }
        });

        this.goalSelector.addGoal(6, new AnimatedAttackGoal(this, 1.25D, true, 7, 3));

        this.goalSelector.addGoal(7, new CivetCleanGoal(this));
        this.forFoodGoal = new LookForFoodItems(this, DoCTags.Items.MEATS);
        this.goalSelector.addGoal(8, this.forFoodGoal);

        this.goalSelector.addGoal(9, new FindFoodBowlGoal(this, 1.1D, 16));
        this.goalSelector.addGoal(10, new FindWaterBowlGoal(this, 1.1D, 16));
        this.goalSelector.addGoal(11, new CritterDrinkGoal(this));
        this.goalSelector.addGoal(12, new FindDigBoxGoal(this, 1.1D, 16));

        this.goalSelector.addGoal(13, new FollowParentGoal(this, 1.15D));
        this.goalSelector.addGoal(14, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override public boolean canUse() {
                boolean canWander = !isTame() || isWandering();
                return canWander && super.canUse();
            }
        });
        this.goalSelector.addGoal(15, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(16, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Rabbit.class, false, (living) -> this.isHungry()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Chicken.class, false, (living) -> this.isHungry()));
    }

    // --- IDLE VARIANTS LOGIC ---
    private static byte toByte(IdleVariant v){
        return switch (v) {
            case NONE -> 0; case STAND_UP -> 1; case SNIFF_LEFT -> 2;
            case SNIFF_RIGHT -> 3; case SIT -> 4; case LAY -> 5;
        };
    }
    private static IdleVariant fromByte(byte b){
        return switch (b) {
            case 1 -> IdleVariant.STAND_UP; case 2 -> IdleVariant.SNIFF_LEFT;
            case 3 -> IdleVariant.SNIFF_RIGHT; case 4 -> IdleVariant.SIT; case 5 -> IdleVariant.LAY;
            default -> IdleVariant.NONE;
        };
    }
    private void setIdleVariant(IdleVariant v){ this.entityData.set(IDLE_VARIANT, toByte(v)); }
    private IdleVariant getIdleVariant(){ return fromByte(this.entityData.get(IDLE_VARIANT)); }
    private void setIdleLockUntil(int tick){ this.entityData.set(IDLE_LOCK_UNTIL, tick); }
    private int  getIdleLockUntil(){ return this.entityData.get(IDLE_LOCK_UNTIL); }


    private void serverHandleIdleVariant() {
        boolean sleepingLike = this.isPreparingSleep() || this.isSleeping() || this.isAwakeing();
        boolean swimming     = this.isInWaterOrBubble();
        boolean climbing     = this.isClimbing();
        boolean onGround     = this.onGround();

        boolean hasDeltaMove = this.getDeltaMovement().horizontalDistanceSqr() > IDLE_MOVE_EPS;
        boolean hasWantedNav = !this.getNavigation().isDone();
        boolean moving       = hasDeltaMove || hasWantedNav;

        boolean doingAttack  = this.isAttacking();
        boolean drinking     = this.IsDrinking();
        boolean hasTarget    = this.getTarget() != null;

        boolean idleBase = this.isAlive()
                && !sleepingLike && !moving && !swimming && !climbing
                && !doingAttack && !drinking && !hasTarget && onGround
                && !this.isCleaning();

        IdleVariant current = getIdleVariant();

        if (current != IdleVariant.NONE && getIdleLockUntil() > 0 && this.tickCount >= getIdleLockUntil()) {
            setIdleVariant(IdleVariant.NONE);
            setIdleLockUntil(-1);
            idleVariantCooldown = 100;
        }

        if (idleVariantCooldown > 0) idleVariantCooldown--;

        if (!idleBase && current != IdleVariant.NONE) {
            setIdleVariant(IdleVariant.NONE);
            setIdleLockUntil(-1);
            idleVariantCooldown = 60;
            return;
        }

        if (idleBase && current == IdleVariant.NONE && idleVariantCooldown == 0) {
            if (this.random.nextFloat() < 0.01f) {
                startServerIdleVariant(pickVariant());
            }
        }

        if (getIdleVariant() != IdleVariant.NONE) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    private IdleVariant pickVariant(){
        int roll = this.random.nextInt(100);
        if      (roll < 20) return IdleVariant.STAND_UP;
        else if (roll < 45) return IdleVariant.SNIFF_LEFT;
        else if (roll < 70) return IdleVariant.SNIFF_RIGHT;
        else if (roll < 85) return IdleVariant.SIT;
        else                return IdleVariant.LAY;
    }

    private void startServerIdleVariant(IdleVariant v){
        setIdleVariant(v);
        int dur; int cd;
        switch (v) {
            case STAND_UP   -> { dur = 40;  cd = 60;  }
            case SNIFF_LEFT -> { dur = 40;  cd = 50;  }
            case SNIFF_RIGHT-> { dur = 40;  cd = 50;  }
            case SIT        -> { dur = 100; cd = 120; }
            case LAY        -> { dur = 140; cd = 140; }
            default         -> { dur = 0;   cd = 0;   }
        }
        setIdleLockUntil(dur > 0 ? this.tickCount + dur : -1);
        idleVariantCooldown = cd;
    }

    private static final double IDLE_MOVE_EPS = 0.0004D;

    // --- MAIN TICK ---
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            this.setClimbing(this.horizontalCollision && (this.isClimbableX || this.isClimbableZ));
            serverHandleIdleVariant();
        }

        if (IsDrinking() && getDrinkPos() != null) {
            BlockPos washingPos = getDrinkPos();
            if (this.distanceToSqr(washingPos.getX() + 0.5D, washingPos.getY() + 0.5D, washingPos.getZ() + 0.5D) < 3) {
                if (this.getRandom().nextInt(3)==0){
                    for (int j = 0; j < 4; ++j) {
                        double d2 = this.random.nextDouble()/2;
                        double d3 = this.random.nextDouble()/2;
                        Vec3 v = this.getDeltaMovement();
                        this.level().addParticle(ParticleTypes.SPLASH, washingPos.getX() + d2, washingPos.getY() + 0.8F, washingPos.getZ() + d3, v.x, v.y, v.z);
                    }
                }
            } else {
                setIsDrinking(false);
            }
        }

        if (this.level().isClientSide) {
            setupAnimationStatesClient();
        }
    }

    @Override
    public void aiStep() {
        ItemStack stack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!stack.isEmpty() && stack.is(DoCTags.Items.MEATS)) {
            int baseBowl = this.getDietConfig().hungerPerMeatBowl;
            int restore  = baseBowl + baseBowl / 4;
            this.setHunger(Math.min(this.getHunger() + restore, this.maxHunger()));

            this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.AMBIENT);

            stack.shrink(1);
            if (stack.isEmpty()) {
                this.setItemInHand(InteractionHand.MAIN_HAND, Items.AIR.getDefaultInstance());
            }
        }

        if (isHungry() && (double)this.random.nextFloat() <= 0.2) triggerFoodSearch();

        super.aiStep();

        Vec3 vec3 = this.getDeltaMovement();
        if (this.isClimbing() && (Math.abs(vec3.y) > 0.1D)) {
            if (!this.level().isClientSide && this.getTicksClimbing() < 3) {
                this.setTicksClimbing(this.getTicksClimbing()+1);
            }
        } else {
            if (!this.level().isClientSide && this.getTicksClimbing() > 0) {
                this.setTicksClimbing(this.getTicksClimbing()-1);
            }
        }
    }

    @Override
    public void customServerAiStep() {
        if (getIdleVariant() != IdleVariant.NONE) {
            this.getNavigation().stop();
            this.setSprinting(false);
            this.setPose(Pose.STANDING);
            this.setTarget(null);
            return;
        }
        if (this.getMoveControl().hasWanted()) {
            double d0 = this.getMoveControl().getSpeedModifier();
            this.setPose(Pose.STANDING);
            this.setSprinting(d0 >= 1.25D);
        } else {
            this.setPose(Pose.STANDING);
            this.setSprinting(false);
        }
    }

    // --- MOVEMENT & PHYSICS ---
    @Override
    public void move(MoverType pType, Vec3 pPos) {
        super.move(pType, pPos);
        if (!this.noPhysics){
            Vec3 vec3 = this.collide(pPos);
            boolean flagX = !Mth.equal(pPos.x, vec3.x);
            boolean flagZ = !Mth.equal(pPos.z, vec3.z);

            if (!this.navigation.isDone()){
                if (flagX){
                    BlockPos bp = new BlockPos(new Vec3i((int)(this.getX()+Math.max(-1, Math.min(1, pPos.x*100))), (int)this.getY(), (int)this.getZ()));
                    BlockState bs = this.level().getBlockState(bp);
                    this.isClimbableX = bs.is(DoCTags.Blocks.CIVET_CLIMBABLE);
                    this.setClimbing(this.isClimbableX);
                } else if (flagZ){
                    BlockPos bp = new BlockPos(new Vec3i((int)this.getX(), (int)this.getY(), (int)(this.getZ()+Math.max(-1, Math.min(1, pPos.z*100)))));
                    BlockState bs = this.level().getBlockState(bp);
                    this.isClimbableZ = bs.is(DoCTags.Blocks.CIVET_CLIMBABLE);
                    this.setClimbing(this.isClimbableZ);
                }
            }
        }
    }

    private Vec3 collide(Vec3 pVec) {
        AABB aabb = this.getBoundingBox();
        List<VoxelShape> list = this.level().getEntityCollisions(this, aabb.expandTowards(pVec));
        Vec3 vec3 = pVec.lengthSqr() == 0.0D ? pVec : collideBoundingBox(this, pVec, aabb, this.level(), list);
        boolean flag = pVec.x != vec3.x;
        boolean flag1 = pVec.y != vec3.y;
        boolean flag2 = pVec.z != vec3.z;
        boolean flag3 = this.onGround() || flag1 && pVec.y < 0.0D;
        float stepHeight = getStepHeight();
        if (stepHeight > 0.0F && flag3 && (flag || flag2)) {
            Vec3 vec31 = collideBoundingBox(this, new Vec3(pVec.x, stepHeight, pVec.z), aabb, this.level(), list);
            Vec3 vec32 = collideBoundingBox(this, new Vec3(0.0D, stepHeight, 0.0D), aabb.expandTowards(pVec.x, 0.0D, pVec.z), this.level(), list);
            if (vec32.y < (double)stepHeight) {
                Vec3 vec33 = collideBoundingBox(this, new Vec3(pVec.x, 0.0D, pVec.z), aabb.move(vec32), this.level(), list).add(vec32);
                if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr()) vec31 = vec33;
            }
            if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                return vec31.add(collideBoundingBox(this, new Vec3(0.0D, -vec31.y + pVec.y, 0.0D), aabb.move(vec31), this.level(), list));
            }
        }
        return vec3;
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    public void jumpInFluid(FluidType type) {
        self().setDeltaMovement(self().getDeltaMovement().add(0.0D, 0.04F * self().getAttributeValue(ForgeMod.SWIM_SPEED.get())/3, 0.0D));
    }

    @Override
    public void sinkInFluid(FluidType type) {
        self().setDeltaMovement(self().getDeltaMovement().add(0.0D, -0.04F * self().getAttributeValue(ForgeMod.SWIM_SPEED.get())/6, 0.0D));
    }

    // --- CLIENT VISUALS ---
    private void setupAnimationStatesClient() {
        if (this.isOrderedToSit()) {
            this.idleAnimationState.stop();
            this.idleStandUpState.stop();
            this.idleSniffLeftState.stop();
            this.idleSniffRightState.stop();
            this.idleSitState.stop();
            this.idleLayState.stop();
            this.cleanAnimationState.stop();
            this.diggingAnimationState.stop();
            this.climbingUpState.stop();
            this.attackAnimationState.stop();
            this.drinkingAnimationState.stop();
            return;
        }

        boolean sleepingLike = this.isPreparingSleep() || this.isSleeping() || this.isAwakeing();
        boolean swimming     = this.isInWaterOrBubble();
        boolean climbing     = this.isClimbing();
        boolean doingAttack  = this.isAttacking();
        boolean drinking     = this.IsDrinking();
        boolean hasTarget    = this.getTarget() != null;

        boolean softIdle = this.isAlive()
                && !sleepingLike && !swimming && !climbing
                && !doingAttack && !drinking && !hasTarget
                && !this.isDigging() && getIdleVariant() == IdleVariant.NONE
                && !this.isCleaning();

        this.idleAnimationState.animateWhen(softIdle, this.tickCount);

        IdleVariant v = getIdleVariant();
        this.idleStandUpState.animateWhen(   v == IdleVariant.STAND_UP,    this.tickCount);
        this.idleSniffLeftState.animateWhen( v == IdleVariant.SNIFF_LEFT,  this.tickCount);
        this.idleSniffRightState.animateWhen(v == IdleVariant.SNIFF_RIGHT, this.tickCount);
        this.idleSitState.animateWhen(       v == IdleVariant.SIT,         this.tickCount);
        this.idleLayState.animateWhen(       v == IdleVariant.LAY,         this.tickCount);

        this.cleanAnimationState.animateWhen(this.isCleaning(), this.tickCount);
        this.climbingUpState.animateWhen(this.isClimbingUp(), this.tickCount);
        this.drinkingAnimationState.animateWhen(this.isAlive() && this.IsDrinking(), this.tickCount);
        this.diggingAnimationState.animateWhen(this.isDigging(), this.tickCount);

        if (this.isAttacking() && this.attackAnimationTimeout <= 0) {
            this.attackAnimationTimeout = 10;
            attackAnimationState.start(this.tickCount);
        } else {
            --this.attackAnimationTimeout;
        }
    }

    // --- INTERACTION & EVENTS ---
    @Override
    public boolean hurt(DamageSource src, float amt) {
        boolean res = super.hurt(src, amt);
        if (!level().isClientSide && getIdleVariant() != IdleVariant.NONE) {
            setIdleVariant(IdleVariant.NONE);
            setIdleLockUntil(-1);
            idleVariantCooldown = 80;
        }
        return res;
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        // 1. DOMESTICACIÓN
        boolean isItemTamingMeat = itemstack.is(DoCTags.Items.MEATS) || itemstack.is(ItemTags.FISHES);
        if (isItemTamingMeat && !this.isTame()) {
            if (this.level().isClientSide()) {
                return InteractionResult.CONSUME;
            } else {
                if (!pPlayer.getAbilities().instabuild) itemstack.shrink(1);
                if (this.tamingFeedsLeft <= 0) this.tamingFeedsLeft = 3 + this.random.nextInt(3);
                this.tamingFeedsLeft--;
                if (this.tamingFeedsLeft <= 0 && !ForgeEventFactory.onAnimalTame(this, pPlayer)) {
                    this.tame(pPlayer);
                    this.navigation.recomputePath();
                    this.setTarget(null);
                    this.level().broadcastEntityEvent(this, (byte)7);
                    this.setWandering(true);
                    this.setOrderedToSit(false);
                    this.setInSittingPose(true);
                } else {
                    this.level().broadcastEntityEvent(this, (byte)6);
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 2. CURACIÓN
        if (this.isTame()) {
            if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                var foodProps = itemstack.isEdible() ? itemstack.getFoodProperties(this) : null;
                if (foodProps != null) this.heal((float) foodProps.getNutrition());
                if (!pPlayer.getAbilities().instabuild) itemstack.shrink(1);
                this.level().broadcastEntityEvent(this, (byte)7);
                this.gameEvent(GameEvent.EAT, this);
                return InteractionResult.SUCCESS;
            }
        }

        // 3. TRAINING STICK
        if (this.isOwnedBy(pPlayer) && itemstack.is(DOCItems.TRAINING_STICK.get())) {
            if (level().isClientSide) return InteractionResult.CONSUME;

            if (!this.isOrderedToSit() && !this.isWandering()) {
                this.setWandering(true);
                this.setOrderedToSit(false);
                this.setInSittingPose(false);
                this.messageState("wandering", pPlayer);
            }
            else {
                boolean willSit = !this.isOrderedToSit();

                if (willSit) {
                    setIdleVariant(IdleVariant.NONE);
                    setIdleLockUntil(-1);
                    this.setCleaning(false);
                    this.setAttacking(false);
                    this.getNavigation().stop();
                    this.setTarget(null);

                    setModeSit(true);

                    this.level().playSound(null, this.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.6f, 1.0f);
                } else {
                    this.setWandering(false);
                    this.setOrderedToSit(false);
                    this.setInSittingPose(false);
                }
                this.messageState(willSit ? "sit" : "following", pPlayer);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(pPlayer, pHand);
    }

    private void triggerFoodSearch() {
        if (this.forFoodGoal != null) {
            this.forFoodGoal.trigger();
        } else {
            this.navigation.stop();
            Predicate<ItemEntity> predicate = (e) -> e.getItem().is(DoCTags.Items.MEATS);
            List<? extends ItemEntity> list = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(32.0D, 8.0D, 32.0D), predicate);
            if (!list.isEmpty()) this.navigation.moveTo(list.get(0), 1.1);
        }
    }

    @Override
    public boolean canPickUpLoot() { return true; }

    @Override
    public boolean canAttack(LivingEntity pTarget) {
        return super.canAttack(pTarget) && ((this.getLastHurtByMob() != null && this.getLastHurtByMob() == pTarget) || this.isHungry());
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(Items.SWEET_BERRIES);
    }

    // --- HELPERS & CONFIG ---
    @Override public int maxHunger() { return 4000; }
    @Override public int maxThirst() { return 4000; }
    @Override public int maxEnrichment() { return 4000; }
    @Override public int maxHygiene() { return 4000; }

    // Sleep
    @Override public int getPreparingSleepDuration() {return 25;}
    @Override public int getAwakeningDuration() {return 24;}
    @Override protected boolean getDefaultDiurnal() { return true; }

    public boolean isClimbing() { return this.entityData.get(IS_CLIMBING); }
    public int getTicksClimbing() { return this.entityData.get(TICKS_CLIMBING); }
    public void setTicksClimbing(int v) { this.entityData.set(TICKS_CLIMBING, v); }
    public boolean isClimbingUp() { return this.isClimbing() && this.getDeltaMovement().y>0.1f; }
    public void setClimbing(boolean v) { this.entityData.set(IS_CLIMBING, v); }
    public boolean onClimbable() { return isClimbing(); }

    public boolean isDigging() {return this.entityData.get(IS_DIGGING);}
    public void setDigging(boolean isDigging) {this.entityData.set(IS_DIGGING, isDigging);}

    public static boolean checkCivetSpawnRules(EntityType<CivetEntity> t, LevelAccessor lvl, MobSpawnType type, BlockPos pos, RandomSource rnd) {
        return rnd.nextInt(3) != 0;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        CivetEntity civet = DOCEntities.CIVET.get().create(pLevel);
        civet.setIsMale(this.random.nextBoolean());
        return civet;
    }

    @Override
    public CritterDietConfig getDietConfig() {
        return new CritterDietConfig(true, true, true, 200, 200, 200, 300);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TamingFeedsLeft", this.tamingFeedsLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.tamingFeedsLeft = tag.getInt("TamingFeedsLeft");
    }

    public boolean isIdleLocked() {
        return getIdleVariant() != IdleVariant.NONE;
    }
}