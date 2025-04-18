package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DoubleHighBlockItem extends BlockItem {
    public DoubleHighBlockItem(Block p_41010_, Item.Properties p_41011_) {
        super(p_41010_, p_41011_);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext pContext, BlockState pState) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos().above();
        BlockState blockstate = level.isWaterAt(blockpos) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        level.setBlock(blockpos, blockstate, 27);
        return super.placeBlock(pContext, pState);
    }
}