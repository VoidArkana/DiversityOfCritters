package com.evirapo.diversityofcritters.common.entity.custom.base;

import com.evirapo.diversityofcritters.client.menu.DOCStatsMenu;
import com.evirapo.diversityofcritters.common.entity.util.ISleepingEntity;
import com.evirapo.diversityofcritters.common.entity.util.SleepCycleController;
import com.evirapo.diversityofcritters.common.item.DOCItems;
import com.evirapo.diversityofcritters.network.DOCNetworkHandler;
import com.evirapo.diversityofcritters.network.OpenStatsScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class DiverseCritter extends TamableAnimal implements ContainerListener, ISleepingEntity {

    private static final EntityDataAccessor<Boolean> IS_MALE = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> THIRST = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DRINKING = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> DRINK_POS = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    protected static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> PREPARING_SLEEP = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> AWAKENING = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DIURNAL = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);

    int prevHunger;
    int prevThirst;

    protected DiverseCritter(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.createInventory();
        this.lookControl = new CritterLookControl();
        this.moveControl = new DiverseCritter.CritterMoveControl();
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.sleepController = createSleepController();
    }

    public int maxHunger(){
        return 100;
    }

    public int maxThirst(){
        return 100;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_MALE, true);
        this.entityData.define(HUNGER, maxHunger());
        this.entityData.define(THIRST, maxThirst());
        this.entityData.define(DRINKING, false);
        this.entityData.define(DRINK_POS, Optional.empty());
        this.entityData.define(DATA_FLAGS_ID, (byte)0);
        this.entityData.define(SLEEPING, false);
        this.entityData.define(PREPARING_SLEEP, false);
        this.entityData.define(AWAKENING, false);
        this.entityData.define(DIURNAL, getDefaultDiurnal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("IsMale", this.getIsMale());
        pCompound.putInt("Hunger", this.getHunger());
        pCompound.putInt("Thirst", this.getThirst());
        pCompound.putBoolean("Sleeping", isSleeping());
        pCompound.putBoolean("PreparingSleep", isPreparingSleep());
        pCompound.putBoolean("Awakening", isAwakeing());
        pCompound.putBoolean("Diurnal", this.isDiurnal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setIsMale(pCompound.getBoolean("IsMale"));
        this.setHunger(pCompound.getInt("Hunger"));
        this.setThirst(pCompound.getInt("Thirst"));
        setSleeping(pCompound.getBoolean("Sleeping"));
        setPreparingSleep(pCompound.getBoolean("PreparingSleep"));
        setAwakeing(pCompound.getBoolean("Awakening"));
        this.setDiurnal(pCompound.getBoolean("Diurnal"));
    }

    public Boolean getIsMale() {
        return this.entityData.get(IS_MALE);
    }

    public void setIsMale(Boolean isMale) {
        this.entityData.set(IS_MALE, isMale);
    }

    public int getHungerPercentage() {
        return (100 * this.getHunger())/this.maxHunger();
    }

    public int getThirstPercentage() {
        return (100 * this.getThirst())/this.maxThirst();
    }

    public int getHunger() {
        return this.entityData.get(HUNGER);
    }

    public void setHunger(int hunger) {
        this.entityData.set(HUNGER, hunger);
    }

    public int getThirst() {
        return this.entityData.get(THIRST);
    }

    public void setThirst(int hunger) {
        this.entityData.set(THIRST, hunger);
    }

    public Boolean IsDrinking() {
        return this.entityData.get(DRINKING);
    }

    public void setIsDrinking(Boolean isDrinking) {
        this.entityData.set(DRINKING, isDrinking);
    }

    public BlockPos getDrinkPos() {
        return this.entityData.get(DRINK_POS).orElse(null);
    }

    public void setDrinkPos(BlockPos washingPos) {
        this.entityData.set(DRINK_POS, Optional.ofNullable(washingPos));
    }

    public void clearStates() {
        this.setSleeping(false);
    }

    protected boolean getFlag(int pFlagId) {
        return (this.entityData.get(DATA_FLAGS_ID) & pFlagId) != 0;
    }

    protected void setFlag(int pFlagId, boolean pValue) {
        if (pValue) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | pFlagId));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~pFlagId));
        }
    }

    @Override
    public void tick() {

        if (sleepController == null) {
            sleepController = createSleepController();
        }
        if (!this.level().isClientSide()) {
            sleepController.tick(this.tickCount);
        }

        if (this.getHunger()>0){
            this.prevHunger = this.getHunger();
            this.setHunger(prevHunger-1);
        }

        if (this.getThirst()>0){
            this.prevThirst = this.getThirst();
            this.setThirst(prevThirst-1);
        }

        if ((this.getHunger() <= 0 || this.getThirst() <= 0) && random.nextInt(10) > 8){
            this.starve();
        }

        super.tick();
    }

    protected void starve() {
        this.hurt(level().damageSources().starve(), 1);
    }

    public class CritterLookControl extends LookControl {
        public CritterLookControl() {
            super(DiverseCritter.this);
        }

        public void tick() {
            if (!DiverseCritter.this.isSleeping()) {
                super.tick();
            }

        }

        protected boolean resetXRotOnTick() {
            return !DiverseCritter.this.isCrouching();
        }
    }

    public class CritterMoveControl extends MoveControl {
        public CritterMoveControl() {
            super(DiverseCritter.this);
        }

        public void tick() {
            if (DiverseCritter.this.canMove()) {
                super.tick();
            }
        }
    }

    public boolean isThirsty(){
        return this.getThirst() > (this.maxThirst()/2);
    }

    public boolean isHungry(){
        return this.getHunger() <= (this.maxHunger()/2);
    }

    private boolean inventoryOpen;
    private DiverseInventory inventory;

    private void createInventory() {
        SimpleContainer simplecontainer = this.getInventory();
        this.inventory = new DiverseInventory(this);
        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
        }

        this.getInventory().addListener(this);
    }

    public void openGUI(Player player) {
        if (!this.level().isClientSide()) {
            ServerPlayer sp = (ServerPlayer) player;
            if (sp.containerMenu != sp.inventoryMenu) {
                sp.closeContainer();
            }

            sp.nextContainerCounter();
            DOCNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new OpenStatsScreenPacket(sp.containerCounter, this.getId()));
            sp.containerMenu = new DOCStatsMenu(sp.containerCounter, this.getInventory(), sp.getInventory());
            sp.initMenu(sp.containerMenu);
            this.inventoryOpen = true;
            MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(sp, sp.containerMenu));
        }
    }

    @Nullable
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public void closeInventory() {
        this.inventoryOpen = false;
    }

    @Override
    public void containerChanged(Container pContainer) {}

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        if (itemstack.is(DOCItems.ZOO_BOOK.get())) {
            this.openGUI(pPlayer);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(pPlayer, pHand);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        SpawnGroupData res = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        if (dataTag == null || !dataTag.contains("IsMale")) {
            this.setIsMale(this.random.nextBoolean());
        }
        return res;
    }


    //--------Sleep----------

    @Override public boolean isSleeping() { return entityData.get(SLEEPING); }
    @Override public void setSleeping(boolean value) { entityData.set(SLEEPING, value); }
    @Override public boolean isPreparingSleep() { return entityData.get(PREPARING_SLEEP); }
    @Override public void setPreparingSleep(boolean value) { entityData.set(PREPARING_SLEEP, value); }
    @Override public boolean isAwakeing() { return entityData.get(AWAKENING); }
    @Override public void setAwakeing(boolean value) { entityData.set(AWAKENING, value); }

    public boolean isDiurnal() { return this.entityData.get(DIURNAL); }
    public void setDiurnal(boolean value) { this.entityData.set(DIURNAL, value); }

    public final AnimationState preparingSleepState = new AnimationState();
    public final AnimationState sleepState = new AnimationState();
    public final AnimationState awakeningState = new AnimationState();

    public SleepCycleController<DiverseCritter> sleepController;

    protected int getPreparingSleepDuration() { return 0; }

    protected int getAwakeningDuration() { return 0; }

    protected boolean getDefaultDiurnal() { return true; }

    protected SleepCycleController<DiverseCritter> createSleepController() {
        return new SleepCycleController<>(
                this, preparingSleepState, sleepState, awakeningState,
                getPreparingSleepDuration(), getAwakeningDuration()
        );
    }

    protected boolean isIdleLocked() {
        return false;
    }

    boolean canMove() {
        return !this.isSleeping() && !this.isAwakeing() && !this.isPreparingSleep() && !this.isIdleLocked();
    }

    @Override
    public void aiStep() {
        if (isSleeping() || isPreparingSleep() || isAwakeing()) {
            setTarget(null);
        }
        super.aiStep();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isSleeping() || isPreparingSleep() || isAwakeing() || isIdleLocked()) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            getNavigation().stop();
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (!this.level().isClientSide && result && this.getTarget() == null) {
            if (this.sleepController != null) {
                this.sleepController.interruptSleep("damage", this.tickCount);
            }
        }
        return result;
    }

    private int lastAnimationChangeTick = -20;
    private static final int MIN_TICKS_BETWEEN_ANIMS = 3;

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!this.level().isClientSide()) return;
        if (this.tickCount - lastAnimationChangeTick < MIN_TICKS_BETWEEN_ANIMS) {
            return;
        }
        if (key == PREPARING_SLEEP) {
            if (this.isPreparingSleep()) {
                preparingSleepState.start(this.tickCount);
                sleepState.stop();
                awakeningState.stop();
                lastAnimationChangeTick = this.tickCount;
            } else {
                preparingSleepState.stop();
            }
        }
        if (key == SLEEPING) {
            if (this.isSleeping()) {
                sleepState.start(this.tickCount);
                preparingSleepState.stop();
                awakeningState.stop();
                lastAnimationChangeTick = this.tickCount;
            } else {
                sleepState.stop();
            }
        }
        if (key == AWAKENING) {
            if (this.isAwakeing()) {
                awakeningState.start(this.tickCount);
                sleepState.stop();
                preparingSleepState.stop();
                lastAnimationChangeTick = this.tickCount;
            } else {
                awakeningState.stop();
            }
        }
    }

}
