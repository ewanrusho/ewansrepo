package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WeepingVinesFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] DIRECTIONS = Direction.values();

    public WeepingVinesFeature(Codec<NoneFeatureConfiguration> p_67375_) {
        super(p_67375_);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_160661_) {
        WorldGenLevel worldgenlevel = p_160661_.level();
        BlockPos blockpos = p_160661_.origin();
        RandomSource randomsource = p_160661_.random();
        if (!worldgenlevel.isEmptyBlock(blockpos)) {
            return false;
        } else {
            BlockState blockstate = worldgenlevel.getBlockState(blockpos.above());
            if (!blockstate.is(Blocks.NETHERRACK) && !blockstate.is(Blocks.NETHER_WART_BLOCK)) {
                return false;
            } else {
                this.placeRoofNetherWart(worldgenlevel, randomsource, blockpos);
                this.placeRoofWeepingVines(worldgenlevel, randomsource, blockpos);
                return true;
            }
        }
    }

    private void placeRoofNetherWart(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos) {
        pLevel.setBlock(pPos, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 200; i++) {
            blockpos$mutableblockpos.setWithOffset(
                pPos,
                pRandom.nextInt(6) - pRandom.nextInt(6),
                pRandom.nextInt(2) - pRandom.nextInt(5),
                pRandom.nextInt(6) - pRandom.nextInt(6)
            );
            if (pLevel.isEmptyBlock(blockpos$mutableblockpos)) {
                int j = 0;

                for (Direction direction : DIRECTIONS) {
                    BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos1.setWithOffset(blockpos$mutableblockpos, direction));
                    if (blockstate.is(Blocks.NETHERRACK) || blockstate.is(Blocks.NETHER_WART_BLOCK)) {
                        j++;
                    }

                    if (j > 1) {
                        break;
                    }
                }

                if (j == 1) {
                    pLevel.setBlock(blockpos$mutableblockpos, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
                }
            }
        }
    }

    private void placeRoofWeepingVines(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 100; i++) {
            blockpos$mutableblockpos.setWithOffset(
                pPos,
                pRandom.nextInt(8) - pRandom.nextInt(8),
                pRandom.nextInt(2) - pRandom.nextInt(7),
                pRandom.nextInt(8) - pRandom.nextInt(8)
            );
            if (pLevel.isEmptyBlock(blockpos$mutableblockpos)) {
                BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos.above());
                if (blockstate.is(Blocks.NETHERRACK) || blockstate.is(Blocks.NETHER_WART_BLOCK)) {
                    int j = Mth.nextInt(pRandom, 1, 8);
                    if (pRandom.nextInt(6) == 0) {
                        j *= 2;
                    }

                    if (pRandom.nextInt(5) == 0) {
                        j = 1;
                    }

                    int k = 17;
                    int l = 25;
                    placeWeepingVinesColumn(pLevel, pRandom, blockpos$mutableblockpos, j, 17, 25);
                }
            }
        }
    }

    public static void placeWeepingVinesColumn(
        LevelAccessor pLevel, RandomSource pRandom, BlockPos.MutableBlockPos pPos, int pHeight, int pMinAge, int pMaxAge
    ) {
        for (int i = 0; i <= pHeight; i++) {
            if (pLevel.isEmptyBlock(pPos)) {
                if (i == pHeight || !pLevel.isEmptyBlock(pPos.below())) {
                    pLevel.setBlock(
                        pPos,
                        Blocks.WEEPING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Integer.valueOf(Mth.nextInt(pRandom, pMinAge, pMaxAge))),
                        2
                    );
                    break;
                }

                pLevel.setBlock(pPos, Blocks.WEEPING_VINES_PLANT.defaultBlockState(), 2);
            }

            pPos.move(Direction.DOWN);
        }
    }
}