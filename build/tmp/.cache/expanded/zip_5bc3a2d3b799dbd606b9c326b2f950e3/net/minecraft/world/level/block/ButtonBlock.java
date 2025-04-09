package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ButtonBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<ButtonBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360402_ -> p_360402_.group(
                    BlockSetType.CODEC.fieldOf("block_set_type").forGetter(p_312681_ -> p_312681_.type),
                    Codec.intRange(1, 1024).fieldOf("ticks_to_stay_pressed").forGetter(p_312686_ -> p_312686_.ticksToStayPressed),
                    propertiesCodec()
                )
                .apply(p_360402_, ButtonBlock::new)
    );
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int PRESSED_DEPTH = 1;
    private static final int UNPRESSED_DEPTH = 2;
    protected static final int HALF_AABB_HEIGHT = 2;
    protected static final int HALF_AABB_WIDTH = 3;
    protected static final VoxelShape CEILING_AABB_X = Block.box(6.0, 14.0, 5.0, 10.0, 16.0, 11.0);
    protected static final VoxelShape CEILING_AABB_Z = Block.box(5.0, 14.0, 6.0, 11.0, 16.0, 10.0);
    protected static final VoxelShape FLOOR_AABB_X = Block.box(6.0, 0.0, 5.0, 10.0, 2.0, 11.0);
    protected static final VoxelShape FLOOR_AABB_Z = Block.box(5.0, 0.0, 6.0, 11.0, 2.0, 10.0);
    protected static final VoxelShape NORTH_AABB = Block.box(5.0, 6.0, 14.0, 11.0, 10.0, 16.0);
    protected static final VoxelShape SOUTH_AABB = Block.box(5.0, 6.0, 0.0, 11.0, 10.0, 2.0);
    protected static final VoxelShape WEST_AABB = Block.box(14.0, 6.0, 5.0, 16.0, 10.0, 11.0);
    protected static final VoxelShape EAST_AABB = Block.box(0.0, 6.0, 5.0, 2.0, 10.0, 11.0);
    protected static final VoxelShape PRESSED_CEILING_AABB_X = Block.box(6.0, 15.0, 5.0, 10.0, 16.0, 11.0);
    protected static final VoxelShape PRESSED_CEILING_AABB_Z = Block.box(5.0, 15.0, 6.0, 11.0, 16.0, 10.0);
    protected static final VoxelShape PRESSED_FLOOR_AABB_X = Block.box(6.0, 0.0, 5.0, 10.0, 1.0, 11.0);
    protected static final VoxelShape PRESSED_FLOOR_AABB_Z = Block.box(5.0, 0.0, 6.0, 11.0, 1.0, 10.0);
    protected static final VoxelShape PRESSED_NORTH_AABB = Block.box(5.0, 6.0, 15.0, 11.0, 10.0, 16.0);
    protected static final VoxelShape PRESSED_SOUTH_AABB = Block.box(5.0, 6.0, 0.0, 11.0, 10.0, 1.0);
    protected static final VoxelShape PRESSED_WEST_AABB = Block.box(15.0, 6.0, 5.0, 16.0, 10.0, 11.0);
    protected static final VoxelShape PRESSED_EAST_AABB = Block.box(0.0, 6.0, 5.0, 1.0, 10.0, 11.0);
    private final BlockSetType type;
    private final int ticksToStayPressed;

    @Override
    public MapCodec<ButtonBlock> codec() {
        return CODEC;
    }

    public ButtonBlock(BlockSetType pType, int pTicksToStayPressed, BlockBehaviour.Properties pProperties) {
        super(pProperties.sound(pType.soundType()));
        this.type = pType;
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(FACE, AttachFace.WALL)
        );
        this.ticksToStayPressed = pTicksToStayPressed;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        boolean flag = pState.getValue(POWERED);
        switch ((AttachFace)pState.getValue(FACE)) {
            case FLOOR:
                if (direction.getAxis() == Direction.Axis.X) {
                    return flag ? PRESSED_FLOOR_AABB_X : FLOOR_AABB_X;
                }

                return flag ? PRESSED_FLOOR_AABB_Z : FLOOR_AABB_Z;
            case WALL:
                return switch (direction) {
                    case EAST -> flag ? PRESSED_EAST_AABB : EAST_AABB;
                    case WEST -> flag ? PRESSED_WEST_AABB : WEST_AABB;
                    case SOUTH -> flag ? PRESSED_SOUTH_AABB : SOUTH_AABB;
                    case NORTH, UP, DOWN -> flag ? PRESSED_NORTH_AABB : NORTH_AABB;
                };
            case CEILING:
            default:
                if (direction.getAxis() == Direction.Axis.X) {
                    return flag ? PRESSED_CEILING_AABB_X : CEILING_AABB_X;
                } else {
                    return flag ? PRESSED_CEILING_AABB_Z : CEILING_AABB_Z;
                }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_329418_, Level p_334611_, BlockPos p_332004_, Player p_330636_, BlockHitResult p_327724_) {
        if (p_329418_.getValue(POWERED)) {
            return InteractionResult.CONSUME;
        } else {
            this.press(p_329418_, p_334611_, p_332004_, p_330636_);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected void onExplosionHit(BlockState p_310762_, ServerLevel p_363623_, BlockPos p_312982_, Explosion p_311820_, BiConsumer<ItemStack, BlockPos> p_312672_) {
        if (p_311820_.canTriggerBlocks() && !p_310762_.getValue(POWERED)) {
            this.press(p_310762_, p_363623_, p_312982_, null);
        }

        super.onExplosionHit(p_310762_, p_363623_, p_312982_, p_311820_, p_312672_);
    }

    public void press(BlockState pState, Level pLevel, BlockPos pPos, @Nullable Player pPlayer) {
        pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(true)), 3);
        this.updateNeighbours(pState, pLevel, pPos);
        pLevel.scheduleTick(pPos, this, this.ticksToStayPressed);
        this.playSound(pPlayer, pLevel, pPos, true);
        pLevel.gameEvent(pPlayer, GameEvent.BLOCK_ACTIVATE, pPos);
    }

    protected void playSound(@Nullable Player pPlayer, LevelAccessor pLevel, BlockPos pPos, boolean pHitByArrow) {
        pLevel.playSound(pHitByArrow ? pPlayer : null, pPos, this.getSound(pHitByArrow), SoundSource.BLOCKS);
    }

    protected SoundEvent getSound(boolean pIsOn) {
        return pIsOn ? this.type.buttonClickOn() : this.type.buttonClickOff();
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving && !pState.is(pNewState.getBlock())) {
            if (pState.getValue(POWERED)) {
                this.updateNeighbours(pState, pLevel, pPos);
            }

            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        }
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) && getConnectedDirection(pBlockState) == pSide ? 15 : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    protected void tick(BlockState p_220903_, ServerLevel p_220904_, BlockPos p_220905_, RandomSource p_220906_) {
        if (p_220903_.getValue(POWERED)) {
            this.checkPressed(p_220903_, p_220904_, p_220905_);
        }
    }

    @Override
    protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        if (!pLevel.isClientSide && this.type.canButtonBeActivatedByArrows() && !pState.getValue(POWERED)) {
            this.checkPressed(pState, pLevel, pPos);
        }
    }

    protected void checkPressed(BlockState pState, Level pLevel, BlockPos pPos) {
        AbstractArrow abstractarrow = this.type.canButtonBeActivatedByArrows()
            ? pLevel.getEntitiesOfClass(AbstractArrow.class, pState.getShape(pLevel, pPos).bounds().move(pPos)).stream().findFirst().orElse(null)
            : null;
        boolean flag = abstractarrow != null;
        boolean flag1 = pState.getValue(POWERED);
        if (flag != flag1) {
            pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(flag)), 3);
            this.updateNeighbours(pState, pLevel, pPos);
            this.playSound(null, pLevel, pPos, flag);
            pLevel.gameEvent(abstractarrow, flag ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pPos);
        }

        if (flag) {
            pLevel.scheduleTick(new BlockPos(pPos), this, this.ticksToStayPressed);
        }
    }

    private void updateNeighbours(BlockState pState, Level pLevel, BlockPos pPos) {
        Direction direction = getConnectedDirection(pState).getOpposite();
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(
            pLevel, direction, direction.getAxis().isHorizontal() ? Direction.UP : pState.getValue(FACING)
        );
        pLevel.updateNeighborsAt(pPos, this, orientation);
        pLevel.updateNeighborsAt(pPos.relative(direction), this, orientation);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, POWERED, FACE);
    }
}