package com.evirapo.diversityofcritters.common.command;

import com.evirapo.diversityofcritters.common.entity.ai.FindFoodBowlGoal;
import com.evirapo.diversityofcritters.common.entity.custom.CivetEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Debug command: /civet_debug
 * Run while looking at or standing near a CivetEntity.
 * Prints a full diagnostic snapshot to chat:
 *   - Entity state (onGround, climbing, climbState, hasAdjacentClimbable)
 *   - Active path (node count, current index, next 5 nodes with Y deltas)
 *   - Navigation state (isDone, canUpdatePath equivalent)
 *   - MoveControl wanted position
 *   - Goal selector top active goals
 */
public class CivetDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("civet_debug")
                .requires(source -> source.hasPermission(2))
                .executes(context -> run(context.getSource()))
        );
    }

    private static int run(CommandSourceStack source) {
        Vec3 pos = source.getPosition();

        // Find the nearest CivetEntity within 10 blocks
        List<CivetEntity> civets = source.getLevel().getEntitiesOfClass(
                CivetEntity.class,
                new AABB(pos.x - 10, pos.y - 10, pos.z - 10,
                         pos.x + 10, pos.y + 10, pos.z + 10)
        );

        if (civets.isEmpty()) {
            source.sendFailure(Component.literal("[CivetDebug] No CivetEntity found within 10 blocks."));
            return 0;
        }

        // Pick the closest one
        CivetEntity civet = civets.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(pos.x, pos.y, pos.z),
                                              b.distanceToSqr(pos.x, pos.y, pos.z)))
                .orElse(civets.get(0));

        send(source, "§e=== CIVET DEBUG [" + civet.getId() + "] ===");

        // --- Entity state ---
        send(source, "§bPOS: " + fmt(civet.getX()) + " " + fmt(civet.getY()) + " " + fmt(civet.getZ()));
        send(source, "§bonGround: " + civet.onGround()
                + "  horizontalCollision: " + civet.horizontalCollision
                + "  verticalCollision: " + civet.verticalCollision);
        send(source, "§bclimbState: " + climbStateName(civet.getClimbState())
                + "  isClimbing: " + civet.isClimbing()
                + "  hasAdjacentClimbable: " + civet.hasAdjacentClimbableBlock());

        // Block directly below
        BlockPos below = civet.blockPosition().below();
        String belowBlock = source.getLevel().getBlockState(below).getBlock().getDescriptionId();
        send(source, "§bblockBelow: " + belowBlock + " @ " + below);

        // --- Navigation state ---
        Path path = civet.getNavigation().getPath();
        boolean navDone = civet.getNavigation().isDone();
        send(source, "§anav.isDone: " + navDone);

        // Simulate canUpdatePath check
        boolean canUpdate = civet.isClimbing() || civet.onGround();
        send(source, "§acanUpdatePath (simulated): " + canUpdate
                + " (climbing=" + civet.isClimbing() + " onGround=" + civet.onGround() + ")");

        if (path == null) {
            send(source, "§cPATH: null — no active path");
        } else {
            int nodeCount = path.getNodeCount();
            int nextIdx = path.getNextNodeIndex();
            int currentY = nextIdx > 0 ? path.getNode(nextIdx - 1).y : (int) civet.getY();

            send(source, "§aPATH: " + nodeCount + " nodes, nextIndex=" + nextIdx
                    + ", reached=" + path.canReach());

            // Always print all nodes when count is small, for full diagnosis
            if (nodeCount <= 8) {
                printAllNodes(source, path);
            } else {
                // Print next 6 nodes with dy
                int limit = Math.min(nextIdx + 6, nodeCount);
                for (int i = nextIdx; i < limit; i++) {
                    Node n = path.getNode(i);
                    int dy = n.y - currentY;
                    String marker = (i == nextIdx) ? " §e<-- NEXT" : "";
                    String dyStr = dy > 0 ? "§a▲+" + dy : dy < 0 ? "§c▼" + dy : "§7→0";
                    send(source, "  §7[" + i + "] (" + n.x + "," + n.y + "," + n.z + ")"
                            + " type=" + n.type
                            + " costMalus=" + fmt(n.costMalus)
                            + " dy=" + dyStr + marker);
                    currentY = n.y;
                }
            }

            // End node
            Node end = path.getEndNode();
            if (end != null) {
                send(source, "§aPATH TARGET: (" + end.x + "," + end.y + "," + end.z + ")");
            }
        }

        // --- MoveControl state ---
        // We can't directly read wantedX/Y/Z (private in MoveControl base),
        // but we can print the navigation's last setWantedPosition indirectly
        // by reading what the navigation tick just computed.
        send(source, "§dmoveControl.operation: " + civet.getMoveControl().getClass().getSimpleName());

        // --- Active goal extra info ---
        send(source, "§6Active goals:");
        civet.goalSelector.getRunningGoals()
                .limit(5)
                .forEach(goal -> {
                    String extra = "";
                    if (goal.getGoal() instanceof FindFoodBowlGoal ffg) {
                        BlockPos bp = ffg.getBowlPos();
                        if (bp != null) {
                            double dist = Math.sqrt(civet.distanceToSqr(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5));
                            double followRange = civet.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
                            extra = " bowlPos=" + bp
                                    + " dist=" + fmt(dist)
                                    + " followRange=" + fmt(followRange)
                                    + (dist > followRange ? " §cOUT_OF_RANGE" : " §aIN_RANGE");
                        } else {
                            extra = " bowlPos=null";
                        }
                    }
                    send(source, "  §6- " + goal.getGoal().getClass().getSimpleName() + extra);
                });

        send(source, "§e=== END ===");
        return 1;
    }

    /**
     * Extra: print full node list when PATH has few nodes, to see the exact
     * coordinates and confirm whether the target Y is being adjusted.
     */
    private static void printAllNodes(CommandSourceStack source, Path path) {
        int count = path.getNodeCount();
        send(source, "§7Full node list (" + count + " nodes):");
        int refY = count > 0 ? path.getNode(0).y : 0;
        for (int i = 0; i < count; i++) {
            var n = path.getNode(i);
            int dy = n.y - refY;
            String dyStr = dy > 0 ? "§a▲+" + dy : dy < 0 ? "§c▼" + dy : "§7→0";
            send(source, "  §7[" + i + "] (" + n.x + "," + n.y + "," + n.z + ") dy=" + dyStr
                    + " type=" + n.type + " malus=" + String.format("%.1f", n.costMalus));
            refY = n.y;
        }
    }

    private static String climbStateName(byte state) {
        return switch (state) {
            case 0 -> "NONE";
            case 1 -> "UP";
            case 2 -> "DOWN";
            case 3 -> "HANG";
            default -> "UNKNOWN(" + state + ")";
        };
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static void send(CommandSourceStack source, String msg) {
        source.sendSuccess(() -> Component.literal(msg), false);
    }
}
