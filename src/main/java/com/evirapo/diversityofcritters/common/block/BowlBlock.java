package com.evirapo.diversityofcritters.common.block;

import com.evirapo.diversityofcritters.common.block.entity.BowlBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class BowlBlock extends Block implements EntityBlock {

    // Propiedad que ya teníamos
    public static final EnumProperty<BowlContent> CONTENT =
            EnumProperty.create("content", BowlContent.class);

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 3, 12);

    public BowlBlock(Properties props) {
        super(props);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(CONTENT, BowlContent.EMPTY)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // ---- BlockEntity ----

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BowlBlockEntity(pos, state);
    }

    // ---- Abrir GUI ----

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BowlBlockEntity bowl) {
                NetworkHooks.openScreen((ServerPlayer) player, bowl, pos);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
