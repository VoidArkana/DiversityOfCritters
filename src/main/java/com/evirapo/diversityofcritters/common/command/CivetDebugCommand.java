package com.evirapo.diversityofcritters.common.command;

import com.evirapo.diversityofcritters.common.entity.ai.FindFoodBowlGoal;
import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CivetDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("civet_debug")
                .requires(source -> source.hasPermission(2))
                .executes(context -> run(context.getSource()))
        );
    }

    private static int run(CommandSourceStack source) {
        Vec3 pos = source.getPosition();

        List<CivetEntity> civets = source.getLevel().getEntitiesOfClass(
                CivetEntity.class,
                new AABB(pos.x - 10, pos.y - 10, pos.z - 10,
                         pos.x + 10, pos.y + 10, pos.z + 10)
        );

        if (civets.isEmpty()) {
            source.sendFailure(Component.literal("[CivetDebug] No CivetEntity within 10 blocks."));
            return 0;
        }

        CivetEntity civet = civets.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(pos.x, pos.y, pos.z),
                                              b.distanceToSqr(pos.x, pos.y, pos.z)))
                .orElse(civets.get(0));

        send(source, "§e=== CIVET DEBUG [" + civet.getId() + "] ===");

        // --- POSITION & PHYSICS ---
        send(source, "§bPOS: " + fmt(civet.getX()) + " " + fmt(civet.getY()) + " " + fmt(civet.getZ()));
        Vec3 vel = civet.getDeltaMovement();
        send(source, "§bVEL: dx=" + fmt(vel.x) + " dy=" + fmt(vel.y) + " dz=" + fmt(vel.z));
        send(source, "§bonGround=" + civet.onGround()
                + "  hCollision=" + civet.horizontalCollision
                + "  vCollision=" + civet.verticalCollision);

        // Block below
        BlockPos below = civet.blockPosition().below();
        String belowBlock = source.getLevel().getBlockState(below).getBlock().getDescriptionId();
        send(source, "§bblockBelow: " + belowBlock + " @ " + below);

        // --- CLIMB STATE ---
        send(source, "§dclimbState: " + climbStateName(civet.getClimbState())
                + "  isClimbing=" + civet.isClimbing()
                + "  isClimbingDown=" + civet.isClimbingDown()
                + "  isHanging=" + civet.isHanging());
        send(source, "§dhasAdjacentClimbable=" + civet.hasAdjacentClimbableBlock());

        // Check each probe direction manually for detail
        AABB box = civet.getBoundingBox();
        double probeDistance = 0.3D;
        int footY = net.minecraft.util.Mth.floor(box.minY);
        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        double halfW = (box.maxX - box.minX) * 0.5;
        double halfD = (box.maxZ - box.minZ) * 0.5;
        String probeResult = ""
            + "§d  probes footY=" + footY + ": "
            + "+X=" + isClimbable(source, civet, new BlockPos(net.minecraft.util.Mth.floor(cx + halfW + probeDistance), footY, net.minecraft.util.Mth.floor(cz)))
            + " -X=" + isClimbable(source, civet, new BlockPos(net.minecraft.util.Mth.floor(cx - halfW - probeDistance), footY, net.minecraft.util.Mth.floor(cz)))
            + " +Z=" + isClimbable(source, civet, new BlockPos(net.minecraft.util.Mth.floor(cx), footY, net.minecraft.util.Mth.floor(cz + halfD + probeDistance)))
            + " -Z=" + isClimbable(source, civet, new BlockPos(net.minecraft.util.Mth.floor(cx), footY, net.minecraft.util.Mth.floor(cz - halfD - probeDistance)));
        send(source, probeResult);
        String probeResultM1 = ""
            + "§d  probes footY-1=" + (footY-1) + ": "
            + "+X=" + isClimbable(source, civet, new BlockPos(net.minecraft.util.Mth.floor(cx + halfW + probeDistance), footY-1, net.minecraft.util.Mth.floor(cz)))
            + " -X=" + isClimbable(source, civet, new BlockPos(net.minecraft.util.Mth.floor(cx - halfW - probeDistance), footY-1, net.minecraft.util.Mth.floor(cz)))
            + " +Z=" + isClimbable(source, civet, new BlockPos(net.minecraft.util.Mth.floor(cx), footY-1, net.minecraft.util.Mth.floor(cz + halfD + probeDistance)))
            + " -Z=" + isClimbable(source, civet, new BlockPos(net.minecraft.util.Mth.floor(cx), footY-1, net.minecraft.util.Mth.floor(cz - halfD - probeDistance)));
        send(source, probeResultM1);

        // Block directly below (climbable column check)
        BlockPos directBelow = new BlockPos(net.minecraft.util.Mth.floor(civet.getX()), footY - 1, net.minecraft.util.Mth.floor(civet.getZ()));
        boolean belowIsClimbable = source.getLevel().getBlockState(directBelow)
                .is(com.evirapo.diversityofcritters.misc.tags.DoCTags.Blocks.CIVET_CLIMBABLE);
        send(source, "§dblockDirectlyBelow climbable=" + belowIsClimbable + " @ " + directBelow);

        // --- NAVIGATION ---
        Path path = civet.getNavigation().getPath();
        send(source, "§anav.isDone=" + civet.getNavigation().isDone()
                + "  canUpdate(sim)=" + (civet.isClimbing() || civet.onGround()));

        if (path == null) {
            send(source, "§cPATH: null");
        } else {
            int nodeCount = path.getNodeCount();
            int nextIdx = path.getNextNodeIndex();
            send(source, "§aPATH: " + nodeCount + " nodes  nextIdx=" + nextIdx
                    + "  reached=" + path.canReach());

            // Print all nodes (up to 12)
            int limit = Math.min(nodeCount, 12);
            int refY = nodeCount > 0 ? path.getNode(0).y : (int) civet.getY();
            for (int i = 0; i < limit; i++) {
                Node n = path.getNode(i);
                int dy = n.y - refY;
                String marker = (i == nextIdx) ? " §e<<" : "";
                String dyStr = dy > 0 ? "§a▲+" + dy : dy < 0 ? "§c▼" + dy : "§7=0";
                send(source, "  §7[" + i + "] (" + n.x + "," + n.y + "," + n.z
                        + ") " + dyStr + " t=" + n.type + " m=" + fmt(n.costMalus) + marker);
                refY = n.y;
            }
            Node end = path.getEndNode();
            if (end != null) send(source, "§aTARGET: (" + end.x + "," + end.y + "," + end.z + ")");
        }

        // --- GOALS ---
        send(source, "§6Active goals:");
        civet.goalSelector.getRunningGoals().limit(5).forEach(goal -> {
            String extra = "";
            if (goal.getGoal() instanceof FindFoodBowlGoal ffg) {
                BlockPos bp = ffg.getBowlPos();
                if (bp != null) {
                    double dist = Math.sqrt(civet.distanceToSqr(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5));
                    double fr = civet.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
                    extra = " bowl=" + bp + " d=" + fmt(dist) + "/" + fmt(fr)
                            + (dist > fr ? " §cOOB" : " §aOK");
                } else {
                    extra = " bowl=null";
                }
            }
            send(source, "  §6- " + goal.getGoal().getClass().getSimpleName() + extra);
        });

        send(source, "§e=== END ===");
        return 1;
    }

    private static String isClimbable(CommandSourceStack source, CivetEntity civet, BlockPos pos) {
        boolean c = source.getLevel().getBlockState(pos)
                .is(com.evirapo.diversityofcritters.misc.tags.DoCTags.Blocks.CIVET_CLIMBABLE);
        return c ? "§aY" : "§7N";
    }

    private static String climbStateName(byte state) {
        return switch (state) {
            case 0 -> "NONE";
            case 1 -> "UP";
            case 2 -> "DOWN";
            case 3 -> "HANG";
            default -> "?(" + state + ")";
        };
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static void send(CommandSourceStack source, String msg) {
        source.sendSuccess(() -> Component.literal(msg), false);
    }
}
