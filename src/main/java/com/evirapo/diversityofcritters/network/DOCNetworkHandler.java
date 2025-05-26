package com.evirapo.diversityofcritters.network;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class DOCNetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DiversityOfCritters.MODID, "channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    @SuppressWarnings("UnusedAssignment")
    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenStatsScreenPacket.class, OpenStatsScreenPacket::encode, OpenStatsScreenPacket::decode, OpenStatsScreenPacket.Handler::handle);
    }
}
