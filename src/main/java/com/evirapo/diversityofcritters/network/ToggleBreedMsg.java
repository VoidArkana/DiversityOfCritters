package com.evirapo.diversityofcritters.network;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleBreedMsg {
    public final int entityId;
    public final boolean canBreed;

    public ToggleBreedMsg(int entityId, boolean canBreed) {
        this.entityId = entityId;
        this.canBreed = canBreed;
    }

    public ToggleBreedMsg(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.canBreed = buffer.readBoolean();
    }

    public static ToggleBreedMsg decode(FriendlyByteBuf buffer) {
        return new ToggleBreedMsg(buffer);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeBoolean(this.canBreed);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Entity entity = player.level().getEntity(this.entityId);
                if (entity instanceof DiverseCritter critter && critter.isOwnedBy(player)) {
                    critter.setCanBreed(this.canBreed);
                }
            }
        });
        context.setPacketHandled(true);
    }
}