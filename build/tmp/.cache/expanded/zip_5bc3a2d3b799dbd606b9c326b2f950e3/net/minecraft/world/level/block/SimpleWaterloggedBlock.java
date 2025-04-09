package net.minecraft.world.level.block;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public interface SimpleWaterloggedBlock extends BucketPickup, LiquidBlockContainer {
    @Override
    default boolean canPlaceLiquid(@Nullable Player p_298313_, BlockGetter p_56301_, BlockPos p_56302_, BlockState p_56303_, Fluid p_56304_) {
        return p_56304_ == Fluids.WATER;
    }

    @Override
    default boolean placeLiquid(LevelAccessor pLevel, BlockPos pPos, BlockState pState, FluidState pFluidState) {
        if (!pState.getValue(BlockStateProperties.WATERLOGGED) && pFluidState.getType() == Fluids.WATER) {
            if (!pLevel.isClientSide()) {
                pLevel.setBlock(pPos, pState.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true)), 3);
                pLevel.scheduleTick(pPos, pFluidState.getType(), pFluidState.getType().getTickDelay(pLevel));
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    default ItemStack pickupBlock(@Nullable Player p_301318_, LevelAccessor p_154560_, BlockPos p_154561_, BlockState p_154562_) {
        if (p_154562_.getValue(BlockStateProperties.WATERLOGGED)) {
            p_154560_.setBlock(p_154561_, p_154562_.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(false)), 3);
            if (!p_154562_.canSurvive(p_154560_, p_154561_)) {
                p_154560_.destroyBlock(p_154561_, true);
            }

            return new ItemStack(Items.WATER_BUCKET);
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    default Optional<SoundEvent> getPickupSound() {
        return Fluids.WATER.getPickupSound();
    }
}