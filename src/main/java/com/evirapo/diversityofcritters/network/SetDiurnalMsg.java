package com.evirapo.diversityofcritters.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetDiurnalMsg {
    private final int entityId;
    private final boolean diurnal;

    public SetDiurnalMsg(int entityId, boolean diurnal) {
        this.entityId = entityId;
        this.diurnal = diurnal;
    }

    public static void encode(SetDiurnalMsg msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.diurnal);
    }

    public static SetDiurnalMsg decode(FriendlyByteBuf buf) {
        return new SetDiurnalMsg(buf.readInt(), buf.readBoolean());
    }

    public static void handle(SetDiurnalMsg msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player == null) return;

            var level = player.level();
            var e = level.getEntity(msg.entityId);

            if (e instanceof com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter critter) {
                critter.setDiurnal(msg.diurnal);

                System.out.println("[SLEEP][SVR][GUI] SetDiurnal entity#" + critter.getId() + " -> " + msg.diurnal);
            }
        });
        ctx.setPacketHandled(true);
    }
}