package com.evirapo.diversityofcritters.common.entity.custom;

import com.evirapo.diversityofcritters.common.entity.DOCEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class LionEntity extends Animal {

    private static final EntityDataAccessor<Boolean> IS_MALE = SynchedEntityData.defineId(LionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(LionEntity.class, EntityDataSerializers.BYTE);

    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    public LionEntity(EntityType<? extends Animal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.lookControl = new LionEntity.LionLookControl();
        this.moveControl = new LionEntity.LionMoveControl();
    }

    public class LionAlertableEntitiesSelector implements Predicate<LivingEntity> {
        public boolean test(LivingEntity pEntity) {
            if (pEntity instanceof LionEntity) {
                return false;
            } else if (!(pEntity instanceof Chicken) && !(pEntity instanceof Rabbit) && !(pEntity instanceof Monster)) {
                if (pEntity instanceof TamableAnimal) {
                    return !((TamableAnimal)pEntity).isTame();
                } else if (!(pEntity instanceof Player) || !pEntity.isSpectator() && !((Player)pEntity).isCreative()) {
                        return !pEntity.isSleeping() && !pEntity.isDiscrete();
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(Items.BEEF) || pStack.is(Items.COOKED_BEEF);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LionFloatGoal());
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.of(Items.BEEF, Items.COOKED_BEEF), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new SeekShelterGoal(1.25D));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(7, new SleepGoal());
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
    }

    //attributes
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, (double)0.2F)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_MALE, true);
        this.entityData.define(DATA_FLAGS_ID, (byte)0);
    }

    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("IsMale", this.getIsMale());
        pCompound.putBoolean("Sleeping", this.isSleeping());
    }

    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setIsMale(pCompound.getBoolean("IsMale"));
        this.setSleeping(pCompound.getBoolean("Sleeping"));
    }

    public Boolean getIsMale() {
        return this.entityData.get(IS_MALE);
    }

    public void setIsMale(Boolean isMale) {
        this.entityData.set(IS_MALE, isMale);
    }

    void clearStates() {
        this.setSleeping(false);
    }

    public boolean isSleeping() {
        return this.getFlag(32);
    }

    void setSleeping(boolean pSleeping) {
        this.setFlag(32, pSleeping);
    }

    private boolean getFlag(int pFlagId) {
        return (this.entityData.get(DATA_FLAGS_ID) & pFlagId) != 0;
    }

    private void setFlag(int pFlagId, boolean pValue) {
        if (pValue) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | pFlagId));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~pFlagId));
        }

    }

    @Override
    public void tick() {
        super.tick();

        if (this.isEffectiveAi()) {
            boolean flag = this.isInWater();
            if (flag || this.getTarget() != null || this.level().isThundering()) {
                this.wakeUp();
            }
        }
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }
    }

    void wakeUp() {
        this.setSleeping(false);
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 39;//this.random.nextInt(40) + 80;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        this.setIsMale(this.random.nextBoolean());
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public boolean canMate(Animal pOtherAnimal) {
        LionEntity otherLion = (LionEntity) pOtherAnimal;
        return otherLion.getIsMale() != this.getIsMale() && super.canMate(pOtherAnimal);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        LionEntity lion = DOCEntities.LION.get().create(pLevel);
        lion.setIsMale(this.random.nextBoolean());
        return lion;
    }

    public class LionLookControl extends LookControl {
        public LionLookControl() {
            super(LionEntity.this);
        }

        public void tick() {
            if (!LionEntity.this.isSleeping()) {
                super.tick();
            }

        }

        protected boolean resetXRotOnTick() {
            return !LionEntity.this.isCrouching();
        }
    }

    class LionMoveControl extends MoveControl {
        public LionMoveControl() {
            super(LionEntity.this);
        }

        public void tick() {
            if (LionEntity.this.canMove()) {
                super.tick();
            }
        }
    }

    boolean canMove() {
        return !this.isSleeping();
    }

    abstract class LionBehaviorGoal extends Goal {
        private final TargetingConditions alertableTargeting = TargetingConditions.forCombat().range(12.0D).ignoreLineOfSight().selector(LionEntity.this.new LionAlertableEntitiesSelector());

        protected boolean hasShelter() {
            BlockPos blockpos = BlockPos.containing(LionEntity.this.getX(), LionEntity.this.getBoundingBox().maxY, LionEntity.this.getZ());
            return !LionEntity.this.level().canSeeSky(blockpos) && LionEntity.this.getWalkTargetValue(blockpos) >= 0.0F;
        }

        protected boolean alertable() {
            return !LionEntity.this.level().getNearbyEntities(LivingEntity.class, this.alertableTargeting, LionEntity.this, LionEntity.this.getBoundingBox().inflate(12.0D, 6.0D, 12.0D)).isEmpty();
        }
    }

    class LionFloatGoal extends FloatGoal {
        public LionFloatGoal() {
            super(LionEntity.this);
        }

        public void start() {
            super.start();
            LionEntity.this.clearStates();
        }
    }

    class SleepGoal extends LionEntity.LionBehaviorGoal {
        private static final int WAIT_TIME_BEFORE_SLEEP = reducedTickDelay(140);
        private int countdown = LionEntity.this.random.nextInt(WAIT_TIME_BEFORE_SLEEP);

        public SleepGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        public boolean canUse() {
            if (LionEntity.this.xxa == 0.0F && LionEntity.this.yya == 0.0F && LionEntity.this.zza == 0.0F) {
                return this.canSleep() || LionEntity.this.isSleeping();
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            } else {
                return LionEntity.this.level().isNight() && !this.alertable() && !LionEntity.this.isInPowderSnow;
            }
        }

        public void stop() {
            this.countdown = LionEntity.this.random.nextInt(WAIT_TIME_BEFORE_SLEEP);
            LionEntity.this.clearStates();
        }

        public void start() {
            LionEntity.this.setJumping(false);
            LionEntity.this.setSleeping(true);
            LionEntity.this.getNavigation().stop();
            LionEntity.this.getMoveControl().setWantedPosition(LionEntity.this.getX(), LionEntity.this.getY(), LionEntity.this.getZ(), 0.0D);
        }
    }

    class SeekShelterGoal extends FleeSunGoal {
        private int interval = reducedTickDelay(100);

        public SeekShelterGoal(double pSpeedModifier) {
            super(LionEntity.this, pSpeedModifier);
        }

        public boolean canUse() {
            if (!LionEntity.this.isSleeping() && this.mob.getTarget() == null) {
                if (LionEntity.this.level().isThundering() && LionEntity.this.level().canSeeSky(this.mob.blockPosition())) {
                    return this.setWantedPos();
                } else if (this.interval > 0) {
                    --this.interval;
                    return false;
                } else {
                    this.interval = 100;
                    BlockPos blockpos = this.mob.blockPosition();
                    return LionEntity.this.level().isNight() && LionEntity.this.level().canSeeSky(blockpos) && !((ServerLevel)LionEntity.this.level()).isVillage(blockpos) && this.setWantedPos();
                }
            } else {
                return false;
            }
        }

        public void start() {
            LionEntity.this.clearStates();
            super.start();
        }
    }
}
