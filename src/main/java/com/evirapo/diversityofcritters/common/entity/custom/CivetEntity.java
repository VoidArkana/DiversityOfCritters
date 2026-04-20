package com.evirapo.diversityofcritters.common.entity.custom;

import com.evirapo.diversityofcritters.common.entity.DOCEntities;
import com.evirapo.diversityofcritters.common.entity.ai.*;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.common.entity.util.CritterDietConfig;
import com.evirapo.diversityofcritters.common.item.DOCItems;
import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class CivetEntity extends DiverseCritter {

    // --- ANIMATION STATES ---
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState idleStandUpState   = new AnimationState();
    public final AnimationState idleSniffLeftState = new AnimationState();
    public final AnimationState idleSniffRightState= new AnimationState();
    public final AnimationState idleSitStartingState = new AnimationState();
    public final AnimationState idleSitState         = new AnimationState();
    public final AnimationState idleSitEndingState   = new AnimationState();
    public final AnimationState idleLayStartingState = new AnimationState();
    public final AnimationState idleLayState         = new AnimationState();
    public final AnimationState idleLayEndingState   = new AnimationState();
    public final AnimationState climbIdleState      = new AnimationState();
    public final AnimationState climbingUpState     = new AnimationState();
    public final AnimationState climbingDownState   = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState drinkStartingState = new AnimationState();
    public final AnimationState drinkIdleState     = new AnimationState();
    public final AnimationState drinkEndingState   = new AnimationState();
    public final AnimationState digStartingState = new AnimationState();
    public final AnimationState digIdleState     = new AnimationState();
    public final AnimationState digEndingState   = new AnimationState();
    public final AnimationState cleanStartingState = new AnimationState();
    public final AnimationState cleanIdleState     = new AnimationState();
    public final AnimationState cleanEndingState   = new AnimationState();
    public final AnimationState preparingCryState = new AnimationState();
    public final AnimationState cryingState = new AnimationState();
    public final AnimationState stoppingCryState = new AnimationState();
    public final AnimationState scratchStartingState = new AnimationState();
    public final AnimationState scratchIdleState     = new AnimationState();
    public final AnimationState scratchEndingState   = new AnimationState();
    public final AnimationState swimAnimationState   = new AnimationState();

    // --- IDLE VARIANTS ENUM ---
    public enum IdleVariant { NONE, STAND_UP, SNIFF_LEFT, SNIFF_RIGHT, SIT, LAY }

    // --- DATA ---
    private static final EntityDataAccessor<Byte> IDLE_VARIANT = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.BYTE);

    // Climb state: 0=NONE, 1=UP, 2=DOWN
    private static final EntityDataAccessor<Byte> CLIMB_STATE = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> CLIMB_FACING_YAW = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_DIGGING = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SCRATCHING = SynchedEntityData.defineId(CivetEntity.class, EntityDataSerializers.BOOLEAN);

    public static final byte CLIMB_NONE = 0;
    public static final byte CLIMB_UP   = 1;
    public static final byte CLIMB_DOWN = 2;
    public static final byte CLIMB_HANG = 3;

    // --- VARIABLES ---
    public int idleVariantCooldown = 0;
    private LookForFoodItems forFoodGoal;
    private int tamingFeedsLeft = 0;
    private int cryTimer = 0;
    private int stopCryTimer = 0;
    private boolean wasCryingClient = false;

    private IdleVariant prevClientVariant = IdleVariant.NONE;
    private int clientIdleTick = 0;

    private boolean prevScratchingClient = false;
    private int clientScratchTick = 0;

    private boolean prevCleaningClient = false;
    private int clientCleanTick = 0;

    private boolean prevDrinkingClient = false;
    private int clientDrinkTick = 0;
    private int clientDrinkEndingTick = 0;

    private boolean prevDiggingClient = false;
    private int clientDigTick = 0;
    private int clientDigEndingTick = 0;

    private boolean climbRewardArmed = false;
    private int climbRewardCooldown = 0;

    /** Progreso de sprint (0 = walk, 1 = run), interpolado en el cliente. */
    public float sprintProgress = 0.0F;

    /** True mientras una variante idle (STAND_UP, SNIFF, SIT, LAY) esté activa.
     *  Expuesto al modelo para suprimir el blend de idle base. */
    public boolean isVariantActive = false;

    private int postDescentCooldown = 0;
    private static final int POST_DESCENT_COOLDOWN = 20;
    private boolean cachedHasClimbable = false;

    public boolean isBeingCleaned = false;

    public CivetEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setMaxUpStep(1);
        this.moveControl = new CivetMoveControl(this);
        this.idleAnimationState.start(0);
    }

    // --- SETUP ---
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CLIMB_STATE, CLIMB_NONE);
        this.entityData.define(CLIMB_FACING_YAW, 0.0F);
        this.entityData.define(IS_DIGGING, false);
        this.entityData.define(IDLE_VARIANT, toByte(IdleVariant.NONE));
        this.entityData.define(IS_SCRATCHING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    protected PathNavigation createNavigation(Level pLevel) {
        return new CivetNavigation(this, pLevel);
    }

    // --- GOALS ---
    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(0, new CustomFloatGoal(this));
        this.goalSelector.addGoal(1, new SleepBehaviorGoal(this));

        this.goalSelector.addGoal(2, new BabyCryGoal(this));
        this.goalSelector.addGoal(2, new FindCryingBabyGoal(this, 1.4D, 24.0D));
        this.goalSelector.addGoal(3, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(4, new NaturalMateGoal(this, 1.0D));

        this.goalSelector.addGoal(5, new TemptGoal(this, 1.15D, Ingredient.of(Items.BEEF), false));

        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.2D, 8.0F, 2.0F, false) {
            @Override public boolean canUse() { return isFollowing() && super.canUse(); }
            @Override public boolean canContinueToUse() { return isFollowing() && super.canContinueToUse(); }
        });

        this.goalSelector.addGoal(7, new AnimatedAttackGoal(this, 1.25D, true, 7, 3));

        this.goalSelector.addGoal(8, new CivetCleanGoal(this));
        this.goalSelector.addGoal(9, new MotherCleanBabyGoal(this, 1.2D, 16.0D));
        this.forFoodGoal = new LookForFoodItems(this, DoCTags.Items.MEATS);
        this.goalSelector.addGoal(10, this.forFoodGoal);

        this.goalSelector.addGoal(11, new FindFoodBowlGoal(this, 1.1D, 16));
        this.goalSelector.addGoal(12, new FindWaterBowlGoal(this, 1.1D, 16));
        this.goalSelector.addGoal(13, new CritterDrinkGoal(this));
        this.goalSelector.addGoal(14, new FindDigBoxGoal(this, 1.1D, 16));
        this.goalSelector.addGoal(15, new ScratchLogGoal(this, 1.1D, 16));
        this.goalSelector.addGoal(16, new DigDirtGoal(this));
        this.goalSelector.addGoal(16, new CivetClimbGoal(this, 1.1D, 16));

        this.goalSelector.addGoal(17, new CivetIdleGoal(this));
        this.goalSelector.addGoal(18, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(19, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override public boolean canUse() {
                boolean canWander = !isTame() || isWandering();
                return canWander && super.canUse();
            }
        });
        this.goalSelector.addGoal(20, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(21, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Rabbit.class, false, (living) -> this.isHungry()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Chicken.class, false, (living) -> this.isHungry()));
    }

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
    public void setIdleVariant(IdleVariant v){ this.entityData.set(IDLE_VARIANT, toByte(v)); }
    public IdleVariant getIdleVariant(){ return fromByte(this.entityData.get(IDLE_VARIANT)); }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            this.updateClimbState();

            boolean isNewborn = this.isNewborn();
            double desiredSpeed = isNewborn ? 0.095D : 0.2D;
            float desiredStep   = isNewborn ? 0.5F : 1.0F;

            if (this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() != desiredSpeed) {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(desiredSpeed);
            }
            if (this.maxUpStep() != desiredStep) {
                this.setMaxUpStep(desiredStep);
            }

            if (this.idleVariantCooldown > 0) this.idleVariantCooldown--;

            if (this.isNewborn() && this.getHunger() < 3000) {
                if (!this.isCrying()) {
                    this.setCrying(true);
                    this.playSound(SoundEvents.FOX_SCREECH, 0.5F, 2.0F);
                }

                if (this.tickCount % 80 == 0) {
                    this.playSound(SoundEvents.FOX_SCREECH, 0.5F, 2.0F);
                }
            } else {
                if (this.isCrying()) {
                    this.setCrying(false);
                }
            }
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
            IdleVariant currentVariant = getIdleVariant();

            if (currentVariant != prevClientVariant) {
                this.clientIdleTick = 0;
                this.prevClientVariant = currentVariant;
            }

            if (currentVariant != IdleVariant.NONE) {
                this.clientIdleTick++;
            }

            boolean currentScratch = isScratching();
            if (currentScratch != prevScratchingClient) {
                this.clientScratchTick = 0;
                this.prevScratchingClient = currentScratch;
            }
            if (currentScratch) {
                this.clientScratchTick++;
            }

            boolean currentCleaning = isCleaning();
            if (currentCleaning != prevCleaningClient) {
                this.clientCleanTick = 0;
                this.prevCleaningClient = currentCleaning;
            }
            if (currentCleaning) {
                this.clientCleanTick++;
            }

            boolean currentDrinking = IsDrinking();

            if (currentDrinking && !prevDrinkingClient) {
                this.clientDrinkTick = 0;
                this.clientDrinkEndingTick = 0;
            } else if (!currentDrinking && prevDrinkingClient) {
                this.clientDrinkEndingTick = 1;
                this.clientDrinkTick = 0;
            }

            if (currentDrinking) {
                this.clientDrinkTick++;
            } else if (this.clientDrinkEndingTick > 0) {
                this.clientDrinkEndingTick++;
                if (this.clientDrinkEndingTick > 6) {
                    this.clientDrinkEndingTick = 0;
                }
            }
            this.prevDrinkingClient = currentDrinking;

            boolean currentDigging = isDigging();

            if (currentDigging && !prevDiggingClient) {
                this.clientDigTick = 0;
                this.clientDigEndingTick = 0;
            } else if (!currentDigging && prevDiggingClient) {
                this.clientDigEndingTick = 1;
                this.clientDigTick = 0;
            }

            if (currentDigging) {
                this.clientDigTick++;
            } else if (this.clientDigEndingTick > 0) {
                this.clientDigEndingTick++;
                if (this.clientDigEndingTick > 11) {
                    this.clientDigEndingTick = 0;
                }
            }
            this.prevDiggingClient = currentDigging;

            // Sprint progress: lerp para suavizar la transición walk→run en el modelo
            if (this.isSprinting()) {
                this.sprintProgress = Math.min(1.0F, this.sprintProgress + 0.1F);
            } else {
                this.sprintProgress = Math.max(0.0F, this.sprintProgress - 0.1F);
            }

            setupAnimationStatesClient();
            handleCryingAnimationClient();
        }
    }

    @Override
    public void aiStep() {
        ItemStack stack = this.getItemBySlot(EquipmentSlot.MAINHAND);

        if (!stack.isEmpty() && stack.is(DoCTags.Items.MEATS) && !this.isNewborn()) {
            this.setHunger(this.maxHunger());

            var foodProps = stack.isEdible() ? stack.getFoodProperties(this) : null;
            if (foodProps != null) {
                this.heal((float) foodProps.getNutrition());
            } else {
                this.heal(1.0F);
            }
            // ---------------------------------------------

            this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.AMBIENT);

            stack.shrink(1);
            if (stack.isEmpty()) {
                this.setItemInHand(InteractionHand.MAIN_HAND, Items.AIR.getDefaultInstance());
            }
        }

        if (isHungry() && !this.isNewborn() && (double)this.random.nextFloat() <= 0.2) triggerFoodSearch();

        super.aiStep();

        if (!this.level().isClientSide && this.climbRewardCooldown > 0) {
            this.climbRewardCooldown--;
        }

        if (!this.level().isClientSide) {
            if (this.isClimbing()) {
                this.climbRewardArmed = true;
            } else if (this.climbRewardArmed && this.onGround() && this.climbRewardCooldown <= 0) {
                this.setEnrichment(this.maxEnrichment());
                this.climbRewardArmed = false;
                this.climbRewardCooldown = 20;
            }
        }

    }

    @Override
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

    // --- MOVEMENT & PHYSICS ---

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    /**
     * Override fall distance so the WalkNodeEvaluator does not mark positions
     * next to climbable walls as BLOCKED due to fall-distance checks.
     * Without this, any wall node more than 3 blocks high gets rejected by
     * the pathfinder before CivetNodeEvaluator can even evaluate it.
     */
    @Override
    public int getMaxFallDistance() {
        // Use cached value when available (server tick), otherwise compute directly.
        return this.cachedHasClimbable ? 16 : super.getMaxFallDistance();
    }

    @Override
    public void jumpInFluid(FluidType type) {
        self().setDeltaMovement(self().getDeltaMovement().add(0.0D, 0.04F * self().getAttributeValue(ForgeMod.SWIM_SPEED.get())/3, 0.0D));
    }

    @Override
    public void sinkInFluid(FluidType type) {
        self().setDeltaMovement(self().getDeltaMovement().add(0.0D, -0.04F * self().getAttributeValue(ForgeMod.SWIM_SPEED.get())/6, 0.0D));
    }

    private void setupAnimationStatesClient() {
        boolean sleepingLike = this.isPreparingSleep() || this.isSleeping() || this.isAwakeing();
        boolean swimming     = this.isInWaterOrBubble();
        boolean climbing     = this.isClimbing();

        IdleVariant v = getIdleVariant();
        int ticksActive = this.clientIdleTick;

        int sitStarting = 10;
        int sitIdle     = 80;
        int sitEnding   = 20;
        int sitTotal    = sitStarting + sitIdle + sitEnding;

        int layStarting = 20;
        int layIdle     = 80;
        int layEnding   = 20;
        int layTotal    = layStarting + layIdle + layEnding;

        boolean isOrderedSitPlaying = this.isOrderedSitPlaying() && !sleepingLike;

        boolean isVariantPlaying = false;
        if (!isOrderedSitPlaying) {
            if (v == IdleVariant.STAND_UP && ticksActive <= 40) isVariantPlaying = true;
            else if (v == IdleVariant.SNIFF_LEFT && ticksActive <= 40) isVariantPlaying = true;
            else if (v == IdleVariant.SNIFF_RIGHT && ticksActive <= 40) isVariantPlaying = true;
            else if (v == IdleVariant.SIT && ticksActive <= sitTotal) isVariantPlaying = true;
            else if (v == IdleVariant.LAY && ticksActive <= layTotal) isVariantPlaying = true;
        }

        boolean scratching = isScratching();
        int scratchActive = this.clientScratchTick;
        int scratchStart = 10;
        int scratchLoop  = 80;
        int scratchEnd   = 10;
        int scratchTotal = scratchStart + scratchLoop + scratchEnd;

        boolean isScratchPlaying = scratching && scratchActive <= scratchTotal && !isOrderedSitPlaying;

        boolean cleaning = isCleaning();
        int cleanActive = this.clientCleanTick;
        int cleanStart = 10;
        int cleanLoop  = 60;
        int cleanEnd   = 10;
        int cleanTotal = cleanStart + cleanLoop + cleanEnd;

        boolean isCleanPlaying = cleaning && cleanActive <= cleanTotal && !isOrderedSitPlaying;

        boolean drinking = IsDrinking();
        boolean isDrinkStarting = drinking && this.clientDrinkTick <= 5;
        boolean isDrinkIdle     = drinking && this.clientDrinkTick > 5;
        boolean isDrinkEnding   = !drinking && this.clientDrinkEndingTick > 0;

        boolean isDrinkPlaying  = (isDrinkStarting || isDrinkIdle || isDrinkEnding) && !isOrderedSitPlaying;

        boolean digging = isDigging();
        boolean isDigStarting = digging && this.clientDigTick <= 10;
        boolean isDigIdle     = digging && this.clientDigTick > 10;
        boolean isDigEnding   = !digging && this.clientDigEndingTick > 0;

        boolean isDigPlaying  = (isDigStarting || isDigIdle || isDigEnding) && !isOrderedSitPlaying;

        // El idle se suprime mientras haya una variante activa (igual que isOrderedSitPlaying).
        // El PESO en el modelo maneja la visibilidad durante locomoción normal.
        boolean groundLocomotion = this.isAlive()
                && !sleepingLike && !swimming && !climbing
                && !isOrderedSitPlaying && !isVariantPlaying;

        this.isVariantActive = isVariantPlaying;
        this.idleAnimationState.animateWhen(groundLocomotion, this.tickCount);
        this.swimAnimationState.animateWhen(swimming && this.isAlive(), this.tickCount);

        this.idleStandUpState.animateWhen(v == IdleVariant.STAND_UP && isVariantPlaying, this.tickCount);
        this.idleSniffLeftState.animateWhen(v == IdleVariant.SNIFF_LEFT && isVariantPlaying, this.tickCount);
        this.idleSniffRightState.animateWhen(v == IdleVariant.SNIFF_RIGHT && isVariantPlaying, this.tickCount);

        if (v == IdleVariant.SIT && isVariantPlaying) {
            this.idleSitStartingState.animateWhen(ticksActive <= sitStarting, this.tickCount);
            this.idleSitState.animateWhen(ticksActive > sitStarting && ticksActive <= sitStarting + sitIdle, this.tickCount);
            this.idleSitEndingState.animateWhen(ticksActive > sitStarting + sitIdle, this.tickCount);
        } else {
            this.idleSitStartingState.stop();
            this.idleSitState.stop();
            this.idleSitEndingState.stop();
        }

        if (v == IdleVariant.LAY && isVariantPlaying) {
            this.idleLayStartingState.animateWhen(ticksActive <= layStarting, this.tickCount);
            this.idleLayState.animateWhen(ticksActive > layStarting && ticksActive <= layStarting + layIdle, this.tickCount);
            this.idleLayEndingState.animateWhen(ticksActive > layStarting + layIdle, this.tickCount);
        } else {
            this.idleLayStartingState.stop();
            this.idleLayState.stop();
            this.idleLayEndingState.stop();
        }

        if (isScratchPlaying) {
            this.scratchStartingState.animateWhen(scratchActive <= scratchStart, this.tickCount);
            this.scratchIdleState.animateWhen(scratchActive > scratchStart && scratchActive <= scratchStart + scratchLoop, this.tickCount);
            this.scratchEndingState.animateWhen(scratchActive > scratchStart + scratchLoop, this.tickCount);
        } else {
            this.scratchStartingState.stop();
            this.scratchIdleState.stop();
            this.scratchEndingState.stop();
        }

        if (isCleanPlaying) {
            this.cleanStartingState.animateWhen(cleanActive <= cleanStart, this.tickCount);
            this.cleanIdleState.animateWhen(cleanActive > cleanStart && cleanActive <= cleanStart + cleanLoop, this.tickCount);
            this.cleanEndingState.animateWhen(cleanActive > cleanStart + cleanLoop, this.tickCount);
        } else {
            this.cleanStartingState.stop();
            this.cleanIdleState.stop();
            this.cleanEndingState.stop();
        }

        if (isDrinkPlaying) {
            this.drinkStartingState.animateWhen(isDrinkStarting, this.tickCount);
            this.drinkIdleState.animateWhen(isDrinkIdle, this.tickCount);
            this.drinkEndingState.animateWhen(isDrinkEnding, this.tickCount);
        } else {
            this.drinkStartingState.stop();
            this.drinkIdleState.stop();
            this.drinkEndingState.stop();
        }

        if (isDigPlaying) {
            this.digStartingState.animateWhen(isDigStarting, this.tickCount);
            this.digIdleState.animateWhen(isDigIdle, this.tickCount);
            this.digEndingState.animateWhen(isDigEnding, this.tickCount);
        } else {
            this.digStartingState.stop();
            this.digIdleState.stop();
            this.digEndingState.stop();
        }

        boolean climbUp   = this.isClimbingUp();
        boolean climbDown = this.isClimbingDown();
        boolean climbHang = this.isHanging();
        this.climbingUpState.animateWhen(climbUp, this.tickCount);
        this.climbingDownState.animateWhen(climbDown, this.tickCount);
        this.climbIdleState.animateWhen(climbHang, this.tickCount);

        if (this.isAttacking() && this.attackAnimationTimeout <= 0) {
            this.attackAnimationTimeout = 10;
            attackAnimationState.start(this.tickCount);
        } else {
            --this.attackAnimationTimeout;
        }
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        boolean isItemTamingMeat = itemstack.is(DoCTags.Items.MEATS) || itemstack.is(ItemTags.FISHES);
        boolean isNurserBottle = itemstack.is(DOCItems.FILLED_NURSER_BOTTLE.get());

        boolean canTameWithThis = false;
        if (!this.isTame()) {
            if (this.isBaby() && !this.isJuvenile()) {
                canTameWithThis = isNurserBottle;
            } else {
                canTameWithThis = isItemTamingMeat;
            }
        }

        if (canTameWithThis) {
            if (this.level().isClientSide()) {
                return InteractionResult.CONSUME;
            } else {
                if (!pPlayer.getAbilities().instabuild) {
                    if (isNurserBottle) {
                        itemstack.hurtAndBreak(1, pPlayer, (p) -> p.broadcastBreakEvent(pHand));
                    } else {
                        itemstack.shrink(1);
                    }
                }
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

        boolean isMeatFood = itemstack.is(DoCTags.Items.MEATS) || itemstack.is(ItemTags.FISHES);
        if (isMeatFood && !this.isBaby()) {
            if (this.getHunger() < this.maxHunger() || this.getHealth() < this.getMaxHealth()) {
                if (!this.level().isClientSide()) {
                    this.setHunger(this.maxHunger());

                    var foodProps = itemstack.isEdible() ? itemstack.getFoodProperties(this) : null;
                    if (foodProps != null) this.heal((float) foodProps.getNutrition());

                    if (!pPlayer.getAbilities().instabuild) itemstack.shrink(1);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                    this.gameEvent(GameEvent.EAT, this);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(pPlayer, pHand);
    }

    private void triggerFoodSearch() {
        if (this.isNewborn()) return;
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
    public boolean canPickUpLoot() {
        return !this.isNewborn();
    }

    @Override
    public boolean wantsToPickUp(ItemStack pStack) {
        return pStack.is(DoCTags.Items.MEATS);
    }

    @Override
    public boolean canAttack(LivingEntity pTarget) {
        return super.canAttack(pTarget) && ((this.getLastHurtByMob() != null && this.getLastHurtByMob() == pTarget) || this.isHungry());
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return false;
    }

    // --- HELPERS & CONFIG ---
    @Override public int maxHunger() { return 4000; }
    @Override public int maxThirst() { return 4000; }
    @Override public int maxEnrichment() { return 4000; }
    @Override public int maxHygiene() { return 4000; }
    @Override public int getPreparingSleepDuration() {return this.isNewborn() ? 20 : 25;}
    @Override public int getAwakeningDuration() {return this.isNewborn() ? 20 : 24;}
    @Override protected boolean getDefaultDiurnal() { return true; }

    // --- CLIMB STATE ---
    public byte getClimbState() { return this.entityData.get(CLIMB_STATE); }
    public void setClimbState(byte state) {
        byte prev = this.getClimbState();
        this.entityData.set(CLIMB_STATE, state);
        // Arm cooldown when landing from descent.
        // Do NOT reset when activating CLIMB_UP — the cooldown must survive
        // the activation cycle to break the post-descent re-climb loop.
        if (prev == CLIMB_DOWN && state == CLIMB_NONE) {
            this.postDescentCooldown = POST_DESCENT_COOLDOWN;
        }
    }

    /** Read-only access for CivetMoveControl to gate climb activation. */
    public boolean isInPostDescentCooldown() {
        return this.postDescentCooldown > 0;
    }
    public boolean isClimbing() { return getClimbState() != CLIMB_NONE; }
    public boolean isClimbingUp() { return getClimbState() == CLIMB_UP; }
    public boolean isClimbingDown() { return getClimbState() == CLIMB_DOWN; }
    public boolean isHanging() { return getClimbState() == CLIMB_HANG; }

    @Override
    public boolean onClimbable() { return isClimbing(); }

    public boolean hasAdjacentClimbableBlock() {
        return hasAdjacentClimbableBlockInternal();
    }

    private boolean hasAdjacentClimbableBlockInternal() {
        AABB box = this.getBoundingBox();
        double probeDistance = 0.3D;
        int footY = Mth.floor(box.minY);
        int eyeY  = Mth.floor(box.minY + this.getBbHeight() * 0.5);

        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        double halfW = (box.maxX - box.minX) * 0.5;
        double halfD = (box.maxZ - box.minZ) * 0.5;

        int[][] offsets = {
            { Mth.floor(cx + halfW + probeDistance), Mth.floor(cz) },   // +X
            { Mth.floor(cx - halfW - probeDistance), Mth.floor(cz) },   // -X
            { Mth.floor(cx), Mth.floor(cz + halfD + probeDistance) },   // +Z
            { Mth.floor(cx), Mth.floor(cz - halfD - probeDistance) }    // -Z
        };

        for (int[] off : offsets) {
            // Check at foot level, eye level, and one below foot
            // (needed when the mob is in the step-off position beside a column
            // whose climbable blocks are one block below the mob's feet)
            for (int dy : new int[]{0, -1}) {
                BlockPos posFoot = new BlockPos(off[0], footY + dy, off[1]);
                BlockPos posEye  = new BlockPos(off[0], eyeY  + dy, off[1]);
                if (this.level().getBlockState(posFoot).is(DoCTags.Blocks.CIVET_CLIMBABLE)
                 || this.level().getBlockState(posEye).is(DoCTags.Blocks.CIVET_CLIMBABLE)) {
                    return true;
                }
            }
        }
        return false;
    }

    public float getClimbFacingYaw() { return this.entityData.get(CLIMB_FACING_YAW); }

    private void updateClimbFacingYaw() {
        AABB box = this.getBoundingBox();
        double probeDistance = 0.3D;
        int footY = Mth.floor(box.minY);

        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        double halfW = (box.maxX - box.minX) * 0.5;
        double halfD = (box.maxZ - box.minZ) * 0.5;

        double[][] probes = {
            { cx + halfW + probeDistance, cz,                           -90.0 },
            { cx - halfW - probeDistance, cz,                            90.0 },
            { cx,                         cz + halfD + probeDistance,     0.0 },
            { cx,                         cz - halfD - probeDistance,   180.0 }
        };

        for (double[] probe : probes) {
            BlockPos pos = new BlockPos(Mth.floor(probe[0]), footY, Mth.floor(probe[1]));
            if (this.level().getBlockState(pos).is(DoCTags.Blocks.CIVET_CLIMBABLE)) {
                this.entityData.set(CLIMB_FACING_YAW, (float) probe[2]);
                return;
            }
        }
    }

    private void updateClimbState() {
        if (this.postDescentCooldown > 0) this.postDescentCooldown--;

        // Compute once and cache for the entire tick (reused by getMaxFallDistance
        // and exposed to MoveControl via hasAdjacentClimbableBlock).
        this.cachedHasClimbable = this.hasAdjacentClimbableBlockInternal();
        boolean hasClimbable = this.cachedHasClimbable;
        byte currentState = this.getClimbState();

        // --- HANG ---
        if (currentState == CLIMB_HANG) {
            if (!hasClimbable) {
                this.entityData.set(CLIMB_STATE, CLIMB_NONE);
            } else {
                this.updateClimbFacingYaw();
                this.lockClimbRotation();
            }
            return;
        }

        // --- CLIMB_UP ---
        if (currentState == CLIMB_UP) {
            // Blocked by ceiling: verticalCollision=true AND onGround=false = techo sólido arriba.
            if (this.verticalCollision && !this.onGround()) {
                // No puede subir más — cancelar climbing, caer, replanificar ruta.
                this.entityData.set(CLIMB_STATE, CLIMB_NONE);
                this.navigation.recomputePath();
                return;
            }
            if (!hasClimbable && !this.onGround()) {
                float yawRad = this.getYRot() * ((float) Math.PI / 180F);
                this.setDeltaMovement(-Math.sin(yawRad) * 0.25D, 0.4D, Math.cos(yawRad) * 0.25D);
                this.entityData.set(CLIMB_STATE, CLIMB_NONE);
            } else if (this.onGround() && !hasClimbable) {
                this.entityData.set(CLIMB_STATE, CLIMB_NONE);
            } else {
                this.updateClimbFacingYaw();
                this.lockClimbRotation();
            }
            return;
        }

        // --- CLIMB_DOWN ---
        if (currentState == CLIMB_DOWN) {
            if (this.onGround()) {
                // Landed — setClimbState will arm postDescentCooldown
                this.setClimbState(CLIMB_NONE);
                return;
            }
            this.updateClimbFacingYaw();
            this.lockClimbRotation();
            return;
        }

        // --- CLIMB_NONE fallback ---
        // Only activate CLIMB_UP when horizontally blocked against a climbable wall
        // AND the active path has an ascending node — prevents horizontal goals
        // (TemptGoal, FindFoodBowlGoal, etc.) from accidentally triggering climbing.
        if (this.postDescentCooldown > 0) return;
        if (!this.horizontalCollision || !hasClimbable) return;
        if (this.isScratching() || this.isNewborn() || this.onGround()) return;
        if (this.isInWater() || this.isInWaterOrBubble()) return;  // nunca escalar al salir del agua

        boolean pathGoesUp = false;
        net.minecraft.world.level.pathfinder.Path path = this.navigation.getPath();
        if (path != null && !path.isDone()) {
            int idx = path.getNextNodeIndex();
            int curY = Mth.floor(this.getY());
            int limit = Math.min(idx + 3, path.getNodeCount());
            for (int i = idx; i < limit; i++) {
                if (path.getNode(i).y > curY) { pathGoesUp = true; break; }
            }
        }
        if (!pathGoesUp) return;

        this.setClimbState(CLIMB_UP);
        this.updateClimbFacingYaw();
        this.lockClimbRotation();
    }

    private void lockClimbRotation() {
        float wallYaw = this.getClimbFacingYaw();
        this.setYRot(wallYaw);
        this.yRotO = wallYaw;
        this.setYBodyRot(wallYaw);
        this.yBodyRotO = wallYaw;
        this.setYHeadRot(wallYaw);
    }

    public boolean isDigging() {return this.entityData.get(IS_DIGGING);}
    public void setDigging(boolean isDigging) {this.entityData.set(IS_DIGGING, isDigging);}

    public boolean isScratching() { return this.entityData.get(IS_SCRATCHING); }
    public void setScratching(boolean isScratching) { this.entityData.set(IS_SCRATCHING, isScratching); }

    public static boolean checkCivetSpawnRules(EntityType<CivetEntity> t, LevelAccessor lvl, MobSpawnType type, BlockPos pos, RandomSource rnd) {
        return true;
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

    private void handleCryingAnimationClient() {
        boolean isCrying = this.isCrying();

        if (isCrying) {
            this.stoppingCryState.stop();
            this.stopCryTimer = 0;
            this.wasCryingClient = true;

            if (this.cryTimer < 10) {
                this.preparingCryState.startIfStopped(this.tickCount);
                this.cryingState.stop();
            } else {
                this.preparingCryState.stop();
                this.cryingState.startIfStopped(this.tickCount);
            }
            this.cryTimer++;

        } else {
            this.preparingCryState.stop();
            this.cryingState.stop();
            this.cryTimer = 0;

            if (this.wasCryingClient) {
                if (this.stopCryTimer < 10) {
                    this.stoppingCryState.startIfStopped(this.tickCount);
                } else {
                    this.stoppingCryState.stop();
                    this.wasCryingClient = false;
                }
                this.stopCryTimer++;
            } else {
                this.stoppingCryState.stop();
                this.stopCryTimer = 0;
            }
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isSleeping()) {
            return SoundEvents.CAT_PURR;
        }
        if (this.isTame()) {
            return SoundEvents.CAT_AMBIENT;
        }
        return SoundEvents.FOX_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.FOX_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public SoundEvent getEatingSound(ItemStack pStack) {
        return SoundEvents.CAT_EAT;
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        boolean flag = super.doHurtTarget(pEntity);
        if (flag) {
            this.playSound(SoundEvents.FOX_BITE, 1.0F, 1.0F);

            if (!this.level().isClientSide && isPrey(pEntity) && pEntity instanceof LivingEntity living && living.isDeadOrDying()) {
                this.setEnrichment(this.maxEnrichment());
            }
        }
        return flag;
    }

    private boolean isPrey(Entity entity) {
        return entity instanceof Rabbit || entity instanceof Chicken;
    }

}

