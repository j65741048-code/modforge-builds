package dev.oldcontent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public final class GearBlock extends Block {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(3, 0, 3, 13, 3, 13),
            Block.createCuboidShape(6, 0, 0, 10, 3, 3),
            Block.createCuboidShape(6, 0, 13, 10, 3, 16),
            Block.createCuboidShape(0, 0, 6, 3, 3, 10),
            Block.createCuboidShape(13, 0, 6, 16, 3, 10));

    public GearBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
