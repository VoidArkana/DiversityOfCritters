package com.evirapo.diversityofcritters.common.command;

import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SetNeedsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("doc_hunger")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                        .executes(context -> setStat(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "value"),
                                "hunger"
                        ))
                )
        );

        dispatcher.register(Commands.literal("doc_thirst")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                        .executes(context -> setStat(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "value"),
                                "thirst"
                        ))
                )
        );

        dispatcher.register(Commands.literal("doc_enrichment")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                        .executes(context -> setStat(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "value"),
                                "enrichment"
                        ))
                )
        );

        dispatcher.register(Commands.literal("doc_hygiene")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                        .executes(context -> setStat(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "value"),
                                "hygiene"
                        ))
                )
        );
    }

    private static int setStat(CommandSourceStack source, int value, String statName) {
        int successCount = 0;

        Vec3 pos = source.getPosition();
        AABB searchArea = new AABB(
                pos.x - 5, pos.y - 5, pos.z - 5,
                pos.x + 5, pos.y + 5, pos.z + 5
        );

        List<DiverseCritter> nearbyCritters = source.getLevel().getEntitiesOfClass(DiverseCritter.class, searchArea);

        for (DiverseCritter critter : nearbyCritters) {
            switch (statName) {
                case "hunger" -> critter.setHunger(value);
                case "thirst" -> critter.setThirst(value);
                case "enrichment" -> critter.setEnrichment(value);
                case "hygiene" -> critter.setHygiene(value);
            }
            successCount++;
        }

        if (successCount == 0) {
            source.sendFailure(Component.literal("No DiverseCritters were found within 5 blocks."));
        } else {
            Component msg = Component.literal("Successfully set " + statName + " to " + value + " for " + successCount + " critters.");
            source.sendSuccess(() -> msg, true);
        }

        return successCount;
    }
}