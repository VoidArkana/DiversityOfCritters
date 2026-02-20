package com.evirapo.diversityofcritters.network;

import com.evirapo.diversityofcritters.client.menu.DOCStatsMenu;
import com.evirapo.diversityofcritters.client.screen.CivetStatScreen;
import com.evirapo.diversityofcritters.client.screen.DOCStatScreen;
import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenStatsScreenPacket (int containerId, int entityId) {

    public static OpenStatsScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenStatsScreenPacket(buf.readInt(), buf.readInt());
    }

    public static void encode(OpenStatsScreenPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.containerId());
        buf.writeInt(packet.entityId());
    }

    public static class Handler {

        @SuppressWarnings("Convert2Lambda")
        public static void handle(OpenStatsScreenPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(new Runnable() {
                @Override
                public void run() {
                    Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());
                    if (entity instanceof DiverseCritter critter) {
                        LocalPlayer localplayer = Minecraft.getInstance().player;
                        SimpleContainer container = new SimpleContainer(6);
                        DOCStatsMenu menu = new DOCStatsMenu(packet.containerId(), container, localplayer.getInventory());
                        localplayer.containerMenu = menu;
                        if (critter instanceof CivetEntity) {
                            Minecraft.getInstance().setScreen(new CivetStatScreen(menu, localplayer.getInventory(), critter));
                        } else {
                            Minecraft.getInstance().setScreen(new DOCStatScreen(menu, localplayer.getInventory(), critter));
                        }
                    }
                }
            });
            context.get().setPacketHandled(true);
        }
    }
}
