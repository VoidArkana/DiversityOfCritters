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

public class SetPregnancyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("doc_pregnancy")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                        .executes(context -> setPregnancyInArea(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "ticks")
                        ))
                )
        );
    }

    private static int setPregnancyInArea(CommandSourceStack source, int ticks) {
        int successCount = 0;

        Vec3 pos = source.getPosition();

        AABB searchArea = new AABB(
                pos.x - 5, pos.y - 5, pos.z - 5,
                pos.x + 5, pos.y + 5, pos.z + 5
        );

        List<DiverseCritter> nearbyCritters = source.getLevel().getEntitiesOfClass(DiverseCritter.class, searchArea);

        for (DiverseCritter critter : nearbyCritters) {
            if (critter.isPregnant()) {
                critter.pregnancyTimer = ticks;
                successCount++;
            }
        }

        if (successCount == 0) {
            source.sendFailure(Component.literal("No pregnant DiverseCritters were found within 5 blocks."));
        } else if (successCount == 1) {
            Component msg = Component.literal("Successfully set pregnancy timer to " + ticks + " ticks for 1 pregnant critter.");
            source.sendSuccess(() -> msg, true);
        } else {
            Component msg = Component.literal("Successfully set pregnancy timer to " + ticks + " ticks for " + successCount + " pregnant critters.");
            source.sendSuccess(() -> msg, true);
        }

        return successCount;
    }
}