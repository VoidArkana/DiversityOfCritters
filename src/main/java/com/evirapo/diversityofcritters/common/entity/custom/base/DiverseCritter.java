package com.evirapo.diversityofcritters.common.entity.custom.base;

import com.evirapo.diversityofcritters.client.menu.DOCStatsMenu;
import com.evirapo.diversityofcritters.common.item.DOCItems;
import com.evirapo.diversityofcritters.network.DOCNetworkHandler;
import com.evirapo.diversityofcritters.network.OpenStatsScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class DiverseCritter extends Animal implements ContainerListener {

    private static final EntityDataAccessor<Boolean> IS_MALE = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> THIRST = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DRINKING = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> DRINK_POS = SynchedEntityData.defineId(DiverseCritter.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    int prevHunger;
    int prevThirst;

    protected DiverseCritter(EntityType<? extends Animal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.createInventory();
        this.lookControl = new CritterLookControl();
        this.moveControl = new DiverseCritter.CritterMoveControl();
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
    }
    public int maxHunger(){
        return 100;
    }

    public int maxThirst(){
        return 100;
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_MALE, true);
        this.entityData.define(HUNGER, maxHunger());
        this.entityData.define(THIRST, maxThirst());
        this.entityData.define(DRINKING, false);
        this.entityData.define(DRINK_POS, Optional.empty());
        this.entityData.define(DATA_FLAGS_ID, (byte)0);
    }

    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("IsMale", this.getIsMale());
        pCompound.putInt("Hunger", this.getHunger());
        pCompound.putInt("Thirst", this.getThirst());
        //pCompound.putBoolean("Sleeping", this.isSleeping());
    }

    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setIsMale(pCompound.getBoolean("IsMale"));
        this.setHunger(pCompound.getInt("Hunger"));
        this.setThirst(pCompound.getInt("Thirst"));
//        this.setSleeping(pCompound.getBoolean("Sleeping"));
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

    public Boolean getIsDrinking() {
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

    protected void wakeUp() {
        this.setSleeping(false);
    }

    public boolean isSleeping() {
        return this.getFlag(32);
    }

    protected void setSleeping(boolean pSleeping) {
        this.setFlag(32, pSleeping);
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
        if (this.getHunger()>0){
            this.prevHunger = this.getHunger();
            this.setHunger(prevHunger-1);
        }

        if (this.getThirst()>0){
            this.prevThirst = this.getThirst();
            this.setThirst(prevThirst-1);
        }

        if (this.getHunger() <= 0 && this.getThirst() <= 0 && random.nextFloat() <= 0.07){
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

    boolean canMove() {
        return !this.isSleeping();
    }

    public boolean isThirsty(){
        return this.getThirst() > (this.maxThirst()/2);
    }

    public boolean isHungry(){
        return this.getHunger() < (this.maxHunger()/2);
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

        if(itemstack.is(DOCItems.ZOO_BOOK.get())){
            this.openGUI(pPlayer);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(pPlayer, pHand);
    }

}
