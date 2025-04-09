package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceRelativeThresholdFilter extends PlacementFilter {
    public static final MapCodec<SurfaceRelativeThresholdFilter> CODEC = RecordCodecBuilder.mapCodec(
        p_191929_ -> p_191929_.group(
                    Heightmap.Types.CODEC.fieldOf("heightmap").forGetter(p_191944_ -> p_191944_.heightmap),
                    Codec.INT.optionalFieldOf("min_inclusive", Integer.valueOf(Integer.MIN_VALUE)).forGetter(p_191942_ -> p_191942_.minInclusive),
                    Codec.INT.optionalFieldOf("max_inclusive", Integer.valueOf(Integer.MAX_VALUE)).forGetter(p_191939_ -> p_191939_.maxInclusive)
                )
                .apply(p_191929_, SurfaceRelativeThresholdFilter::new)
    );
    private final Heightmap.Types heightmap;
    private final int minInclusive;
    private final int maxInclusive;

    private SurfaceRelativeThresholdFilter(Heightmap.Types pHeightmap, int pMinInclusive, int pMaxInclusive) {
        this.heightmap = pHeightmap;
        this.minInclusive = pMinInclusive;
        this.maxInclusive = pMaxInclusive;
    }

    public static SurfaceRelativeThresholdFilter of(Heightmap.Types pHeightmap, int pMinInclusive, int pMaxInclusive) {
        return new SurfaceRelativeThresholdFilter(pHeightmap, pMinInclusive, pMaxInclusive);
    }

    @Override
    protected boolean shouldPlace(PlacementContext p_226407_, RandomSource p_226408_, BlockPos p_226409_) {
        long i = (long)p_226407_.getHeight(this.heightmap, p_226409_.getX(), p_226409_.getZ());
        long j = i + (long)this.minInclusive;
        long k = i + (long)this.maxInclusive;
        return j <= (long)p_226409_.getY() && (long)p_226409_.getY() <= k;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.SURFACE_RELATIVE_THRESHOLD_FILTER;
    }
}