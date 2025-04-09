package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BaseCoralWallFanBlock extends BaseCoralFanBlock {
    public static final MapCodec<BaseCoralWallFanBlock> CODEC = simpleCodec(BaseCoralWallFanBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Maps.newEnumMap(
        ImmutableMap.of(
            Direction.NORTH,
            Block.box(0.0, 4.0, 5.0, 16.0, 12.0, 16.0),
            Direction.SOUTH,
            Block.box(0.0, 4.0, 0.0, 16.0, 12.0, 11.0),
            Direction.WEST,
            Block.box(5.0, 4.0, 0.0, 16.0, 12.0, 16.0),
            Direction.EAST,
            Block.box(0.0, 4.0, 0.0, 11.0, 12.0, 16.0)
        )
    );

    @Override
    public MapCodec<? extends BaseCoralWallFanBlock> codec() {
        return CODEC;
    }

    public BaseCoralWallFanBlock(BlockBehaviour.Properties p_49196_) {
        super(p_49196_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(true)));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(pState.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_49210_,
        LevelReader p_362875_,
        ScheduledTickAccess p_370057_,
        BlockPos p_49214_,
        Direction p_49211_,
        BlockPos p_49215_,
        BlockState p_49212_,
        RandomSource p_365418_
    ) {
        if (p_49210_.getValue(WATERLOGGED)) {
            p_370057_.scheduleTick(p_49214_, Fluids.WATER, Fluids.WATER.getTickDelay(p_362875_));
        }

        return p_49211_.getOpposite() == p_49210_.getValue(FACING) && !p_49210_.canSurvive(p_362875_, p_49214_) ? Blocks.AIR.defaultBlockState() : p_49210_;
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        Direction direction = pState.getValue(FACING);
        BlockPos blockpos = pPos.relative(direction.getOpposite());
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return blockstate.isFaceSturdy(pLevel, blockpos, direction);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = super.getStateForPlacement(pContext);
        LevelReader levelreader = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        Direction[] adirection = pContext.getNearestLookingDirections();

        for (Direction direction : adirection) {
            if (direction.getAxis().isHorizontal()) {
                blockstate = blockstate.setValue(FACING, direction.getOpposite());
                if (blockstate.canSurvive(levelreader, blockpos)) {
                    return blockstate;
                }
            }
        }

        return null;
    }
}