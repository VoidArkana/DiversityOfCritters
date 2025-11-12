package com.evirapo.diversityofcritters.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReleaseCritterMsg {
    private final int entityId;

    public ReleaseCritterMsg(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(ReleaseCritterMsg msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    public static ReleaseCritterMsg decode(FriendlyByteBuf buf) {
        return new ReleaseCritterMsg(buf.readInt());
    }

    public int getEntityId() {
        return entityId;
    }

    public static void handle(ReleaseCritterMsg msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity e = player.level().getEntity(msg.getEntityId());
            if (e instanceof TamableAnimal ta && ta.isOwnedBy(player)) {
                ta.setTame(false);
                ta.setOwnerUUID(null);
                ta.setOrderedToSit(false);
                ta.setInSittingPose(false);
                ta.setPersistenceRequired();

                player.displayClientMessage(
                        ta.getDisplayName().copy().append(Component.literal(" was released")),
                        true
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
