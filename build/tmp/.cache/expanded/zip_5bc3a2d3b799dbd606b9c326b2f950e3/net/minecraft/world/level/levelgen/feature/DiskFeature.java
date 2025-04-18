package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;

public class DiskFeature extends Feature<DiskConfiguration> {
    public DiskFeature(Codec<DiskConfiguration> p_224992_) {
        super(p_224992_);
    }

    @Override
    public boolean place(FeaturePlaceContext<DiskConfiguration> p_224994_) {
        DiskConfiguration diskconfiguration = p_224994_.config();
        BlockPos blockpos = p_224994_.origin();
        WorldGenLevel worldgenlevel = p_224994_.level();
        RandomSource randomsource = p_224994_.random();
        boolean flag = false;
        int i = blockpos.getY();
        int j = i + diskconfiguration.halfHeight();
        int k = i - diskconfiguration.halfHeight() - 1;
        int l = diskconfiguration.radius().sample(randomsource);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-l, 0, -l), blockpos.offset(l, 0, l))) {
            int i1 = blockpos1.getX() - blockpos.getX();
            int j1 = blockpos1.getZ() - blockpos.getZ();
            if (i1 * i1 + j1 * j1 <= l * l) {
                flag |= this.placeColumn(diskconfiguration, worldgenlevel, randomsource, j, k, blockpos$mutableblockpos.set(blockpos1));
            }
        }

        return flag;
    }

    protected boolean placeColumn(
        DiskConfiguration pConfig, WorldGenLevel pLevel, RandomSource pRandom, int pMaxY, int pMinY, BlockPos.MutableBlockPos pPos
    ) {
        boolean flag = false;
        boolean flag1 = false;

        for (int i = pMaxY; i > pMinY; i--) {
            pPos.setY(i);
            if (pConfig.target().test(pLevel, pPos)) {
                BlockState blockstate = pConfig.stateProvider().getState(pLevel, pRandom, pPos);
                pLevel.setBlock(pPos, blockstate, 2);
                if (!flag1) {
                    this.markAboveForPostProcessing(pLevel, pPos);
                }

                flag = true;
                flag1 = true;
            } else {
                flag1 = false;
            }
        }

        return flag;
    }
}