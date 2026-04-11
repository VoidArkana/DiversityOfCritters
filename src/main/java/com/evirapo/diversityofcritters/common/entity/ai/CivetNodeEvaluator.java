package com.evirapo.diversityofcritters.common.entity.ai;

import com.evirapo.diversityofcritters.misc.tags.DoCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Extends WalkNodeEvaluator to add vertical (climbing) neighbors.
 *
 * Root problems solved vs. previous version:
 *
 * 1. findAcceptedNode() for CLIMB DOWN was returning null/BLOCKED because
 *    MC's fall-distance check triggers when there is no floor below the node.
 *    Fix: bypass findAcceptedNode entirely for wall nodes and build them
 *    manually with getNode(), which skips the fall-distance logic.
 *
 * 2. isWallNodeAcceptable was rejecting those BLOCKED nodes from findAcceptedNode.
 *    Fix: no longer needed for down nodes since we build them ourselves.
 *
 * 3. CLIMB UP also benefits from the direct-node approach for consistency,
 *    but we still check that the space is physically open.
 *
 * Cost model:
 *   Wall nodes cost CLIMB_COST_MALUS more than ground nodes so the
 *   pathfinder prefers walking on the ground and only climbs when necessary.
 */
public class CivetNodeEvaluator extends WalkNodeEvaluator {

    private static final float CLIMB_COST_MALUS = 4.0F;

    @Override
    public int getNeighbors(Node[] pOutputArray, Node pNode) {
        int count = super.getNeighbors(pOutputArray, pNode);

        BlockPos here = new BlockPos(pNode.x, pNode.y, pNode.z);

        // --- CLIMB UP ---
        // Requires: climbable wall adjacent at this level, space above is open.
        if (hasAdjacentClimbable(here) && isOpenSpace(here.above())) {
            Node upNode = buildWallNode(pNode.x, pNode.y + 1, pNode.z);
            if (count < pOutputArray.length) {
                pOutputArray[count++] = upNode;
            }
        }

        // --- CLIMB DOWN ---
        // Requires: climbable wall adjacent here AND one level below,
        // confirming we are on the wall face and not at ground level.
        // We do NOT call findAcceptedNode here — it blocks due to fall distance.
        // Instead we build the node directly, which bypasses MC's fall check.
        if (hasAdjacentClimbable(here) && hasAdjacentClimbable(here.below())
                && isOpenSpace(here.below())) {
            Node downNode = buildWallNode(pNode.x, pNode.y - 1, pNode.z);
            if (count < pOutputArray.length) {
                pOutputArray[count++] = downNode;
            }
        }

        // --- DESCEND FROM TOP OF WALL ---
        // Special case: civet is standing on top of a climbable block
        // (e.g. on top of a log). The block below here is the wall itself.
        // hasAdjacentClimbable(here.below()) alone misses this because
        // the wall is directly under us, not beside us at y-1.
        // Condition: the block directly below is climbable, and the space
        // at y-1 (beside the wall face, one block down) is open.
        if (isClimbable(here.below()) && isOpenSpace(here.below())) {
            // Find which horizontal direction the wall face is on
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos wallFacePos = here.below().relative(dir);
                // The space the civet would occupy at y-1 must be open
                // and there must be a wall to cling to at that level
                if (isOpenSpace(here.relative(dir)) && isClimbable(wallFacePos.above())
                        || isClimbable(wallFacePos)) {
                    Node descendNode = buildWallNode(pNode.x, pNode.y - 1, pNode.z);
                    if (count < pOutputArray.length) {
                        pOutputArray[count++] = descendNode;
                    }
                    break; // one descend node is enough
                }
            }
        }

        return count;
    }

    // -----------------------------------------------------------------------
    // Node construction
    // -----------------------------------------------------------------------

    /**
     * Builds a wall node directly using NodeEvaluator.getNode().
     * This bypasses findAcceptedNode's fall-distance check, which would
     * incorrectly block legitimate wall positions that have no floor below.
     * The node is marked WALKABLE with CLIMB_COST_MALUS so A* can traverse it.
     */
    private Node buildWallNode(int x, int y, int z) {
        Node node = this.getNode(x, y, z);
        node.type = BlockPathTypes.WALKABLE;
        node.costMalus = CLIMB_COST_MALUS;
        return node;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private boolean hasAdjacentClimbable(BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (isClimbable(pos.relative(dir))) return true;
        }
        return false;
    }

    private boolean isClimbable(BlockPos pos) {
        return this.level.getBlockState(pos).is(DoCTags.Blocks.CIVET_CLIMBABLE);
    }

    private boolean isOpenSpace(BlockPos pos) {
        return this.level.getBlockState(pos).isAir()
                || !this.level.getBlockState(pos).isSolid();
    }
}
