package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CactusBlock extends Block implements net.minecraftforge.common.IPlantable {
    public static final MapCodec<CactusBlock> CODEC = simpleCodec(CactusBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final int MAX_AGE = 15;
    protected static final int AABB_OFFSET = 1;
    protected static final VoxelShape COLLISION_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);
    protected static final VoxelShape OUTLINE_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    @Override
    public MapCodec<CactusBlock> codec() {
        return CODEC;
    }

    public CactusBlock(BlockBehaviour.Properties p_51136_) {
        super(p_51136_);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, Integer.valueOf(0)));
    }

    @Override
    protected void tick(BlockState p_220908_, ServerLevel p_220909_, BlockPos p_220910_, RandomSource p_220911_) {
        if (!p_220909_.isAreaLoaded(p_220910_, 1)) return; // Forge: prevent growing cactus from loading unloaded chunks with block update
        if (!p_220908_.canSurvive(p_220909_, p_220910_)) {
            p_220909_.destroyBlock(p_220910_, true);
        }
    }

    @Override
    protected void randomTick(BlockState p_220913_, ServerLevel p_220914_, BlockPos p_220915_, RandomSource p_220916_) {
        BlockPos blockpos = p_220915_.above();
        if (p_220914_.isEmptyBlock(blockpos)) {
            int i = 1;

            while (p_220914_.getBlockState(p_220915_.below(i)).is(this)) {
                i++;
            }

            if (i < 3) {
                int j = p_220913_.getValue(AGE);
                if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(p_220914_, blockpos, p_220913_, true)) {
                if (j == 15) {
                    p_220914_.setBlockAndUpdate(blockpos, this.defaultBlockState());
                    BlockState blockstate = p_220913_.setValue(AGE, Integer.valueOf(0));
                    p_220914_.setBlock(p_220915_, blockstate, 4);
                    p_220914_.neighborChanged(blockstate, blockpos, this, null, false);
                } else {
                    p_220914_.setBlock(p_220915_, p_220913_.setValue(AGE, Integer.valueOf(j + 1)), 4);
                }
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(p_220914_, p_220915_, p_220913_);
                }
            }
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return COLLISION_SHAPE;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return OUTLINE_SHAPE;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_51157_,
        LevelReader p_368068_,
        ScheduledTickAccess p_362750_,
        BlockPos p_51161_,
        Direction p_51158_,
        BlockPos p_51162_,
        BlockState p_51159_,
        RandomSource p_362850_
    ) {
        if (!p_51157_.canSurvive(p_368068_, p_51161_)) {
            p_362750_.scheduleTick(p_51161_, this, 1);
        }

        return super.updateShape(p_51157_, p_368068_, p_362750_, p_51161_, p_51158_, p_51162_, p_51159_, p_362850_);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState blockstate = pLevel.getBlockState(pPos.relative(direction));
            if (blockstate.isSolid() || pLevel.getFluidState(pPos.relative(direction)).is(FluidTags.LAVA)) {
                return false;
            }
        }

        BlockState blockstate1 = pLevel.getBlockState(pPos.below());
        return blockstate1.canSustainPlant(pLevel, pPos, Direction.UP, this) && !pLevel.getBlockState(pPos.above()).liquid();
    }

    @Override
    protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        pEntity.hurt(pLevel.damageSources().cactus(), 1.0F);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE);
    }

    @Override
    protected boolean isPathfindable(BlockState p_51143_, PathComputationType p_51146_) {
        return false;
    }

    @Override
    public net.minecraftforge.common.PlantType getPlantType(BlockGetter world, BlockPos pos) {
        return net.minecraftforge.common.PlantType.DESERT;
    }

    @Override
    public BlockState getPlant(BlockGetter world, BlockPos pos) {
        return defaultBlockState();
    }
}
