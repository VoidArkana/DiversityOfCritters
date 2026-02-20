package com.evirapo.diversityofcritters.common.entity.custom.base;

import com.evirapo.diversityofcritters.client.menu.DOCStatsMenu;
import com.evirapo.diversityofcritters.common.entity.util.CritterDietConfig;
import com.evirapo.diversityofcritters.common.item.DOCItems;
import com.evirapo.diversityofcritters.network.DOCNetworkHandler;
import com.evirapo.diversityofcritters.network.OpenStatsScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Arrays;

public abstract class DiverseCritter extends TamableAnimal implements ContainerListener, IAnimatedAttacker {

    public enum SleepState {
        AWAKE(0), PREPARING(1), SLEEPING(2), AWAKENING(3);
        private final int id;
        SleepState(int id) { this.id = id; }
        public int getId() { return id; }
        public static SleepState byId(int id) {
            return Arrays.stream(values()).filter(s -> s.id == id).findFirst().orElse(AWAKE);
        }
    }

    // --- DATA ACCESSORS ---
    private static final EntityDataAccessor<Boolean> IS_MALE = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Boolean> IS_JUVENILE = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);

    // [FASE 1] Accessors para el Embarazo y la Píldora
    private static final EntityDataAccessor<Boolean> IS_PREGNANT = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CAN_BREED = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);

    // Stats
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> THIRST = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENRICHMENT = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HYGIENE = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.INT);

    // States
    private static final EntityDataAccessor<Boolean> DRINKING = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> DRINK_POS = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> CLEANING = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);

    // Sleep States
    private static final EntityDataAccessor<Integer> SLEEP_STATE_ID = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Boolean> DIURNAL = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);

    // AI States
    protected static final EntityDataAccessor<Boolean> WANDERING = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_CRYING = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);

    int prevHunger;
    int prevThirst;

    private double hungerLossAccum = 0.0;
    private double thirstLossAccum = 0.0;
    private double enrichmentLossAccum = 0.0;
    private double hygieneLossAccum = 0.0;
    public int attackAnimationTimeout = 0;

    public static final int TICKS_PER_DAY = 24000;
    public static final int TOTAL_GROWTH_TIME = 8 * TICKS_PER_DAY;
    public static final int JUVENILE_AGE_THRESHOLD = -4 * TICKS_PER_DAY;

    public int pregnancyTimer = 0;
    public int breedCooldown = 0;
    public static final int PREGNANCY_TIME_TICKS = 48000;

    public final AnimationState preparingSleepState = new AnimationState();
    public final AnimationState sleepState = new AnimationState();
    public final AnimationState awakeningState = new AnimationState();
    public final AnimationState sitState = new AnimationState();

    private SleepState prevSleepState = SleepState.AWAKE;

    protected DiverseCritter(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.createInventory();
        this.lookControl = new CritterLookControl();
        this.moveControl = new DiverseCritter.CritterMoveControl();
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
    }

    // --- CONFIGURATION ---
    public int maxHunger(){ return 100; }
    public int maxThirst(){ return 100; }
    public int maxEnrichment(){ return 100; }
    public int maxHygiene() { return 100; }

    protected double getHungerLossPerSecond() { return 10.0; }
    protected double getThirstLossPerSecond() { return 20.0; }
    protected double getEnrichmentLossPerSecond() { return 10.0; }
    protected double getHygieneLossPerSecond() { return 5.0; }

    public int getPreparingSleepDuration() { return 40; }
    public int getAwakeningDuration() { return 40; }
    protected boolean getDefaultDiurnal() { return true; }

    // --- SYNC & DATA ---
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_MALE, true);
        this.entityData.define(HUNGER, maxHunger());
        this.entityData.define(THIRST, maxThirst());
        this.entityData.define(ENRICHMENT, maxEnrichment());
        this.entityData.define(HYGIENE, maxHygiene());
        this.entityData.define(DRINKING, false);
        this.entityData.define(CLEANING, false);
        this.entityData.define(ATTACKING, false);
        this.entityData.define(DRINK_POS, Optional.empty());
        this.entityData.define(DATA_FLAGS_ID, (byte)0);
        this.entityData.define(SLEEP_STATE_ID, SleepState.AWAKE.getId());
        this.entityData.define(DIURNAL, getDefaultDiurnal());
        this.entityData.define(WANDERING, false);
        this.entityData.define(IS_JUVENILE, false);
        this.entityData.define(IS_CRYING, false);
        this.entityData.define(IS_PREGNANT, false);
        this.entityData.define(CAN_BREED, true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("IsMale", this.getIsMale());
        pCompound.putInt("Hunger", this.getHunger());
        pCompound.putInt("Thirst", this.getThirst());
        pCompound.putInt("Enrichment", this.getEnrichment());
        pCompound.putInt("Hygiene", this.getHygiene());
        pCompound.putBoolean("Cleaning", this.isCleaning());
        pCompound.putInt("SleepState", this.getSleepState().getId());
        pCompound.putBoolean("Diurnal", this.isDiurnal());
        pCompound.putBoolean("Wandering", this.isWandering());
        pCompound.putInt("DiverseAge", this.getAge());
        pCompound.putBoolean("IsPregnant", this.isPregnant());
        pCompound.putBoolean("CanBreed", this.canBreed());
        pCompound.putInt("PregnancyTimer", this.pregnancyTimer);
        pCompound.putInt("BreedCooldown", this.breedCooldown);
    }

    private Integer loadedNbtAge = null;

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("DiverseAge")) {
            this.loadedNbtAge = pCompound.getInt("DiverseAge");
            if (this.loadedNbtAge != null) {
                this.setAge(this.loadedNbtAge);
            }
        }
        this.updateJuvenileState(this.getAge());
        if (pCompound.contains("IsMale")) this.setIsMale(pCompound.getBoolean("IsMale"));
        if (pCompound.contains("Hunger")) this.setHunger(pCompound.getInt("Hunger"));
        if (pCompound.contains("Thirst")) this.setThirst(pCompound.getInt("Thirst"));
        if (pCompound.contains("Enrichment")) this.setEnrichment(pCompound.getInt("Enrichment"));
        if (pCompound.contains("Hygiene")) {
            this.setHygiene(pCompound.getInt("Hygiene"));
        }
        this.setCleaning(pCompound.getBoolean("Cleaning"));
        if (pCompound.contains("SleepState")) {
            this.setSleepState(SleepState.byId(pCompound.getInt("SleepState")));
        }
        if (pCompound.contains("Diurnal")) {
            this.setDiurnal(pCompound.getBoolean("Diurnal"));
        }
        this.setWandering(pCompound.getBoolean("Wandering"));

        // [FASE 1] Carga de NBT
        if (pCompound.contains("IsPregnant")) this.setPregnant(pCompound.getBoolean("IsPregnant"));
        if (pCompound.contains("CanBreed")) this.setCanBreed(pCompound.getBoolean("CanBreed"));
        if (pCompound.contains("PregnancyTimer")) this.pregnancyTimer = pCompound.getInt("PregnancyTimer");
        if (pCompound.contains("BreedCooldown")) this.breedCooldown = pCompound.getInt("BreedCooldown");
    }

    // --- GETTERS & SETTERS ---

    public Boolean getIsMale() { return this.entityData.get(IS_MALE); }
    public void setIsMale(Boolean isMale) { this.entityData.set(IS_MALE, isMale); }

    // [FASE 1] Getters y Setters de Reproducción
    public boolean isPregnant() { return this.entityData.get(IS_PREGNANT); }
    public void setPregnant(boolean pregnant) { this.entityData.set(IS_PREGNANT, pregnant); }
    public boolean canBreed() { return this.entityData.get(CAN_BREED); }
    public void setCanBreed(boolean canBreed) { this.entityData.set(CAN_BREED, canBreed); }

    // Percentages
    public int getHungerPercentage() { return (100 * this.getHunger())/this.maxHunger(); }
    public int getThirstPercentage() { return (100 * this.getThirst())/this.maxThirst(); }
    public int getEnrichmentPercentage() {return (100 * this.getEnrichment()) / this.maxEnrichment();}
    public int getHygienePercentage() {return (100 * this.getHygiene()) / this.maxHygiene();}

    public int getHunger() { return this.entityData.get(HUNGER); }
    public void setHunger(int hunger) { this.entityData.set(HUNGER, Math.max(0, Math.min(maxHunger(), hunger))); }
    public int getThirst() { return this.entityData.get(THIRST); }
    public void setThirst(int thirst) { this.entityData.set(THIRST, Math.max(0, Math.min(maxThirst(), thirst))); }
    public int getEnrichment() {return this.entityData.get(ENRICHMENT);}
    public void setEnrichment(int enrichment) { this.entityData.set(ENRICHMENT, Math.max(0, Math.min(maxEnrichment(), enrichment))); }
    public int getHygiene() { return this.entityData.get(HYGIENE); }
    public void setHygiene(int hygiene) { this.entityData.set(HYGIENE, Math.max(0, Math.min(maxHygiene(), hygiene))); }

    // Flags
    public boolean isCleaning() { return this.entityData.get(CLEANING); }
    public void setCleaning(boolean value) { this.entityData.set(CLEANING, value); }
    public boolean hasLowHygiene() { return this.getHygienePercentage() < 50; }

    public Boolean IsDrinking() { return this.entityData.get(DRINKING); }
    public void setIsDrinking(Boolean isDrinking) { this.entityData.set(DRINKING, isDrinking); }

    public BlockPos getDrinkPos() { return this.entityData.get(DRINK_POS).orElse(null); }
    public void setDrinkPos(BlockPos washingPos) { this.entityData.set(DRINK_POS, Optional.ofNullable(washingPos)); }

    public boolean isWandering() { return this.entityData.get(WANDERING); }
    public void setWandering(boolean value) { this.entityData.set(WANDERING, value); }
    public boolean isFollowing() { return !this.isOrderedToSit() && !this.isWandering(); }

    @Override public boolean isAttacking() { return this.entityData.get(ATTACKING); }
    @Override public void setAttacking(boolean attacking) { this.entityData.set(ATTACKING, attacking); }
    @Override public int attackAnimationTimeout() { return this.attackAnimationTimeout; }
    @Override public void setAttackAnimationTimeout(int timeout) { this.attackAnimationTimeout = timeout; }

    public boolean isJuvenile() {return this.entityData.get(IS_JUVENILE);}
    public boolean isNewborn() {return this.isBaby() && !isJuvenile();}

    public boolean isCrying() { return this.entityData.get(IS_CRYING); }
    public void setCrying(boolean val) { this.entityData.set(IS_CRYING, val); }

    // Sleep Accessors
    public SleepState getSleepState() { return SleepState.byId(this.entityData.get(SLEEP_STATE_ID)); }
    public void setSleepState(SleepState state) { this.entityData.set(SLEEP_STATE_ID, state.getId()); }

    public boolean isSleeping() { return getSleepState() == SleepState.SLEEPING; }
    public boolean isPreparingSleep() { return getSleepState() == SleepState.PREPARING; }
    public boolean isAwakeing() { return getSleepState() == SleepState.AWAKENING; }

    public boolean isDiurnal() { return this.entityData.get(DIURNAL); }
    public void setDiurnal(boolean value) { this.entityData.set(DIURNAL, value); }

    public void clearStates() { this.setSleepState(SleepState.AWAKE); this.setCleaning(false); }

    protected boolean getFlag(int pFlagId) { return (this.entityData.get(DATA_FLAGS_ID) & pFlagId) != 0; }
    protected void setFlag(int pFlagId, boolean pValue) {
        if (pValue) this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | pFlagId));
        else this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~pFlagId));
    }

    private Integer fixAgeOnFirstTick = null;

    // --- MAIN TICK ---
    @Override
    public void tick() {
        if (!this.level().isClientSide && this.fixAgeOnFirstTick != null) {
            this.setAge(this.fixAgeOnFirstTick);
            this.updateJuvenileState(this.fixAgeOnFirstTick);
            this.fixAgeOnFirstTick = null;
        }
        super.tick();

        if (!this.level().isClientSide()) {

            // [FASE 1] Temporizadores de Reproducción y Gestación
            if (this.breedCooldown > 0) {
                this.breedCooldown--;
            }

            if (this.isPregnant()) {
                this.pregnancyTimer--;
                if (this.pregnancyTimer <= 0) {
                    this.giveBirth();
                }
            }

            if (this.getHunger() > 0) {
                double lossPerTick = getHungerLossPerSecond() / 20.0;
                hungerLossAccum += lossPerTick;
                if (hungerLossAccum >= 1.0) {
                    int loss = (int) hungerLossAccum; hungerLossAccum -= loss;
                    this.prevHunger = this.getHunger(); this.setHunger(prevHunger - loss);
                }
            }
            if (this.getThirst() > 0) {
                double lossPerTick = getThirstLossPerSecond() / 20.0;
                thirstLossAccum += lossPerTick;
                if (thirstLossAccum >= 1.0) {
                    int loss = (int) thirstLossAccum; thirstLossAccum -= loss;
                    this.prevThirst = this.getThirst(); this.setThirst(prevThirst - loss);
                }
            }
            if (this.getEnrichment() > 0) {
                double lossPerTick = getEnrichmentLossPerSecond() / 20.0;
                enrichmentLossAccum += lossPerTick;
                if (enrichmentLossAccum >= 1.0) {
                    int loss = (int) enrichmentLossAccum; enrichmentLossAccum -= loss;
                    this.setEnrichment(this.getEnrichment() - loss);
                }
            }
            if (this.getHygiene() > 0) {
                double lossPerTick = getHygieneLossPerSecond() / 20.0;
                hygieneLossAccum += lossPerTick;
                if (hygieneLossAccum >= 1.0) {
                    int loss = (int) hygieneLossAccum; hygieneLossAccum -= loss;
                    this.setHygiene(this.getHygiene() - loss);
                }
            }
            if ((this.getHunger() <= 0 || this.getThirst() <= 0) && random.nextInt(10) > 8){ this.starve(); }
        }

        if (this.isOrderedToSit()) {
            if (!this.level().isClientSide) this.getNavigation().stop();
            Vec3 currentDelta = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, currentDelta.y, 0.0D);
        }

        if (this.level().isClientSide()) {
            updateSitAnimationClient();
            handleSleepAnimationsClient();
        }
    }

    protected void giveBirth() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        AgeableMob baby = this.getBreedOffspring(serverLevel, this);

        if (baby != null) {
            baby.setBaby(true);
            baby.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);

            serverLevel.addFreshEntityWithPassengers(baby);

            serverLevel.broadcastEntityEvent(this, (byte) 18);
        }

        this.setPregnant(false);
        this.pregnancyTimer = 0;
        this.breedCooldown = 96000 + this.random.nextInt(24000);
    }

    private void handleSleepAnimationsClient() {
        SleepState current = getSleepState();

        if (prevSleepState != current) {
            preparingSleepState.stop();
            sleepState.stop();
            awakeningState.stop();

            switch (current) {
                case PREPARING -> preparingSleepState.start(this.tickCount);
                case SLEEPING -> sleepState.start(this.tickCount);
                case AWAKENING -> awakeningState.start(this.tickCount);
                default -> {}
            }
            prevSleepState = current;
        }
    }

    protected void updateSitAnimationClient() {
        if (this.getSleepState() != SleepState.AWAKE) {
            if (sitState.isStarted()) sitState.stop();
            return;
        }

        boolean isOrdered = this.isOrderedToSit();
        boolean inPose = this.isInSittingPose();
        boolean wandering = this.isWandering();
        boolean isStarted = sitState.isStarted();

        boolean shouldSit = isOrdered || (inPose && !wandering);

        if (shouldSit && !isStarted) {
            sitState.start(this.tickCount);
        } else if (!shouldSit && isStarted) {
            sitState.stop();
        }
    }

    protected void starve() { this.hurt(level().damageSources().starve(), 1); }

    // --- CONTROLS ---
    public class CritterLookControl extends LookControl {
        public CritterLookControl() { super(DiverseCritter.this); }
        @Override public void tick() { if (DiverseCritter.this.getSleepState() == SleepState.AWAKE) super.tick(); }
        @Override protected boolean resetXRotOnTick() { return !DiverseCritter.this.isCrouching(); }
    }
    public class CritterMoveControl extends MoveControl {
        public CritterMoveControl() { super(DiverseCritter.this); }
        public void tick() { if (DiverseCritter.this.canMove()) super.tick(); }
    }

    // --- INVENTORY ---
    private boolean inventoryOpen;
    private DiverseInventory inventory;
    private void createInventory() {
        SimpleContainer simplecontainer = this.getInventory();
        this.inventory = new DiverseInventory(this);
        if (simplecontainer != null) simplecontainer.removeListener(this);
        this.getInventory().addListener(this);
    }
    public void openGUI(Player player) {
        if (!this.level().isClientSide()) {
            ServerPlayer sp = (ServerPlayer) player;
            if (sp.containerMenu != sp.inventoryMenu) sp.closeContainer();
            sp.nextContainerCounter();
            DOCNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new OpenStatsScreenPacket(sp.containerCounter, this.getId()));
            sp.containerMenu = new DOCStatsMenu(sp.containerCounter, this.getInventory(), sp.getInventory());
            sp.initMenu(sp.containerMenu);
            this.inventoryOpen = true;
            MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(sp, sp.containerMenu));
        }
    }
    @Nullable public SimpleContainer getInventory() { return this.inventory; }
    public void closeInventory() { this.inventoryOpen = false; }
    @Override public void containerChanged(Container pContainer) {}

    // --- LOGIC ---
    public boolean isThirsty() { return getThirst() <= (int)(maxThirst() * 0.5f); }
    public boolean isHungry() { return getHunger() <= (int)(maxHunger() * 0.5f); }
    public boolean isEnrichmentNeeded() { return getEnrichment() <= (int)(maxEnrichment() * 0.5f); }

    public boolean canMove() {
        return getSleepState() == SleepState.AWAKE
                && !this.isOrderedToSit() && !this.isCleaning();
    }

    protected void messageState(String state, Player player) {
        player.displayClientMessage(this.getName().copy().append(Component.literal(" is now " + state)), true);
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        if (itemstack.is(DOCItems.ZOO_BOOK.get())) {
            this.openGUI(pPlayer);
            return InteractionResult.SUCCESS;
        }

        if (this.isBaby() && itemstack.is(DOCItems.FILLED_NURSER_BOTTLE.get())) {
            if (!this.level().isClientSide) {
                if (!this.isTame()) {
                    this.tame(pPlayer);
                }

                this.setHunger(this.maxHunger());
                this.setThirst(this.maxThirst());

                this.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-this.getAge()), true);

                this.level().broadcastEntityEvent(this, (byte) 7);

                itemstack.hurtAndBreak(1, pPlayer, (player) -> {
                    player.broadcastBreakEvent(pHand);
                });

                if (itemstack.isEmpty()) {
                    pPlayer.setItemInHand(pHand, new ItemStack(DOCItems.EMPTY_NURSER_BOTTLE.get()));
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.isOwnedBy(pPlayer) && itemstack.is(DOCItems.TRAINING_STICK.get())) {
            if (this.isNewborn()) {
                if (level().isClientSide) pPlayer.displayClientMessage(Component.literal("Too young to follow orders!"), true);
                return InteractionResult.PASS;
            }

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

        if (this.isBaby() && this.isFood(itemstack)) {
            return InteractionResult.PASS;
        }

        return super.mobInteract(pPlayer, pHand);
    }

    protected void setModeSit(boolean sit) {
        this.setWandering(false);
        this.setOrderedToSit(sit);
        this.setInSittingPose(sit);
        if (sit) {
            if (!level().isClientSide) {
                this.getNavigation().stop();
                this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
            }
            this.setSprinting(false);
            this.setJumping(false);
            Vec3 dm = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, dm.y, 0.0D);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {

        SpawnGroupData res = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        if (reason != MobSpawnType.EVENT) {
            if (dataTag == null || !dataTag.contains("IsMale")) {
                this.setIsMale(this.random.nextBoolean());
            }
        }


        if (reason == MobSpawnType.EVENT && this.loadedNbtAge != null) {

            if (this.getAge() > this.loadedNbtAge) {
                this.setAge(this.loadedNbtAge);
                this.updateJuvenileState(this.loadedNbtAge);
            }
        }

        return res;
    }

    @Override
    public void aiStep() {
        if (getSleepState() != SleepState.AWAKE) setTarget(null);
        super.aiStep();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (getSleepState() != SleepState.AWAKE || this.isOrderedToSit() || this.IsDrinking() || this.isCleaning()) {
            if (!level().isClientSide) {
                this.getNavigation().stop();
                this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
            }
            Vec3 dm = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, dm.y, 0.0D);
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (!this.level().isClientSide && result) {
            if (this.isCleaning()) this.setCleaning(false);
        }
        return result;
    }

    // --- DIET ---
    public static final boolean DEBUG_BOWL_GOALS = true;
    public abstract CritterDietConfig getDietConfig();
    public void debugGoalMessage(String goalName, String state) {
        if (!DEBUG_BOWL_GOALS) return;
        if (this.level().isClientSide()) return;
        String base = "[BOWL-GOAL][" + this.getName().getString() + "] " + goalName + " " + state;
        System.out.println(base);
        for (Player player : this.level().players()) { if (player.distanceTo(this) < 32.0F) player.displayClientMessage(Component.literal(base), true); }
    }

    //--MATE--
    // [FASE 1] Aquí dejamos canMate preparado, aunque usaremos nuestra propia IA después.
    @Override
    public boolean canMate(Animal otherAnimal) {
        if (otherAnimal == this || !this.canBreed()) return false;
        if (!(otherAnimal instanceof DiverseCritter otherCritter)) return false;
        if (!otherCritter.canBreed()) return false;

        // No se pueden reproducir si son del mismo género, si alguno está embarazada, o sentado
        if (this.getIsMale() == otherCritter.getIsMale()) return false;
        if (this.isPregnant() || otherCritter.isPregnant()) return false;
        if (this.isOrderedToSit() || otherCritter.isOrderedToSit()) return false;
        if (this.isBaby() || otherCritter.isBaby()) return false;

        // Cooldown en 0 para ambos
        return this.breedCooldown <= 0 && otherCritter.breedCooldown <= 0;
    }

    @Override
    public void setBaby(boolean pBaby) {
        this.setAge(pBaby ? -TOTAL_GROWTH_TIME : 0);
    }

    @Override
    public void setAge(int pAge) {
        if (pAge == -24000) {
            int currentAge = this.getAge();
            if (currentAge < -40000) {
                return;
            }
        }

        super.setAge(pAge);
        this.updateJuvenileState(pAge);
    }

    private void updateJuvenileState(int age) {
        boolean isJuvenile = age < 0 && age >= JUVENILE_AGE_THRESHOLD;
        if (this.entityData.get(IS_JUVENILE) != isJuvenile) {
            this.entityData.set(IS_JUVENILE, isJuvenile);
        }
    }

    @Override
    protected float getJumpPower() {
        return this.isNewborn() ? 0.0F : super.getJumpPower();
    }

    @Override
    public boolean canPickUpLoot() {
        return !this.isNewborn();
    }

    @Override
    public void ageUp(int pAmount, boolean pForced) {
        if (pForced) {
            super.ageUp(pAmount, true);
        }
    }
}